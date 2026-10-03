/**
 * @file frame_analyzer.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-05
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements frame analysis
 */
#include "frame_analyzer.h"

#include <stdint.h>
#include <string.h>

#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_err.h"
#include "esp_event.h"

#include "wifi_controller.h"
#include "frame_analyzer_parser.h"

static const char *TAG = "frame_analyzer";
static uint8_t target_bssid[6];
static search_type_t search_type = -1;


/**
 * @brief Analyzes data frames captured by the sniffer.
 * 
 * Filters frames by BSSID, detects active clients, and parses EAPOL packets 
 * to find handshakes or PMKIDs based on the current search type.
 *  
 * @param args Not used.
 * @param event_base Expected SNIFFER_EVENTS.
 * @param event_id Expected SNIFFER_EVENT_CAPTURED_DATA.
 * @param event_data Pointer to the captured wifi_promiscuous_pkt_t.
 */
static void data_frame_handler(void *args, esp_event_base_t event_base, int32_t event_id, void *event_data) {
    wifi_promiscuous_pkt_t *frame = (wifi_promiscuous_pkt_t *) event_data;

    if(!is_frame_bssid_matching(frame, target_bssid)){
        return;
    }

    // Client detection: Extract the client MAC address
    // In 802.11 DATA frames, if BSSID is one of the addresses, the other is likely the client.
    data_frame_mac_header_t *mac_header = (data_frame_mac_header_t *) frame->payload;
    uint8_t *client_mac = NULL;
    
    // Check which address is NOT the BSSID
    if (memcmp(mac_header->addr1, target_bssid, 6) != 0 && memcmp(mac_header->addr1, "\xff\xff\xff\xff\xff\xff", 6) != 0) {
        client_mac = mac_header->addr1;
    } else if (memcmp(mac_header->addr2, target_bssid, 6) != 0 && memcmp(mac_header->addr2, "\xff\xff\xff\xff\xff\xff", 6) != 0) {
        client_mac = mac_header->addr2;
    }

    if (client_mac) {
        // Post client detected event
        esp_event_post(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_CLIENT_DETECTED, client_mac, 6, portMAX_DELAY);
    }

    ESP_LOGD(TAG, "Matching DATA frame from target BSSID");

    eapol_packet_t *eapol_packet = parse_eapol_packet((data_frame_t *) frame->payload);
    if(eapol_packet == NULL){
        return;
    }

    ESP_LOGI(TAG, "EAPOL packet detected!");

    eapol_key_packet_t *eapol_key_packet = parse_eapol_key_packet(eapol_packet);
    if(eapol_key_packet == NULL){
        // Not an EAPOL-Key frame (e.g. might be EAP-Identity)
        return;
    }

    if(search_type == SEARCH_HANDSHAKE){
        // Forward the EAPOL-Key frame for 4-way handshake reconstruction
        ESP_ERROR_CHECK_WITHOUT_ABORT(esp_event_post(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_EAPOLKEY_FRAME, frame, sizeof(wifi_promiscuous_pkt_t) + frame->rx_ctrl.sig_len, portMAX_DELAY));
        return;
    }

    if(search_type == SEARCH_PMKID){
        pmkid_item_t *pmkid_items;
        // Attempt to extract PMKID from the EAPOL M1 frame
        if((pmkid_items = parse_pmkid(eapol_key_packet)) == NULL){
            return;
        }
        ESP_ERROR_CHECK(esp_event_post(FRAME_ANALYZER_EVENTS, DATA_FRAME_EVENT_PMKID, &pmkid_items, sizeof(pmkid_item_t *), portMAX_DELAY));
        return;
    }
}

/**
 * @brief Starts the frame analysis process.
 * 
 * Sets the target BSSID and search type, then registers the sniffer data handler.
 * 
 * @param search_type_arg Type of data to search for (HANDSHAKE or PMKID).
 * @param bssid Pointer to the 6-byte BSSID of the target AP.
 */
void frame_analyzer_capture_start(search_type_t search_type_arg, const uint8_t *bssid){
    ESP_LOGI(TAG, "Frame analysis started...");
    search_type = search_type_arg;
    memcpy(&target_bssid, bssid, 6);
    ESP_ERROR_CHECK(esp_event_handler_register(SNIFFER_EVENTS, SNIFFER_EVENT_CAPTURED_DATA, &data_frame_handler, NULL));
}

/**
 * @brief Stops the frame analysis process and unregisters handlers.
 */
void frame_analyzer_capture_stop(){
    ESP_ERROR_CHECK(esp_event_handler_unregister(SNIFFER_EVENTS, SNIFFER_EVENT_CAPTURED_DATA, &data_frame_handler));
}
