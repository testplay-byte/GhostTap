/**
 * @file ap_scanner.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-05
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements AP scanning functionality.
 */
#include "ap_scanner.h"

#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_err.h"
#include "esp_wifi.h"

#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "esp_event.h"

static const char* TAG = "wifi_controller/ap_scanner";
/**
 * @brief Stores last scanned AP records into linked list.
 * 
 */
static wifictl_ap_records_t ap_records;
static bool is_scanning = false;
static SemaphoreHandle_t scan_sem = NULL;

/**
 * @brief Event handler for Wi-Fi scan completion.
 */
static void scan_done_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data) {
    if (event_id == WIFI_EVENT_SCAN_DONE) {
        if (scan_sem) {
            xSemaphoreGive(scan_sem);
        }
    }
}

/**
 * @brief Checks if an AP scan is currently in progress.
 * @return true if scanning, false otherwise.
 */
bool wifictl_is_scanning() {
    return is_scanning;
}

/**
 * @brief Performs an active scan for nearby Access Points.
 * 
 * This is a blocking call (from the caller's perspective) but uses non-blocking
 * Wi-Fi scan with a semaphore to allow other FreeRTOS tasks to run while waiting.
 */
void wifictl_scan_nearby_aps(){
    ESP_LOGD(TAG, "Scanning nearby APs...");
    
    if (scan_sem == NULL) {
        scan_sem = xSemaphoreCreateBinary();
        ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, WIFI_EVENT_SCAN_DONE, &scan_done_handler, NULL));
    }

    is_scanning = true;

    ap_records.count = CONFIG_SCAN_MAX_AP;

    wifi_scan_config_t scan_config = {
        .ssid = NULL,
        .bssid = NULL,
        .channel = 0,
        .scan_type = WIFI_SCAN_TYPE_ACTIVE
    };
    
    // Use non-blocking scan start
    esp_err_t err = esp_wifi_scan_start(&scan_config, false);
    if (err == ESP_OK) {
        // Wait for scan completion (allows other tasks to run)
        xSemaphoreTake(scan_sem, pdMS_TO_TICKS(10000)); // 10s timeout
    } else {
        ESP_LOGE(TAG, "Scan start failed: %s", esp_err_to_name(err));
    }

    ESP_ERROR_CHECK(esp_wifi_scan_get_ap_records(&ap_records.count, ap_records.records));
    ESP_LOGI(TAG, "Found %u APs:", ap_records.count);
    for (int i = 0; i < ap_records.count; i++) {
        ESP_LOGI(TAG, "AP %d: SSID=%-32s, BSSID=%02x:%02x:%02x:%02x:%02x:%02x, Channel=%2d, RSSI=%d",
                 i, (char *)ap_records.records[i].ssid, // Changed to 0-based index for consistency
                 ap_records.records[i].bssid[0], ap_records.records[i].bssid[1], ap_records.records[i].bssid[2],
                 ap_records.records[i].bssid[3], ap_records.records[i].bssid[4], ap_records.records[i].bssid[5],
                 ap_records.records[i].primary, ap_records.records[i].rssi);
    }
    ESP_LOGD(TAG, "Scan done.");
    is_scanning = false;
}

/**
 * @brief Retrieves the results of the last AP scan.
 * @return Pointer to the wifictl_ap_records_t structure containing the list of APs.
 */
const wifictl_ap_records_t *wifictl_get_ap_records() {
    return &ap_records;
}

/**
 * @brief Retrieves a single AP record by its index in the last scan results.
 * 
 * @param index The index of the AP record to retrieve.
 * @return Pointer to the wifi_ap_record_t, or NULL if the index is out of bounds.
 */
const wifi_ap_record_t *wifictl_get_ap_record(unsigned index) {
    if(index > ap_records.count){
        ESP_LOGE(TAG, "Index out of bounds! %u records available, but %u requested", ap_records.count, index);
        return NULL;
    }
    return &ap_records.records[index];
}

/**
 * @brief Clears the internal AP scan results.
 */
void wifictl_clear_ap_records() {
    ap_records.count = 0;
}