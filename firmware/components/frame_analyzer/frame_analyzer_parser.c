/**
 * @file frame_analyzer_parser.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-05
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements parsing functionality
 */
#include "frame_analyzer_parser.h"

#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "arpa/inet.h"

#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_wifi_types.h"

#include "frame_analyzer_types.h"

static const char *TAG = "frame_analyzer:parser";

ESP_EVENT_DEFINE_BASE(FRAME_ANALYZER_EVENTS);

/**
 * @brief Debug function to print raw frame payload to serial output.
 * 
 * Iterates through the frame payload and prints each byte in hexadecimal format.
 * 
 * @param frame Pointer to the promiscuous packet to print.
 */
void print_raw_frame(const wifi_promiscuous_pkt_t *frame){
    for(unsigned i = 0; i < frame->rx_ctrl.sig_len; i++) {
        printf("%02x", frame->payload[i]);
    }
    printf("\n");
}

/**
 * @brief Debug function to print a MAC address in standard colon-separated format.
 * 
 * @param a Pointer to the 6-byte buffer containing the MAC address.
 */
void print_mac_address(const uint8_t *a){
    printf("%02x:%02x:%02x:%02x:%02x:%02x",
    a[0], a[1], a[2], a[3], a[4], a[5]);
    printf("\n");
}

/**
 * @brief Checks if the BSSID of the given frame matches the target BSSID.
 * 
 * @param frame Pointer to the captured promiscuous packet.
 * @param bssid Pointer to the target BSSID (6 bytes).
 * @return true if any of the MAC addresses (addr1, addr2, addr3) match the BSSID.
 */
bool is_frame_bssid_matching(wifi_promiscuous_pkt_t *frame, uint8_t *bssid) {
    data_frame_mac_header_t *mac_header = (data_frame_mac_header_t *) frame->payload;
    
    // Log occasionally to avoid spamming
    static uint32_t bssid_check_count = 0;
    if (++bssid_check_count % 100 == 0) {
        ESP_LOGD(TAG, "Checking BSSID match. Frame Addrs: "
                 "%02x:%02x:%02x:%02x:%02x:%02x | "
                 "%02x:%02x:%02x:%02x:%02x:%02x | "
                 "%02x:%02x:%02x:%02x:%02x:%02x",
                 mac_header->addr1[0], mac_header->addr1[1], mac_header->addr1[2],
                 mac_header->addr1[3], mac_header->addr1[4], mac_header->addr1[5],
                 mac_header->addr2[0], mac_header->addr2[1], mac_header->addr2[2],
                 mac_header->addr2[3], mac_header->addr2[4], mac_header->addr2[5],
                 mac_header->addr3[0], mac_header->addr3[1], mac_header->addr3[2],
                 mac_header->addr3[3], mac_header->addr3[4], mac_header->addr3[5]);
    }

    bool match = (memcmp(mac_header->addr1, bssid, 6) == 0) || 
                 (memcmp(mac_header->addr2, bssid, 6) == 0) || 
                 (memcmp(mac_header->addr3, bssid, 6) == 0);
    
    if (match) {
        ESP_LOGI(TAG, "BSSID Match found!");
    }
    
    return match;
}

/**
 * @brief Parses a data frame to extract the EAPoL packet.
 * 
 * This function handles various 802.11 frame complexities such as HT Control fields,
 * QoS data frames, and LLC SNAP headers to locate the EAPoL payload.
 * 
 * @param frame Pointer to the parsed data frame structure.
 * @return Pointer to the EAPoL packet, or NULL if not an EAPoL packet or if the frame is protected.
 */
eapol_packet_t *parse_eapol_packet(data_frame_t *frame) {
    uint8_t *frame_buffer = frame->body;

    if(frame->mac_header.frame_control.protected_frame == 1) {
        ESP_LOGD(TAG, "Protected frame, skipping...");
        return NULL;
    }

    // If HTC/Order bit is set, there is a 4-byte HT Control field
    if (frame->mac_header.frame_control.htc_order) {
        ESP_LOGD(TAG, "HT Control field present, skipping 4 bytes");
        frame_buffer += 4;
    }

    if(frame->mac_header.frame_control.subtype > 7) {
        ESP_LOGV(TAG, "QoS data frame");
        // Skipping QoS field (2 bytes)
        frame_buffer += 2;
    }

    // Skipping LLC SNAP header (6 bytes)
    frame_buffer += sizeof(llc_snap_header_t);

    // Check if frame is type of EAPoL (0x888E)
    uint16_t eth_type = (frame_buffer[0] << 8) | frame_buffer[1];
    if(eth_type == ETHER_TYPE_EAPOL) {
        ESP_LOGI(TAG, "EAPOL packet detected! (Type: 0x%04x)", eth_type);
        frame_buffer += 2;
        return (eapol_packet_t *) frame_buffer; 
    } else {
        // Occasionally log non-EAPOL data types for debugging
        static uint32_t non_eapol_count = 0;
        if (++non_eapol_count % 100 == 0) {
            ESP_LOGD(TAG, "Data frame is not EAPOL (Type: 0x%04x)", eth_type);
        }
    }
    return NULL;
}

