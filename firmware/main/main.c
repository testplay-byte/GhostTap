/**
 * @file main.c
 * @brief Entry point for the ESP32 project.
 * 
 * This file initializes the system, including NVS, OLED display, 
 * application logic, and BLE communication.
 */

#include <stdio.h>
#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_event.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "oled_display.h"
#include "ble_comm.h"
#include "app_logic.h"

static const char *TAG = "MAIN";

// OLED Pins (match previous configuration)
#define OLED_SDA_PIN GPIO_NUM_5
#define OLED_SCL_PIN GPIO_NUM_6

/**
 * @brief Main application entry point.
 */
void app_main(void) {
    // 1. Initialize NVS (required for BLE and WiFi storage)
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    // 1.1 Initialize Event Loop (Required for WiFi, BLE, and system events)
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    // 2. Initialize OLED Display (SSD1306)
    ESP_LOGI(TAG, "Initializing OLED...");
    if (!oled_init(OLED_SDA_PIN, OLED_SCL_PIN)) {
        ESP_LOGE(TAG, "OLED initialization failed!");
        return;
    }

    // 3. Show Boot Screen animation
    ESP_LOGI(TAG, "Showing boot screen...");
    for (int i = 0; i < 50; i++) {
        oled_show_boot_screen();
        oled_update();
        animation_tick++;
        vTaskDelay(pdMS_TO_TICKS(30));
    }
    
    // 4. Initialize App Logic (Initializes internal tasks like temp monitoring)
    ESP_LOGI(TAG, "Initializing App Logic...");
    app_logic_init();

    // 5. Initialize BLE Communication (GATT Server for remote control)
    ESP_LOGI(TAG, "Initializing BLE...");
    if (!ble_comm_init("GhostTap")) {
        ESP_LOGE(TAG, "BLE initialization failed!");
        return;
    }

    // Show Orbital Animation until a BLE client connects
    ESP_LOGI(TAG, "Showing orbital animation until BLE connected...");
    while (!ble_comm_is_connected()) {
        oled_clear();
        oled_anim_circles(animation_tick++);
        oled_update();
        vTaskDelay(pdMS_TO_TICKS(30));
    }

    // Indicate successful connection on the screen
    ESP_LOGI(TAG, "BLE connected. Switching to App Logic control.");
    oled_clear();
    oled_draw_string(5, 15, "CONNECTED!", false);
    oled_update();
    vTaskDelay(pdMS_TO_TICKS(1000));

    // 6. Main loop: Just stay alive, app_logic and display tasks handle the work
    while (1) {
        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}
