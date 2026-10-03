package com.ghosttap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ghosttap.app.ui.ScanScreen
import com.ghosttap.app.ui.ControlScreen
import com.ghosttap.app.ui.HackerModeScreen
import com.ghosttap.app.ui.theme.GhostTapTheme

import java.util.Calendar
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val bleManager by lazy { BleManager(this) }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        checkManageExternalStoragePermission()
        
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Some permissions were denied. Certain features may not work.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", packageName))
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val timeSyncRunnable = object : Runnable {
        override fun run() {
            if (bleManager.isConnected.value) {
                val calendar = Calendar.getInstance()
                val h = calendar.get(Calendar.HOUR_OF_DAY)
                val m = calendar.get(Calendar.MINUTE)
                val s = calendar.get(Calendar.SECOND)
                bleManager.sendCommand("SYNC_TIME:$h:$m:$s")
            }
            handler.postDelayed(this, 30000) // Sync every 30 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions on startup
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())

        enableEdgeToEdge()
        handler.post(timeSyncRunnable)
        setContent {
            GhostTapTheme {
                val navController = rememberNavController()
                val isConnected by bleManager.isConnected.collectAsState()

                LaunchedEffect(isConnected) {
                    if (isConnected) {
                        // Only auto-navigate to control if we are on the scan screen
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute == "scan") {
                            navController.navigate("control") {
                                popUpTo("scan") { inclusive = true }
                            }
                        }
                    }
                }
                
                NavHost(navController = navController, startDestination = "scan") {
                    composable("scan") {
                        ScanScreen(bleManager) { _ ->
                            navController.navigate("control")
                        }
                    }
                    composable("control") {
                        ControlScreen(
                            bleManager = bleManager,
                            onDisconnect = {
                                bleManager.disconnect()
                                navController.navigate("scan") {
                                    popUpTo("control") { inclusive = true }
                                }
                            },
                            onHackerMode = {
                                navController.navigate("hacker_mode")
                            }
                        )
                    }
                    composable("hacker_mode") {
                        HackerModeScreen(bleManager) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.onDestroy()
        handler.removeCallbacks(timeSyncRunnable)
    }
}