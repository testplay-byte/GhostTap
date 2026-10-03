/**
 * @file attack.c
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-02
 * @copyright Copyright (c) 2021
 * 
 * @brief Implements common attack wrapper.
 */

#include "attack.h"

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_err.h"
#include "esp_event.h"
#include "esp_timer.h"

#include "attack_pmkid.h"
#include "attack_handshake.h"
#include "attack_dos.h"
#include "attack_beacon.h"
#include "wifi_controller.h"
#include "pcap_serializer.h"

static const char* TAG = "attack";
static attack_status_t attack_status = { .state = READY, .type = -1, .content_size = 0, .content = NULL };
static esp_timer_handle_t attack_timeout_handle;

/**
 * @brief Retrieves the current attack status.
 * @return Pointer to the global attack_status_t structure.
 */
const attack_status_t *attack_get_status() {
    return &attack_status;
}

/**
 * @brief Updates the global attack state and manages the associated timeout timer.
 * 
 * When transitioning to a terminal state (FINISHED or TIMEOUT), the timeout 
 * timer is automatically stopped to prevent redundant callbacks.
 * 
 * @param state The new attack_state_t to transition to.
 */
void attack_update_status(attack_state_t state) {
    attack_status.state = state;
    // Stop timeout timer if the attack is no longer active
    if(state == FINISHED || state == TIMEOUT) {
        ESP_LOGD(TAG, "Stopping attack timeout timer (state=%d)", state);
        esp_timer_stop(attack_timeout_handle);
    } 
}

/**
 * @brief Appends binary data to the attack status result buffer.
 * 
 * Dynamically reallocates the `attack_status.content` buffer to accommodate 
 * the new data. Includes basic error handling for allocation failures and 
 * zero-size requests.
 * 
 * @param buffer Pointer to the data to append.
 * @param size Size of the data to append in bytes.
 */
void attack_append_status_content(uint8_t *buffer, unsigned size){
    if(size == 0){
        ESP_LOGE(TAG, "Size can't be 0 if you want to reallocate");
        return;
    }
    // temporarily save new location in case of realloc failure to preserve current content
    char *reallocated_content = realloc(attack_status.content, attack_status.content_size + size);
    if(reallocated_content == NULL){
        ESP_LOGE(TAG, "Error reallocating status content! Status content may not be complete.");
        return;
    }
    // copy new data after current content
    memcpy(&reallocated_content[attack_status.content_size], buffer, size);
    attack_status.content = reallocated_content;
    attack_status.content_size += size;
}

/**
 * @brief Allocates a new buffer for the attack result content.
 * @param size Size to allocate in bytes.
 * @return Pointer to the allocated buffer or NULL.
 */
char *attack_alloc_result_content(unsigned size) {
    attack_status.content_size = size;
    attack_status.content = (char *) malloc(size);
    return attack_status.content;
}

/**
 * @brief Callback function for attack timeout timer.
 * 
 * This function is called when attack times out. 
 * It updates attack status state to TIMEOUT.
 * It calls appropriate abort functions based on current attack type.
 * @param arg not used.
 */
static void attack_timeout(void* arg){
    ESP_LOGI(TAG, "Attack timed out - stopping attack resources");
    
    // Stop the timeout timer immediately to prevent any re-triggering
    esp_timer_stop(attack_timeout_handle);
    
    attack_type_t type = attack_status.type;
    ESP_LOGI(TAG, "Timeout for attack type: %d", type);

    switch(type) {
        case ATTACK_TYPE_PMKID:
            ESP_LOGI(TAG, "Aborting PMKID attack...");
            attack_pmkid_stop();
            break;
        case ATTACK_TYPE_HANDSHAKE:
            ESP_LOGI(TAG, "Abort HANDSHAKE attack...");
            attack_handshake_stop();
            break;
        case ATTACK_TYPE_PASSIVE:
            ESP_LOGI(TAG, "Abort PASSIVE attack...");
            break;
        case ATTACK_TYPE_DOS:
            ESP_LOGI(TAG, "Abort DOS attack...");
            attack_dos_stop();
            break;
        case ATTACK_TYPE_BEACON:
            ESP_LOGI(TAG, "Abort BEACON attack...");
            attack_beacon_stop();
            break;
        default:
            ESP_LOGE(TAG, "Unknown attack type %d. Not aborting anything", type);
    }

    attack_update_status(TIMEOUT);
}

/**
 * @brief Resets the attack status to READY and frees associated memory.
 */
void attack_reset() {
    ESP_LOGD(TAG, "Resetting attack status...");
    
    // Ensure attack is stopped before resetting to avoid resource leaks
    if (attack_status.state == RUNNING) {
        attack_stop();
    }

    if(attack_status.content){
        free(attack_status.content);
        attack_status.content = NULL;
    }
    attack_status.content_size = 0;
    attack_status.type = -1;
    attack_status.state = READY;
}

/**
 * @brief Initialises common attack resources, including the timeout timer.
 */
void attack_init(){
    const esp_timer_create_args_t attack_timeout_args = {
        .callback = &attack_timeout
    };
    ESP_ERROR_CHECK(esp_timer_create(&attack_timeout_args, &attack_timeout_handle));
}

/**
 * @brief Forcefully stops any currently running attack and cleans up its resources.
 */
void attack_stop() {
    // Stop the global timeout timer
    esp_timer_stop(attack_timeout_handle);
    
    // Call type-specific stop functions
    switch(attack_status.type) {
        case ATTACK_TYPE_PMKID: attack_pmkid_stop(); break;
        case ATTACK_TYPE_HANDSHAKE: attack_handshake_stop(); break;
        case ATTACK_TYPE_DOS: attack_dos_stop(); break;
        case ATTACK_TYPE_BEACON: attack_beacon_stop(); break;
        default: break;
    }
    attack_update_status(FINISHED);
}

/**
 * @brief Configures and starts a new attack session.
 * 
 * If an attack is already running, it is stopped first. The function initializes 
 * the attack state, clears any existing timers, and dispatches to the appropriate 
 * attack module (PMKID, Handshake, DoS, or Beacon).
 * 
 * @param config Pointer to the attack_config_t structure containing type, target, and duration.
 */
void attack_start(attack_config_t *config) {
    if (attack_status.state == RUNNING) {
        ESP_LOGI(TAG, "Attack already running, stopping first...");
        attack_stop();
    }
    
    // Clear any existing timeout timer just in case
    esp_timer_stop(attack_timeout_handle);
    
    attack_status.state = RUNNING;
    attack_status.type = config->type;
    
    // Log the start of a new attack session for debugging
    ESP_LOGI(TAG, "***************************************************");
    ESP_LOGI(TAG, "* STARTING ATTACK: %-30d *", config->type);
    ESP_LOGI(TAG, "***************************************************");
    
    // Dispatch to the specific attack module
    switch(config->type) {
        case ATTACK_TYPE_PMKID: attack_pmkid_start(config); break;
        case ATTACK_TYPE_HANDSHAKE: attack_handshake_start(config); break;
        case ATTACK_TYPE_DOS:
            attack_dos_start(config);
            break;
        case ATTACK_TYPE_BEACON:
            attack_beacon_start(config);
            break;
    }
}

