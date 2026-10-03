/**
 * @file attack.h
 * @author risinek (risinek@gmail.com)
 * @date 2021-04-02
 * @copyright Copyright (c) 2021
 *
 * @brief Provides interface to attack wrapper
 * 
 * This file provide interface to control attack wrapper like setting current attack state, 
 * update attack status content, etc...
 */

#ifndef ATTACK_H
#define ATTACK_H

#include "esp_system.h"
#include "esp_event.h"
#include "esp_wifi.h"
#include "esp_wifi_types.h"

/**
 * @brief Implemented attack types that can be chosen.
 * 
 */
typedef enum {
    ATTACK_TYPE_PASSIVE,
    ATTACK_TYPE_HANDSHAKE,
    ATTACK_TYPE_PMKID,
    ATTACK_TYPE_DOS,
    ATTACK_TYPE_BEACON
} attack_type_t;

/**
 * @brief States of single attack run. 
 * 
 * @note TIMEOUT will be removed in #64
 */
typedef enum {
    READY,      ///< no attack is in progress and results from previous attack run are available.
    RUNNING,    ///< attack is in progress, attack_status_t.content may not be consistent.
    FINISHED,   ///< last attack finsihed and results are available.
    TIMEOUT     ///< last attack timed out. This option will be moved as sub category of FINISHED state.
} attack_state_t;

/**
 * @brief Attack config parsed from webserver request
 * 
 * @deprecated will be removed in #45
 */
typedef struct __attribute__((packed)) {
    uint8_t type;
    uint8_t method;
    uint8_t timeout;
    const wifi_ap_record_t *ap_record;
    const wifi_ap_record_t **ap_records;
    uint8_t ap_count;
} attack_config_t;

/**
 * @brief Contains current attack status.
 * 
 * This structure contains all information and data about latest attack.
 */
typedef struct {
    uint8_t state;  ///< attack_state_t
    uint8_t type;   ///< attack_type_t
    uint16_t content_size;
    char *content;
} attack_status_t;

/**
 * @brief Returns pointer to attack_status_t structure.
 * 
 * @return const attack_status_t*  pointer to the status strucutre
 */
const attack_status_t *attack_get_status();

/**
 * @brief Function to update current status of attack.
 * 
 * If FINISHED state is passed, then the attack timeout timer is stopped.
 * @param state new attack state of type attack_state_t to be set
 */
void attack_update_status(attack_state_t state);

/**
 * @brief Initialises attack wrapper. This function should be callend only once.
 * 
 * This function creates all necessary resources for attack wrapper. It has to be called before any attack can be run.
 */
void attack_init();

/**
 * @brief Resets attack status to READY and frees resources.
 */
void attack_reset();

/**
 * @brief Allocates status content of given size.
 *  
 * @param size size to be allocated
 * @return char* pointer to newly allocated status content
 */
char *attack_alloc_result_content(unsigned size);

/**
 * @brief Reallocates current status content and appends new data.
 * 
 * @param buffer new data to be appended to status content
 * @param size size of the new data to be appended
 */
void attack_append_status_content(uint8_t *buffer, unsigned size);

// Added for manual control
void attack_start(attack_config_t *config);
void attack_stop();

#endif