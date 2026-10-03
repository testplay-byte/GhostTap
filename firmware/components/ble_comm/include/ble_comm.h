#ifndef BLE_COMM_H
#define BLE_COMM_H

#include <stdint.h>
#include <stdbool.h>

/**
 * @brief Initialize BLE stack and GATTS server
 * @param device_name The name the device will broadcast
 * @return true if success, false otherwise
 */
bool ble_comm_init(const char *device_name);

/**
 * @brief Send a response/notification to the connected Android device
 * @param data The string data to send
 * @return true if success
 */
bool ble_comm_send_response(const char *data);

/**
 * @brief Send binary data to the connected Android device
 * @param data The binary data to send
 * @param len The length of the data
 * @return true if success
 */
bool ble_comm_send_binary_response(const uint8_t *data, size_t len);

/**
 * @brief Check if a device is currently connected via BLE
 * @return true if connected
 */
bool ble_comm_is_connected(void);

#endif // BLE_COMM_H
