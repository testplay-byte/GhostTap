#ifndef APP_LOGIC_H
#define APP_LOGIC_H

/**
 * @brief Main command handler for string-based protocol
 * @param cmd The command string received via BLE
 */
void app_handle_command(const char *cmd);
void app_handle_binary_command(const uint8_t *data, size_t len);

/**
 * @brief Initialize application logic
 */
void app_logic_init(void);
void app_set_time(int h, int m, int s);

#endif // APP_LOGIC_H
