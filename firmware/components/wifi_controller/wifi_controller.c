#include "wifi_controller.h"

#include <stdio.h>
#include <string.h>

#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include "esp_log.h"
#include "esp_err.h"
#include "esp_wifi.h"
#include "esp_wifi_types.h"
#include "esp_netif.h"
#include "esp_event.h"

static const char* TAG = "wifi_controller";
/**
 * @brief Stores current state of Wi-Fi interface
 */
static bool wifi_init = false;
static uint8_t original_mac_ap[6];
static uint8_t original_mac_sta[6];
static esp_netif_t *ap_netif = NULL;
static esp_netif_t *sta_netif = NULL;

/**
 * @brief Main Wi-Fi event handler. Currently unused but registered for potential future use.
 */
static void wifi_event_handler(void *event_handler_arg, esp_event_base_t event_base, int32_t event_id, void *event_data){

}

/**
 * @brief Internal helper to initialize Wi-Fi in Station (STA) mode.
 * 
 * Sets up the network stack, default STA interface, and initializes the Wi-Fi driver.
 * Stores the original MAC address and starts the Wi-Fi hardware.
 * 
 * @attention This function should be called only once.
 */
static void wifi_init_sta(){
    ESP_ERROR_CHECK(esp_netif_init());

    // Configure STA interface by default
    sta_netif = esp_netif_create_default_wifi_sta();
    // We don't create ap_netif here anymore to avoid automatic broadcast
    // It will be created in wifictl_ap_start if needed

    wifi_init_config_t wifi_init_config = WIFI_INIT_CONFIG_DEFAULT();

    ESP_ERROR_CHECK(esp_wifi_init(&wifi_init_config));
    ESP_ERROR_CHECK(esp_wifi_set_storage(WIFI_STORAGE_RAM));
    
    // Set to STA mode by default
    ESP_LOGI(TAG, "Setting Wi-Fi mode to STA (no broadcast)");
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));

    ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL));

    // save original STA MAC address
    ESP_ERROR_CHECK(esp_wifi_get_mac(WIFI_IF_STA, original_mac_sta));
    ESP_LOGI(TAG, "Original STA MAC: %02x:%02x:%02x:%02x:%02x:%02x", 
             original_mac_sta[0], original_mac_sta[1], original_mac_sta[2],
             original_mac_sta[3], original_mac_sta[4], original_mac_sta[5]);
    
    // save original AP MAC address
    ESP_ERROR_CHECK(esp_wifi_get_mac(WIFI_IF_AP, original_mac_ap));
    ESP_LOGI(TAG, "Original AP MAC: %02x:%02x:%02x:%02x:%02x:%02x", 
             original_mac_ap[0], original_mac_ap[1], original_mac_ap[2],
             original_mac_ap[3], original_mac_ap[4], original_mac_ap[5]);
    ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));

    ESP_ERROR_CHECK(esp_wifi_start());
    wifi_init = true;
}

/**
 * @brief Public API to initialize the Wi-Fi controller.
 */
void wifictl_init() {
    if(!wifi_init){
        wifi_init_sta();
    }
}

/**
 * @brief Starts the Access Point (AP) interface with the given configuration.
 * 
 * Ensures the Wi-Fi is initialized, creates the AP netif if it doesn't exist,
 * and sets the mode to APSTA (Dual mode) to allow simultaneous STA and AP operations.
 * 
 * @param wifi_config Pointer to the Wi-Fi configuration structure.
 */
void wifictl_ap_start(wifi_config_t *wifi_config) {
    ESP_LOGI(TAG, "Starting AP with SSID: %s", wifi_config->ap.ssid);
    if(!wifi_init){
        wifi_init_sta();
    }
    
    if (ap_netif == NULL) {
        ap_netif = esp_netif_create_default_wifi_ap();
    }
    
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_APSTA));
    ESP_ERROR_CHECK(esp_wifi_set_config(ESP_IF_WIFI_AP, wifi_config));
    ESP_LOGI(TAG, "AP started and broadcasting.");
}

/**
 * @brief Stops the AP interface and reverts to STA-only mode.
 */
void wifictl_ap_stop(){
    ESP_LOGI(TAG, "Stopping AP broadcast...");
    // Switch back to STA mode to stop AP broadcast
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_LOGI(TAG, "AP stopped, mode set to STA only.");
}

/**
 * @brief Starts a management AP using compile-time configurations.
 */
void wifictl_mgmt_ap_start(){
    wifi_config_t mgmt_wifi_config = {
        .ap = {
            .ssid = CONFIG_MGMT_AP_SSID,
            .ssid_len = strlen(CONFIG_MGMT_AP_SSID),
            .password = CONFIG_MGMT_AP_PASSWORD,
            .max_connection = CONFIG_MGMT_AP_MAX_CONNECTIONS,
            .authmode = WIFI_AUTH_WPA2_PSK
        },
    };
    wifictl_ap_start(&mgmt_wifi_config);
}

/**
 * @brief Connects the STA interface to a target Access Point.
 * 
 * @param ap_record Pointer to the AP record obtained from a scan.
 * @param password Password for the AP (optional).
 */
