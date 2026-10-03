/**
 * @file sniffer.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-05
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements sniffer logic.
 */
#include "sniffer.h"

#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_err.h"
#include "esp_event.h"
#include "esp_wifi.h"
#include "esp_wifi_types.h"

static const char *TAG = "sniffer"; 

ESP_EVENT_DEFINE_BASE(SNIFFER_EVENTS);

/**
 * @brief Callback for promiscuous reciever. 
 * 
 * It forwards captured frames into event pool and sorts them based on their type
 * - Data
 * - Management
 * - Control
 * 
 * @param buf 
 * @param type 
 */
static void frame_handler(void *buf, wifi_promiscuous_pkt_type_t type) {
    wifi_promiscuous_pkt_t *frame = (wifi_promiscuous_pkt_t *) buf;

    static uint32_t pkt_count = 0;
    if (++pkt_count % 1000 == 0) {
        ESP_LOGD(TAG, "Captured 1000 frames... (Total: %u)", (unsigned int)pkt_count);
    }

    int32_t event_id;
    switch (type) {
        case WIFI_PKT_DATA:
            event_id = SNIFFER_EVENT_CAPTURED_DATA;
            break;
        case WIFI_PKT_MGMT:
            event_id = SNIFFER_EVENT_CAPTURED_MGMT;
            break;
        case WIFI_PKT_CTRL:
            event_id = SNIFFER_EVENT_CAPTURED_CTRL;
            break;
        default:
            return;
    }

    ESP_ERROR_CHECK(esp_event_post(SNIFFER_EVENTS, event_id, frame, frame->rx_ctrl.sig_len + sizeof(wifi_promiscuous_pkt_t), portMAX_DELAY));
}

/**
 * @see https://docs.espressif.com/projects/esp-idf/en/latest/esp32/api-reference/network/esp_wifi.html#_CPPv425wifi_promiscuous_filter_t
 */
/**
 * @brief Configures the promiscuous mode filter for different frame types.
 * 
 * @param data Enable/disable capturing data frames.
 * @param mgmt Enable/disable capturing management frames.
 * @param ctrl Enable/disable capturing control frames.
 */
void wifictl_sniffer_filter_frame_types(bool data, bool mgmt, bool ctrl) {
    wifi_promiscuous_filter_t filter = { .filter_mask = 0 };
    if(data) {
        filter.filter_mask |= WIFI_PROMIS_FILTER_MASK_DATA;
    }
    if(mgmt) {
        filter.filter_mask |= WIFI_PROMIS_FILTER_MASK_MGMT;
    }
    if(ctrl) {
        filter.filter_mask |= WIFI_PROMIS_FILTER_MASK_CTRL;
    }
    esp_wifi_set_promiscuous_filter(&filter);
}

/**
 * @brief Enables promiscuous mode (sniffer) on a specific channel.
 * 
 * Ensures the device is in APSTA mode, sets the target channel, 
 * disconnects all associated stations, and registers the frame handler.
 * 
 * @param channel The channel to sniff on (1-13).
 */
void wifictl_sniffer_start(uint8_t channel) {
    ESP_LOGI(TAG, "Starting promiscuous mode on channel %u...", channel);
    
    // Set channel on both interfaces if possible, but mainly hardware
    esp_wifi_set_promiscuous(false);
    
    // Ensure we are in APSTA mode to support sniffer and maintain control connection
    wifi_mode_t current_mode;
    esp_wifi_get_mode(&current_mode);
    if (current_mode != WIFI_MODE_APSTA) {
        ESP_LOGI(TAG, "Changing mode from %d to APSTA to support sniffer and control", current_mode);
        esp_wifi_set_mode(WIFI_MODE_APSTA);
    } else {
        ESP_LOGD(TAG, "Already in APSTA mode");
    }
    
    esp_err_t err = esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to set channel: %s", esp_err_to_name(err));
    }
    
    // ESP32 cannot switch port, if there is some STA connected to AP
    ESP_LOGD(TAG, "Kicking all connected STAs from AP");
    esp_wifi_deauth_sta(0);
    
    esp_wifi_set_promiscuous_rx_cb(&frame_handler);
    err = esp_wifi_set_promiscuous(true);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to enable promiscuous mode: %s", esp_err_to_name(err));
    } else {
        ESP_LOGI(TAG, "Promiscuous mode enabled successfully");
    }
}

/**
 * @brief Disables promiscuous mode and stops frame capture.
 */
void wifictl_sniffer_stop() {
    ESP_LOGI(TAG, "Stopping promiscuous mode...");
    esp_wifi_set_promiscuous(false);
}