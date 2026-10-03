package com.ghosttap.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private val prefs: SharedPreferences = context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var connectionTimeoutJob: Job? = null

    // State flows - declared at the top to avoid initialization order issues
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    enum class State {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED
    }
    private val _connectionState = MutableStateFlow(State.DISCONNECTED)
    val connectionState: StateFlow<State> = _connectionState

    private val _scanResults = MutableStateFlow<List<ScanResultItem>>(emptyList())
    val scanResults: StateFlow<List<ScanResultItem>> = _scanResults

    private val _capturedFiles = MutableStateFlow<List<CapturedFile>>(emptyList())
    val capturedFiles: StateFlow<List<CapturedFile>> = _capturedFiles

    private val _handshakeStatus = MutableStateFlow(HandshakeStatus())
    val handshakeStatus: StateFlow<HandshakeStatus> = _handshakeStatus

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _downloadedFile = MutableStateFlow<ByteArray?>(null)
    val downloadedFile: StateFlow<ByteArray?> = _downloadedFile

    private val _isReady = MutableStateFlow(true)
    val isReady: StateFlow<Boolean> = _isReady

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _temperature = MutableStateFlow(0f)
    val temperature: StateFlow<Float> = _temperature

    private val _beaconSSIDs = MutableStateFlow<List<String>>(listOf("FREE WIFI", "HACKED", "POLICE", "GUEST", "NETGEAR"))
    val beaconSSIDs: StateFlow<List<String>> = _beaconSSIDs

    private var isFilesLoaded = false
    private var isManualDisconnect = false

    private val SERVICE_UUID = UUID.fromString("00004faf-0000-1000-8000-00805f9b34fb")
    private val CHAR_COMMAND_UUID = UUID.fromString("0000beb5-0000-1000-8000-00805f9b34fb")
    private val CHAR_RESPONSE_UUID = UUID.fromString("0000beb6-0000-1000-8000-00805f9b34fb")
    private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    fun resetHandshakeStatus() {
        _handshakeStatus.value = HandshakeStatus()
        _statusMessage.value = "Handshake status reset."
    }

    data class ScanResultItem(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val channel: Int,
        val isSecure: Boolean,
        val index: Int
    )

    data class CapturedFile(
        val filename: String,
        val ssid: String,
        val bssid: String,
        val status: String,
        val size: String,
        val timestamp: String,
        val format: String,
        val number: Int = 0,
        val eapolMask: Int = 0
    )

    init {
        loadFilesFromPrefs()
    }

    private var currentTargetNetworks: List<ScanResultItem> = emptyList()
    private var isAllModeEnabled: Boolean = false
    private var currentTargetNetwork: ScanResultItem? = null

    fun setTargetNetworks(networks: List<ScanResultItem>) {
        currentTargetNetworks = networks
        isAllModeEnabled = false
        currentTargetNetwork = if (networks.size == 1) networks[0] else null
    }

    fun setAllMode(enabled: Boolean) {
        isAllModeEnabled = enabled
        if (enabled) {
            currentTargetNetworks = emptyList()
            currentTargetNetwork = null
        }
    }

    fun getTargetNetworks(): List<ScanResultItem> = currentTargetNetworks
    fun isAllMode(): Boolean = isAllModeEnabled
    fun getCurrentTargetNetwork(): ScanResultItem? = currentTargetNetwork

    fun setBeaconSSIDs(ssids: List<String>) {
        _beaconSSIDs.value = ssids
        // Send to ESP32
        val commaSeparated = ssids.joinToString(",")
        sendCommand("SWARM_SSIDS:$commaSeparated")
    }

    fun addCapturedFile(file: CapturedFile) {
        val currentList = _capturedFiles.value.toMutableList()
        
        // Robust duplicate check: same BSSID within last 10 seconds
        val now = System.currentTimeMillis()
        if (currentList.any { 
            it.bssid == file.bssid && 
            (now - (it.filename.substringAfterLast("_").substringBefore(".pcap").toLongOrNull() ?: 0L)) < 10000 
        }) {
            Log.d("BleManager", "Duplicate capture detected for ${file.ssid}, skipping.")
            return
        }
        
        // Find the highest number and add 1
        val nextNumber = (currentList.maxByOrNull { it.number }?.number ?: 0) + 1
        val updatedFile = file.copy(number = nextNumber)
        
        // Add to start of list
        currentList.add(0, updatedFile)
        
        // Limit app list to keep it clean, e.g., last 20 captures
        if (currentList.size > 20) {
            val removed = currentList.removeAt(currentList.size - 1)
            // Delete internal file for removed entry
            try {
                val internalFile = getInternalFile(removed.filename)
                if (internalFile.exists()) internalFile.delete()
            } catch (e: Exception) {
                Log.e("BleManager", "Error cleaning up old file", e)
            }
        }
        
        _capturedFiles.value = currentList
        saveFilesToPrefs()
    }

    private fun analyzePcap(data: ByteArray): Pair<Int, String> {
        if (data.size < 24) return Pair(0, "0.0 KB")
        
        var eapolMask = 0
        var offset = 24 // Skip Global Header
        
        while (offset + 16 <= data.size) {
            val inclLen = (data[offset + 8].toInt() and 0xFF) or 
                          ((data[offset + 9].toInt() and 0xFF) shl 8) or 
                          ((data[offset + 10].toInt() and 0xFF) shl 16) or 
                          ((data[offset + 11].toInt() and 0xFF) shl 24)
            
            val packetStart = offset + 16
            if (packetStart + inclLen > data.size) break
            
            val frameData = data.sliceArray(packetStart until (packetStart + inclLen))
            eapolMask = analyzeSingleFrame(frameData, eapolMask)
            
            offset += 16 + inclLen
        }
        
        val sizeStr = "%.1f KB".format(data.size / 1024.0)
        return Pair(eapolMask, sizeStr)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun analyzeSingleFrame(data: ByteArray, currentMask: Int): Int {
        var eapolMask = currentMask
        // Search for EAPOL EtherType 0x888E after 802.11 header + LLC
        val searchRange = if (data.size > 60) 60 else data.size
        for (i in 0 until (searchRange - 1)) {
            if (data[i] == 0x88.toByte() && data[i + 1] == 0x8E.toByte()) {
                val eapolStart = i + 2
                if (eapolStart + 6 <= data.size) {
                    val eapolType = data[eapolStart + 1].toInt() and 0xFF
                    if (eapolType == 3) { // EAPOL-Key
                        val keyInfo = ((data[eapolStart + 5].toInt() and 0xFF) shl 8) or 
                                      (data[eapolStart + 6].toInt() and 0xFF)
                        
                        val keyAck = (keyInfo and 0x0080) != 0
                        val keyMic = (keyInfo and 0x0100) != 0
                        val secure = (keyInfo and 0x0200) != 0
                        val keyType = (keyInfo and 0x0008) != 0 // Pairwise
                        
                        if (keyType) {
                            if (keyAck && !keyMic) eapolMask = eapolMask or 0x01 // M1
                            else if (!keyAck && keyMic && !secure) eapolMask = eapolMask or 0x02 // M2
                            else if (keyAck && keyMic) eapolMask = eapolMask or 0x04 // M3
                            else if (!keyAck && keyMic && secure) eapolMask = eapolMask or 0x08 // M4
                        }
                    }
                }
                break
            }
        }
        return eapolMask
    }

    private fun saveDownloadedFileInternally(data: ByteArray, filename: String) {
        try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
                stream.write(data)
            }
            Log.d("BleManager", "Saved file internally: $filename")
        } catch (e: Exception) {
            Log.e("BleManager", "Error saving file internally", e)
        }
    }

    fun getInternalFile(filename: String): java.io.File {
        return java.io.File(context.filesDir, filename)
    }

    fun deleteCapturedFile(file: CapturedFile) {
        _capturedFiles.value = _capturedFiles.value.filter { it != file }
        saveFilesToPrefs()
        // Also delete internal file
        try {
            val internalFile = getInternalFile(file.filename)
            if (internalFile.exists()) {
                internalFile.delete()
            }
        } catch (e: Exception) {
            Log.e("BleManager", "Error deleting internal file", e)
        }
    }

    private fun saveFilesToPrefs(sync: Boolean = false) {
        if (!isFilesLoaded) {
            Log.w("BleManager", "Skipping save: Files not yet loaded")
            return
        }
        
        val list = _capturedFiles.value
        if (list.isEmpty()) {
            // Only save empty list if we are absolutely sure we want to (i.e., we explicitly deleted everything)
            // For now, let's just log it.
            Log.d("BleManager", "Saving empty file list to prefs")
        }
        
        val serialized = list.joinToString(";") { 
            val encodedSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.util.Base64.getEncoder().encodeToString(it.ssid.toByteArray())
            } else {
                android.util.Base64.encodeToString(it.ssid.toByteArray(), android.util.Base64.DEFAULT).trim()
            }
            "${it.filename}|$encodedSsid|${it.bssid}|${it.status}|${it.size}|${it.timestamp}|${it.format}|${it.number}|${it.eapolMask}" 
        }
        Log.d("BleManager", "Saving ${list.size} files to prefs")
        val editor = prefs.edit().putString("captured_files", serialized)
        if (sync) editor.commit() else editor.apply()
    }

    private fun loadFilesFromPrefs() {
        try {
            val serialized = prefs.getString("captured_files", "") ?: ""
            if (serialized.isEmpty()) {
                Log.d("BleManager", "No saved files found in prefs")
                isFilesLoaded = true
                return
            }
            
            val list = serialized.split(";").filter { it.isNotBlank() }.mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 7) {
                    val filename = parts[0]
                    val ssid = try {
                        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            java.util.Base64.getDecoder().decode(parts[1])
                        } else {
                            android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
                        }
                        String(decoded)
                    } catch (e: Exception) {
                        parts[1] // Fallback to raw if decoding fails
                    }
                    val bssid = parts[2]
                    val status = parts[3]
                    val size = parts[4]
                    val timestamp = parts[5]
                    val format = parts[6]
                    val number = if (parts.size > 7) parts[7].toIntOrNull() ?: 0 else 0
                    val eapolMask = if (parts.size > 8) parts[8].toIntOrNull() ?: 0 else 0
                    CapturedFile(filename, ssid, bssid, status, size, timestamp, format, number, eapolMask)
                } else {
                    null
                }
            }
            Log.d("BleManager", "Loaded ${list.size} files from prefs")
            _capturedFiles.value = list
            isFilesLoaded = true
        } catch (e: Exception) {
            Log.e("BleManager", "Error loading files from prefs", e)
            // DO NOT set isFilesLoaded = true here, so we don't overwrite on exit
        }
    }

    fun onDestroy() {
        saveFilesToPrefs(sync = true)
        scope.cancel()
    }

    private fun updateCapturedFileVerified(filename: String, verifiedMask: Int, verifiedSize: String) {
        val currentList = _capturedFiles.value.toMutableList()
        val index = currentList.indexOfFirst { it.filename == filename }
        if (index != -1) {
            val oldFile = currentList[index]
            currentList[index] = oldFile.copy(
                eapolMask = verifiedMask,
                size = verifiedSize,
                status = if (verifiedMask >= 0x03) "SUCCESS" else oldFile.status
            )
            _capturedFiles.value = currentList
            saveFilesToPrefs()
            
            // Also update the current handshake status if this is the active session
            if (filename == downloadingFilename) {
                _handshakeStatus.value = _handshakeStatus.value.copy(
                    eapolMask = verifiedMask,
                    fileSize = (java.io.File(context.filesDir, filename).length())
                )
            }
        }
    }

    data class HandshakeStatus(
        val frames: Int = 0,
        val eapolMask: Int = 0, // Bit 0: Frame 1, Bit 1: Frame 2, etc.
        val clients: Int = 0,
        val fileSize: Long = 0,
        val isComplete: Boolean = false
    )

    private var fileBuffer: ByteArray? = null
    private var expectedFileSize: Int = 0
    private var receivedBytes: Int = 0
    private var downloadingFilename: String? = null

    fun clearHandshakeStatus() {
        _handshakeStatus.value = HandshakeStatus()
        _downloadProgress.value = 0f
        _downloadedFile.value = null
        fileBuffer = null
        expectedFileSize = 0
        receivedBytes = 0
    }

    fun clearScanResults() {
        _scanResults.value = emptyList()
    }

    private val commandQueue = Channel<Any>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (command in commandQueue) {
                // Wait until connected and ready
                while (!_isConnected.value || !_isReady.value) {
                    // Optimized polling for better power and CPU usage
                    delay(10) 
                }
                
                _isReady.value = false
                when (command) {
                    is String -> performSendCommand(command)
                    is ByteArray -> performSendBinary(command)
                }
                
                // Safety timeout: if no response, reset isReady
                // For regular commands, we wait longer (5s). For binary data, we want it fast (500ms).
                scope.launch {
                    val timeout = if (command is String && (command.startsWith("SCAN") || command.startsWith("ATTACK"))) 30000L else 2000L
                    delay(timeout)
                    if (!_isReady.value) {
                        Log.w("BleManager", "Command timeout ($timeout ms), forcing ready: $command")
                        _isReady.value = true
                    }
                }
            }
        }
    }

    fun setReady(ready: Boolean) {
        _isReady.value = ready
    }

    private fun addLog(message: String) {
        Log.d("BleApp", message)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // Strict filtering: Only show devices that have our service UUID or a name containing "GhostTap" (or an ESP32)
            val hasName = device.name?.contains("ESP32", ignoreCase = true) == true || 
                          device.name?.contains("GhostTap", ignoreCase = true) == true
            
            if (hasName && !_scannedDevices.value.any { it.address == device.address }) {
                addLog("Found compatible device: ${device.name} (${device.address})")
                _scannedDevices.value = _scannedDevices.value + device

                // Auto-connect if this is the last used device
                val savedMac = prefs.getString("last_device", null)
                if (device.address == savedMac && !isConnected.value && connectionState.value == State.DISCONNECTED) {
                    addLog("Auto-connecting to known device: ${device.address}")
                    stopScanning()
                    connect(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            addLog("ERROR: Scan failed with error code: $errorCode")
            _isScanning.value = false
            _statusMessage.value = "Scan failed ($errorCode)"
        }
    }

    fun startScanning() {
        if (!bluetoothAdapter.isEnabled) {
            addLog("ERROR: Bluetooth is disabled. Please enable it.")
            _statusMessage.value = "Bluetooth is disabled"
            return
        }

        if (_isScanning.value) {
            stopScanning()
        }

        addLog("Starting scan for ESP32 devices...")
        _scannedDevices.value = emptyList()
        
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            addLog("ERROR: Bluetooth LE Scanner not available")
            _statusMessage.value = "BLE Scanner not available"
            return
        }

        _isScanning.value = true
        scanner.startScan(scanCallback)
        
        // Auto-reconnect if we have a saved MAC
        val savedMac = prefs.getString("last_device", null)
        if (savedMac != null && !isConnected.value && connectionState.value == State.DISCONNECTED) {
            addLog("Found saved device $savedMac, will auto-connect if discovered")
        }

        // Auto-stop scan after 10 seconds to save battery and resources
        scope.launch {
            delay(10000)
            if (_isScanning.value) {
                addLog("Scan timeout reached (10s)")
                stopScanning()
            }
        }
    }

    fun stopScanning() {
        if (!_isScanning.value) return
        
        addLog("Stopping scan")
        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    fun connect(device: BluetoothDevice) {
        if (connectionState.value == State.CONNECTING) return
        
        isManualDisconnect = false
        addLog("Connecting to ${device.name} (${device.address})")
        _connectionState.value = State.CONNECTING
        
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(30000) // 30 second timeout
            if (connectionState.value == State.CONNECTING) {
                addLog("ERROR: Connection timeout reached")
                disconnect()
                _connectionState.value = State.FAILED
                _statusMessage.value = "Connection timeout"
            }
        }
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        addLog("Disconnecting...")
        isManualDisconnect = true
        connectionTimeoutJob?.cancel()
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
        }
        bluetoothGatt = null
        _isConnected.value = false
        _isReady.value = true
        _connectionState.value = State.DISCONNECTED
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                addLog("Connected to $deviceAddress, requesting MTU...")
                // Request larger MTU for image uploads
                gatt.requestMtu(517)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                addLog("Disconnected from $deviceAddress")
                _isConnected.value = false
                _connectionState.value = State.DISCONNECTED
                _isReady.value = true
                bluetoothGatt = null
                connectionTimeoutJob?.cancel()

                // Auto-reconnect logic: If it wasn't manual, start scanning again
                if (!isManualDisconnect) {
                    addLog("Unexpected disconnect, starting auto-reconnect scan...")
                    scope.launch {
                        delay(2000) // Wait 2s before starting scan to let things settle
                        startScanning()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    addLog("Service verified! Secure connection established.")
                    _isConnected.value = true
                    _connectionState.value = State.CONNECTED
                    connectionTimeoutJob?.cancel()
                    
                    // Store last successful device
                    prefs.edit().putString("last_device", gatt.device.address).apply()

                    val characteristic = service.getCharacteristic(CHAR_RESPONSE_UUID)
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID)
                        descriptor?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            } else {
                                @Suppress("DEPRECATION")
                                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(it)
                            }
                        }
                    }
                } else {
                    addLog("ERROR: Security check failed: Required service not found")
                    disconnect()
                    _connectionState.value = State.FAILED
                    _statusMessage.value = "Not a compatible ESP32 device"
                }
            } else {
                addLog("ERROR: Service discovery failed with status: $status")
                _connectionState.value = State.FAILED
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            onCharacteristicChanged(gatt, characteristic, characteristic.value)
        }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (characteristic.uuid == CHAR_RESPONSE_UUID) {
            // Check for binary file data first
            if (value.size > 10 && value.sliceArray(0 until 10).decodeToString() == "FILE_DATA:") {
                val data = value.sliceArray(10 until value.size)
                fileBuffer?.let { buffer ->
                    System.arraycopy(data, 0, buffer, receivedBytes, data.size)
                    receivedBytes += data.size
                    _downloadProgress.value = receivedBytes.toFloat() / expectedFileSize.toFloat()
                }
                return
            }

            val message = value.decodeToString()
            if (message == "READY") {
                _isReady.value = true
            } else if (message.startsWith("FILE_START:")) {
                try {
                    val parts = message.split(":")
                    expectedFileSize = parts[1].toInt()
                    fileBuffer = ByteArray(expectedFileSize)
                    receivedBytes = 0
                    _downloadProgress.value = 0.05f
                    
                    // If slot is provided, update downloadingFilename
                    if (parts.size > 2) {
                        val slot = parts[2].toInt()
                        downloadingFilename = if (slot == 0) {
                            _capturedFiles.value.firstOrNull()?.filename
                        } else {
                            _capturedFiles.value.getOrNull(slot)?.filename
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BleManager", "Error starting file download", e)
                }
            } else if (message == "FILE_END") {
                _downloadedFile.value = fileBuffer
                _downloadProgress.value = 1.0f
                
                // Save internally if we have a filename
                downloadingFilename?.let { filename ->
                    fileBuffer?.let { data ->
                        saveDownloadedFileInternally(data, filename)
                        
                        // Update the captured file entry with verified data from the PCAP
                        val (verifiedMask, verifiedSize) = analyzePcap(data)
                        updateCapturedFileVerified(filename, verifiedMask, verifiedSize)
                    }
                }
                
                fileBuffer = null
                downloadingFilename = null
            } else if (message.startsWith("FILE_ERROR:")) {
                _downloadProgress.value = 0f
                _statusMessage.value = "Download failed: ${message.split(":")[1]}"
            } else if (message.startsWith("SCAN_RESULT:")) {
                    // Format: SCAN_RESULT:COUNT:INDEX:SSID:BSSID:RSSI:CH:SECURE
                    try {
                        val parts = message.split(":")
                        if (parts.size >= 8) {
                            val count = parts[1].toInt()
                            val index = parts[2].toInt()
                            val ssid = parts[3]
                            val bssid = parts[4]
                            val rssi = parts[5].toInt()
                            val channel = parts[6].toInt()
                            val secure = parts[7].toInt() == 1
                            
                            if (index == 0) {
                                _scanResults.value = emptyList()
                            }
                            
                            if (ssid != "NO_NETWORKS") {
                                val newItem = ScanResultItem(ssid, bssid, rssi, channel, secure, index)
                                _scanResults.value = _scanResults.value + newItem
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BleManager", "Error parsing scan result: $message", e)
                    }
                } else if (message.startsWith("HS_FRAME:")) {
                // Real-time EAPOL frame analysis from ESP32
                try {
                    val hexData = message.substring(9)
                    val frameData = hexToBytes(hexData)
                    val newMask = analyzeSingleFrame(frameData, _handshakeStatus.value.eapolMask)
                    if (newMask != _handshakeStatus.value.eapolMask) {
                        _handshakeStatus.value = _handshakeStatus.value.copy(eapolMask = newMask)
                    }
                } catch (e: Exception) {
                    Log.e("BleManager", "Error parsing handshake frame", e)
                }
            } else if (message.startsWith("HS_STATUS:")) {
                    // Format: HS_STATUS:FRAMES:EAPOL_MASK:CLIENTS:FILE_SIZE
                    try {
                        Log.d("BleManager", "Parsing HS_STATUS: $message")
                        val parts = message.split(":")
                        if (parts.size >= 5) {
                            val frames = parts[1].toInt()
                            val eapolMask = parts[2].toInt()
                            val clients = parts[3].toInt()
                            val fileSize = parts[4].toLong()
                            
                            Log.d("BleManager", "Handshake Update: frames=$frames, mask=$eapolMask, clients=$clients, size=$fileSize")
                            
                            _handshakeStatus.value = HandshakeStatus(
                                frames = frames,
                                eapolMask = _handshakeStatus.value.eapolMask or eapolMask, // Merge real-time app analysis with ESP feedback
                                clients = clients,
                                fileSize = fileSize,
                                isComplete = false
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("BleManager", "Error parsing handshake status: $message", e)
                    }
                } else if (message.startsWith("TEMP:")) {
                    try {
                        val temp = message.substring(5).toFloat()
                        _temperature.value = temp
                    } catch (e: Exception) {
                        Log.e("BleManager", "Error parsing temperature: $message", e)
                    }
                } else if (message == "ATTACK_SUCCESS" || message == "ATTACK_COMPLETED" || message == "ATTACK_FAILED" || message == "TIMEOUT" || message == "ATTACK_TIMEOUT") {
                    _handshakeStatus.value = _handshakeStatus.value.copy(isComplete = true)
                    
                    // Auto-save to file list
                    currentTargetNetwork?.let { network ->
                        val status = if (message == "ATTACK_SUCCESS") "SUCCESS" else if (message == "ATTACK_FAILED") "FAILED" else if (message.contains("TIMEOUT")) "TIMEOUT" else "COMPLETED"
                        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                        // Internal filename is just a unique identifier - sanitize SSID for filename
                        val sanitizedSsid = network.ssid.replace(Regex("[^a-zA-Z0-9]"), "_")
                        val filename = "${sanitizedSsid}_${System.currentTimeMillis()}.pcap"
                        downloadingFilename = filename
                        
                        // Only save if we actually captured something or it's a success
                        if (_handshakeStatus.value.fileSize > 0 || message == "ATTACK_SUCCESS") {
                            val capturedFile = CapturedFile(
                                filename = filename,
                                ssid = network.ssid,
                                bssid = network.bssid,
                                status = status,
                                size = "%.1f KB".format(_handshakeStatus.value.fileSize / 1024.0),
                                timestamp = timestamp,
                                format = "PCAP",
                                eapolMask = _handshakeStatus.value.eapolMask
                            )
                            addCapturedFile(capturedFile)
                    
                    // Auto-download after capture completion to internal storage
                    Log.d("BleManager", "Auto-downloading capture to internal storage: ${capturedFile.ssid}")
                    savePcap() 
                }
            }
                    
                    addLog("RECV: $message")
                    _statusMessage.value = message
                } else if (message != "CMD_OK" && message != "OK" && !message.contains("UNKNOWN_CMD")) {
                    addLog("RECV: $message")
                    _statusMessage.value = message
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                addLog("ERROR: Write failed with status: $status")
            }
        }
    }

    fun sendCommand(command: String) {
        Log.d("BleManager", "Sending command: $command")
        if (command == "CLEAR") {
            while (commandQueue.tryReceive().isSuccess) { /* drop */ }
        }
        commandQueue.trySend(command)
    }

    fun stopAttack() {
        Log.d("BleManager", "Manually stopping attack...")
        // Send STOP command to terminate attack on ESP32 immediately
        sendCommand("STOP")
    }

    fun savePcap(slot: Int = 0) {
        _downloadProgress.value = 0.01f // Show starting
        _downloadedFile.value = null
        fileBuffer = null
        expectedFileSize = 0
        receivedBytes = 0
        
        // If we're requesting a specific slot, set the target filename
        downloadingFilename = if (slot == 0) {
            _capturedFiles.value.firstOrNull()?.filename
        } else {
            _capturedFiles.value.getOrNull(slot)?.filename
        }
        
        if (slot > 0) {
            sendCommand("SAVE_PCAP:$slot")
        } else {
            sendCommand("SAVE_PCAP")
        }
    }

    fun sendBinaryCommand(data: ByteArray) {
        commandQueue.trySend(data)
    }

    private suspend fun performSendBinary(data: ByteArray) {
        val gatt = bluetoothGatt
        val service = gatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_COMMAND_UUID)

        if (gatt == null || service == null || characteristic == null) {
            addLog("ERROR: Cannot send binary, GATT/Service/Char not ready")
            _isReady.value = true
            return
        }

        addLog("SEND BIN: ${data.size} bytes")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun performSendCommand(command: String) {
        val gatt = bluetoothGatt
        val service = gatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_COMMAND_UUID)

        if (gatt == null || service == null || characteristic == null) {
            addLog("ERROR: Cannot send command, GATT/Service/Char not ready")
            _isReady.value = true
            return
        }

        addLog("SEND: $command")
        
        val bytes = command.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }
}
