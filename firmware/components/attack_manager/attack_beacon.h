#ifndef ATTACK_BEACON_H
#define ATTACK_BEACON_H

#include "attack.h"

/**
 * @brief Starts beacon swarm attack
 * 
 * @param config 
 */
void attack_beacon_start(attack_config_t *config);
void attack_beacon_stop();

/**
 * @brief Load swarm SSIDs from NVS or use defaults
 */
void attack_beacon_load_ssids();

/**
 * @brief Update swarm SSIDs and save to NVS
 * 
 * @param comma_separated_ssids List of SSIDs separated by commas
 */
void attack_beacon_set_ssids(const char *comma_separated_ssids);

/**
 * @brief Get all swarm SSIDs as a comma-separated string
 * 
 * @param buffer Buffer to store the string
 * @param max_len Maximum length of the buffer
 */
void attack_beacon_get_ssids(char *buffer, size_t max_len);

#endif