void wifictl_sta_connect_to_ap(const wifi_ap_record_t *ap_record, const char password[]){
    ESP_LOGD(TAG, "Connecting STA to AP...");
    if(!wifi_init){
        wifi_init_sta();
    }

    wifi_config_t sta_wifi_config = {
        .sta = {
            .channel = ap_record->primary,
            .scan_method = WIFI_FAST_SCAN,
            .pmf_cfg.capable = false,
            .pmf_cfg.required = false
        },
    };
    mempcpy(sta_wifi_config.sta.ssid, ap_record->ssid, 32);

    if(password != NULL){
        if(strlen(password) >= 64) {
            ESP_LOGE(TAG, "Password is too long. Max supported length is 64");
            return;
        }
        memcpy(sta_wifi_config.sta.password, password, strlen(password) + 1);
    }

    ESP_LOGD(TAG, ".ssid=%s", sta_wifi_config.sta.ssid);

    ESP_ERROR_CHECK(esp_wifi_set_config(ESP_IF_WIFI_STA, &sta_wifi_config));
    ESP_ERROR_CHECK(esp_wifi_connect());

}

/**
 * @brief Disconnects the STA interface from the current AP.
 */
void wifictl_sta_disconnect(){
    ESP_ERROR_CHECK(esp_wifi_disconnect());
}

/**
 * @brief Sets a custom MAC address for the AP interface.
 * 
 * Useful for MAC spoofing or Rogue AP attacks.
 * 
 * @param mac_ap The 6-byte MAC address to set.
 */
void wifictl_set_ap_mac(const uint8_t *mac_ap){
    ESP_LOGD(TAG, "Changing AP MAC address...");
    
    // Ensure the AP interface is initialized before setting MAC
    if (ap_netif == NULL) {
        ESP_LOGI(TAG, "Initializing AP interface for MAC change...");
        ap_netif = esp_netif_create_default_wifi_ap();
    }
    
    // Interface must be active or at least created for MAC setting to work reliably
    esp_err_t err = esp_wifi_set_mac(WIFI_IF_AP, mac_ap);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "Failed to set AP MAC (err: %s).", esp_err_to_name(err));
    }
}

/**
 * @brief Gets the current MAC address of the AP interface.
 * @param mac_ap Buffer to store the 6-byte MAC address.
 */
void wifictl_get_ap_mac(uint8_t *mac_ap){
    esp_wifi_get_mac(WIFI_IF_AP, mac_ap);
}

/**
 * @brief Restores the original factory MAC address for the AP interface.
 */
void wifictl_restore_ap_mac(){
    ESP_LOGD(TAG, "Restoring original AP MAC address...");
    
    // Check if original MAC is valid (not empty)
    uint8_t zero_mac[6] = {0};
    if (memcmp(original_mac_ap, zero_mac, 6) == 0) {
        ESP_LOGW(TAG, "Original AP MAC is empty! Skipping restoration.");
        return;
    }

    wifi_mode_t current_mode;
    if (esp_wifi_get_mode(&current_mode) == ESP_OK) {
        if (current_mode == WIFI_MODE_AP || current_mode == WIFI_MODE_APSTA) {
            ESP_LOGI(TAG, "Restoring AP MAC to %02x:%02x:%02x:%02x:%02x:%02x", 
                     original_mac_ap[0], original_mac_ap[1], original_mac_ap[2],
                     original_mac_ap[3], original_mac_ap[4], original_mac_ap[5]);
                     
            esp_err_t err = esp_wifi_set_mac(WIFI_IF_AP, original_mac_ap);
            if (err != ESP_OK) {
                ESP_LOGW(TAG, "Failed to restore AP MAC: %s", esp_err_to_name(err));
            }
        } else {
            ESP_LOGD(TAG, "Not in AP mode, skipping MAC restoration for WIFI_IF_AP");
        }
    }
}

/**
 * @brief Gets the current MAC address of the STA interface.
 * @param mac_sta Buffer to store the 6-byte MAC address.
 */
void wifictl_get_sta_mac(uint8_t *mac_sta){
    esp_wifi_get_mac(WIFI_IF_STA, mac_sta);
}

/**
 * @brief Sets a custom MAC address for the STA interface.
 * 
 * @param mac_sta The 6-byte MAC address to set.
 */
void wifictl_set_sta_mac(const uint8_t *mac_sta){
    ESP_LOGD(TAG, "Changing STA MAC address...");
    esp_err_t err = esp_wifi_set_mac(WIFI_IF_STA, mac_sta);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "Failed to set STA MAC (err: %s).", esp_err_to_name(err));
    }
}

/**
 * @brief Restores the original factory MAC address for the STA interface.
 */
void wifictl_restore_sta_mac(){
    ESP_LOGD(TAG, "Restoring original STA MAC address...");
    
    // Check if original MAC is valid
    uint8_t zero_mac[6] = {0};
    if (memcmp(original_mac_sta, zero_mac, 6) == 0) {
        ESP_LOGW(TAG, "Original STA MAC is empty! Skipping restoration.");
        return;
    }

    esp_err_t err = esp_wifi_set_mac(WIFI_IF_STA, original_mac_sta);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "Failed to restore STA MAC: %s", esp_err_to_name(err));
    }
}

/**
 * @brief Sets the Wi-Fi radio to a specific channel.
 * 
 * @param channel The channel number (1-13).
 */
void wifictl_set_channel(uint8_t channel){
    if((channel == 0) || (channel >  13)){
        ESP_LOGE(TAG,"Channel out of range. Expected value from <1,13> but got %u", channel);
        return;
    }
    esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
}