/**
 * @brief Extracts the EAPoL-Key body from an EAPoL packet.
 * 
 * @param eapol_packet Pointer to the EAPoL packet.
 * @return Pointer to the EAPoL-Key packet body, or NULL if the packet is not of type EAPOL_KEY.
 */
eapol_key_packet_t *parse_eapol_key_packet(eapol_packet_t *eapol_packet){
    if(eapol_packet->header.packet_type != EAPOL_KEY){
        ESP_LOGD(TAG, "Not an EAPoL-Key packet.");
        return NULL;
    }
    return (eapol_key_packet_t *) eapol_packet->packet_body;
}

/**
 * @brief Parses key data fields to extract PMKIDs into a linked list.
 * 
 * Iterates through the EAPoL-Key data buffer, identifying and extracting
 * PMKID Key Data Elements (KDEs). Each found PMKID is allocated and added
 * to a linked list.
 * 
 * @param key_data Pointer to the start of the key data buffer.
 * @param length Length of the key data buffer.
 * @return Pointer to the head of the linked list of PMKID items, or NULL if none found.
 */
static pmkid_item_t *parse_pmkid_from_key_data(uint8_t *key_data, const uint16_t length){
    uint8_t *key_data_index = key_data;
    uint8_t *key_data_max_index = key_data + length;

    pmkid_item_t *pmkid_item_head = NULL;
    key_data_field_t *key_data_field;
    do{
        key_data_field = (key_data_field_t *) key_data_index;

        ESP_LOGV(TAG, "EAPOL-Key -> Key-Data -> type=%x; length=%x; oui=%x; data_type=%x",
                    key_data_field->type, 
                    key_data_field->length, 
                    key_data_field->oui,
                    key_data_field->data_type);
        
        if(key_data_field->type != KEY_DATA_TYPE){
            ESP_LOGD(TAG, "Wrong type %x (expected %x)", key_data_field->type, KEY_DATA_TYPE);
            continue;
        }

        if(ntohl(key_data_field->oui) != KEY_DATA_OUI_IEEE80211){
            ESP_LOGD(TAG, "Wrong OUI %x (expected %x)", key_data_field->oui, KEY_DATA_OUI_IEEE80211);
            continue;
        }

        if(key_data_field->data_type != KEY_DATA_DATA_TYPE_PMKID_KDE){
            ESP_LOGD(TAG, "Wrong data type %x (expected %x)", key_data_field->data_type, KEY_DATA_DATA_TYPE_PMKID_KDE);
            continue;
        }

        ESP_LOGI(TAG, "Found PMKID: ");
        pmkid_item_t *pmkid_item = (pmkid_item_t *) malloc(sizeof(pmkid_item_t));
        pmkid_item->next = pmkid_item_head;
        pmkid_item_head = pmkid_item;
        for(unsigned i = 0; i < 16; i++){
            pmkid_item->pmkid[i] = key_data_field->data[i];
            printf("%02x", pmkid_item->pmkid[i]);
        }
        printf("\n");

    } while((key_data_index = key_data_field->data + key_data_field->length - 4 + 1) < key_data_max_index); 

    return pmkid_item_head;
}

/**
 * @brief Attempts to extract PMKIDs from an EAPoL-Key packet.
 * 
 * Verifies that the packet contains unencrypted key data before attempting to parse.
 * 
 * @param eapol_key Pointer to the EAPoL-Key packet.
 * @return Linked list of found PMKID items, or NULL if none found or data is encrypted.
 */
pmkid_item_t *parse_pmkid(eapol_key_packet_t *eapol_key){
    if(eapol_key->key_data_length == 0){
        ESP_LOGD(TAG, "Empty Key Data");
        return NULL;
    }

    if(eapol_key->key_information.encrypted_key_data == 1){
        ESP_LOGD(TAG, "Key Data encrypted");
        return NULL;
    }

    return parse_pmkid_from_key_data(eapol_key->key_data, ntohs(eapol_key->key_data_length));
}