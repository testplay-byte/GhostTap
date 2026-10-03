/**
 * @file pcap_serializer.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-05
 * @copyright Copyright (c) 2021
 * 
 * @brief Implementation of PCAP serializer
 */
#include "pcap_serializer.h"

#include <stdint.h>
#include <string.h>
#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_err.h"

static const char *TAG = "pcap_serializer";


/**
 * @brief Constanst according to reference
 * 
 * @see Ref: https://gitlab.com/wireshark/wireshark/-/wikis/Development/LibpcapFileFormat#global-header
 */
//@{
#define SNAPLEN 65535
#define PCAP_MAGIC_NUMBER 0xa1b2c3d4
//@}

/**
 * @brief Constanst according to reference
 * 
 * @see Ref: http://www.tcpdump.org/linktypes.html (LINKTYPE_IEEE802_11)
 */
#define LINKTYPE_IEEE802_11 105

#define PCAP_SLOTS 2
static unsigned pcap_sizes[PCAP_SLOTS] = {0};
static uint8_t *pcap_buffers[PCAP_SLOTS] = {NULL, NULL};

/**
 * @brief Initializes the PCAP serializer.
 * 
 * Rotates existing capture slots (Slot 0 moves to Slot 1) and allocates a new
 * buffer for Slot 0 with a PCAP global header.
 * 
 * @return Pointer to the newly allocated PCAP buffer in Slot 0, or NULL on failure.
 */
uint8_t *pcap_serializer_init(){
    // Rotate slots: Slot 0 moves to Slot 1, new capture starts in Slot 0
    // First, free memory in Slot 1
    if (pcap_buffers[1]) {
        free(pcap_buffers[1]);
        pcap_buffers[1] = NULL;
        pcap_sizes[1] = 0;
    }

    // Move Slot 0 to Slot 1
    pcap_buffers[1] = pcap_buffers[0];
    pcap_sizes[1] = pcap_sizes[0];

    // Initialize Slot 0
    pcap_global_header_t pcap_global_header = {
        .magic_number = PCAP_MAGIC_NUMBER,
        .version_major = 2,
        .version_minor = 4,
        .thiszone = 0,
        .sigfigs = 0,
        .snaplen = SNAPLEN,
        .network = LINKTYPE_IEEE802_11
    };
    
    pcap_buffers[0] = (uint8_t *)malloc(sizeof(pcap_global_header_t));
    if (pcap_buffers[0] == NULL) {
        ESP_LOGE(TAG, "Error allocating PCAP buffer!");
        pcap_sizes[0] = 0;
        return NULL;
    }
    
    pcap_sizes[0] = sizeof(pcap_global_header_t);
    memcpy(pcap_buffers[0], &pcap_global_header, sizeof(pcap_global_header_t));
    
    return pcap_buffers[0];
}

/**
 * @brief Appends a new frame to the current PCAP buffer (Slot 0).
 * 
 * Allocates/reallocates memory as needed and adds a PCAP record header.
 * 
 * @param buffer Pointer to the raw frame bytes.
 * @param size Size of the frame in bytes.
 * @param ts_usec Timestamp of the frame in microseconds.
 */
void pcap_serializer_append_frame(const uint8_t *buffer, unsigned size, unsigned ts_usec){
    if(size == 0 || pcap_buffers[0] == NULL){
        ESP_LOGD(TAG, "Frame size is 0 or buffer not initialized. Not appending anything.");
        return;
    }
    // Ref: https://gitlab.com/wireshark/wireshark/-/wikis/Development/LibpcapFileFormat#record-packet-header
    pcap_record_header_t pcap_record_header = {
        .ts_sec = ts_usec / 1000000,
        .ts_usec = ts_usec % 1000000,
        .incl_len = size,
        .orig_len = size,
    };
    // Ref: https://gitlab.com/wireshark/wireshark/-/wikis/Development/LibpcapFileFormat#record-packet-header
    // Stored packet/frame cannot be larger than SNAPLEN
    if(size > SNAPLEN){
        size = SNAPLEN;
        pcap_record_header.incl_len = SNAPLEN;
    }

    uint8_t *reallocated_pcap_buffer = realloc(pcap_buffers[0], pcap_sizes[0] + sizeof(pcap_record_header_t) + size);
    if(reallocated_pcap_buffer == NULL){
        ESP_LOGE(TAG, "Error reallocating PCAP buffer! PCAP buffer may not be complete.");
        return;
    }
    memcpy(&reallocated_pcap_buffer[pcap_sizes[0]], &pcap_record_header, sizeof(pcap_record_header_t));
    memcpy(&reallocated_pcap_buffer[pcap_sizes[0] + sizeof(pcap_record_header_t)], buffer, size);
    pcap_buffers[0] = reallocated_pcap_buffer;
    pcap_sizes[0] += sizeof(pcap_record_header_t) + size;
}

/**
 * @brief Deinitializes the PCAP serializer.
 * 
 * Frees all allocated buffers in all slots.
 */
void pcap_serializer_deinit(){
    for (int i = 0; i < PCAP_SLOTS; i++) {
        if (pcap_buffers[i]) {
            free(pcap_buffers[i]);
            pcap_buffers[i] = NULL;
            pcap_sizes[i] = 0;
        }
    }
}

/**
 * @brief Returns the size of the current PCAP buffer (Slot 0).
 * 
 * @return Size in bytes.
 */
unsigned pcap_serializer_get_size(){
    return pcap_sizes[0];
}

/**
 * @brief Returns a pointer to the current PCAP buffer (Slot 0).
 * 
 * @return Pointer to the buffer.
 */
uint8_t *pcap_serializer_get_buffer(){
    return pcap_buffers[0];
}

/**
 * @brief Returns the size of a specific PCAP buffer slot.
 * 
 * @param slot Slot index (0 for latest, 1 for previous).
 * @return Size in bytes, or 0 if the slot is invalid.
 */
unsigned pcap_serializer_get_size_at(int slot){
    if (slot >= 0 && slot < PCAP_SLOTS) return pcap_sizes[slot];
    return 0;
}

/**
 * @brief Returns a pointer to a specific PCAP buffer slot.
 * 
 * @param slot Slot index (0 for latest, 1 for previous).
 * @return Pointer to the buffer, or NULL if the slot is invalid.
 */
uint8_t *pcap_serializer_get_buffer_at(int slot){
    if (slot >= 0 && slot < PCAP_SLOTS) return pcap_buffers[slot];
    return NULL;
}