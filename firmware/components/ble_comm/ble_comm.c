#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include "esp_log.h"
#include "esp_bt.h"
#include "esp_gap_ble_api.h"
#include "esp_gatts_api.h"
#include "esp_bt_main.h"
#include "esp_gatt_common_api.h"
#include "ble_comm.h"
#include "app_logic.h"

#define TAG "BLE_COMM"

#define GATTS_SERVICE_UUID      0x4FAF
#define GATTS_CHAR_COMMAND_UUID  0xBEB5
#define GATTS_CHAR_RESPONSE_UUID 0xBEB6

#define PROFILE_NUM 1
#define PROFILE_APP_ID 0

struct gatts_profile_inst {
    esp_gatts_cb_t gatts_cb;
    uint16_t gatts_if;
    uint16_t app_id;
    uint16_t conn_id;
    uint16_t service_handle;
    esp_gatt_srvc_id_t service_id;
    uint16_t char_command_handle;
    esp_bt_uuid_t char_command_uuid;
    uint16_t char_response_handle;
    esp_bt_uuid_t char_response_uuid;
    esp_gatt_perm_t perm;
    esp_gatt_char_prop_t property;
    uint16_t descr_handle;
    esp_bt_uuid_t descr_uuid;
};

static void gatts_profile_event_handler(esp_gatts_cb_event_t event, esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param);
static void gap_event_handler(esp_gap_ble_cb_event_t event, esp_ble_gap_cb_param_t *param);

static struct gatts_profile_inst gl_profile_tab[PROFILE_NUM] = {
    [PROFILE_APP_ID] = {
        .gatts_cb = gatts_profile_event_handler,
        .gatts_if = ESP_GATT_IF_NONE,
    },
};

static uint8_t char_str[] = {0x11, 0x22, 0x33};

static esp_attr_value_t gatts_char_val = {
    .attr_max_len = 4096,
    .attr_len     = sizeof(char_str),
    .attr_value   = char_str,
};

static uint8_t adv_config_done = 0;
#define adv_config_flag      (1 << 0)
#define scan_rsp_config_flag (1 << 1)

static uint8_t adv_service_uuid128[16] = {
    /* LSB <--------------------------------------------------------------------------------> MSB */
    //first uuid, 16bit, [12],[13] is the value
    0xfb, 0x34, 0x9b, 0x5f, 0x80, 0x00, 0x00, 0x80, 0x00, 0x10, 0x00, 0x00, 0xAF, 0x4F, 0x00, 0x00,
};

static esp_ble_adv_params_t adv_params = {
    .adv_int_min        = 0x40, // 64 * 0.625ms = 40ms
    .adv_int_max        = 0x80, // 128 * 0.625ms = 80ms
    .adv_type           = ADV_TYPE_IND,
    .own_addr_type      = BLE_ADDR_TYPE_PUBLIC,
    .channel_map        = ADV_CHNL_ALL,
    .adv_filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
};

/**
 * @brief GAP (Generic Access Profile) event handler.
 * 
 * Manages advertising state and connection parameter updates.
 */
static void gap_event_handler(esp_gap_ble_cb_event_t event, esp_ble_gap_cb_param_t *param) {
    switch (event) {
    case ESP_GAP_BLE_ADV_DATA_SET_COMPLETE_EVT:
        adv_config_done &= (~adv_config_flag);
        if (adv_config_done == 0) {
            esp_ble_gap_start_advertising(&adv_params);
        }
        break;
    case ESP_GAP_BLE_SCAN_RSP_DATA_SET_COMPLETE_EVT:
        adv_config_done &= (~scan_rsp_config_flag);
        if (adv_config_done == 0) {
            esp_ble_gap_start_advertising(&adv_params);
        }
        break;
    case ESP_GAP_BLE_ADV_START_COMPLETE_EVT:
        if (param->adv_start_cmpl.status != ESP_BT_STATUS_SUCCESS) {
            ESP_LOGE(TAG, "Advertising start failed");
        }
        break;
    case ESP_GAP_BLE_ADV_STOP_COMPLETE_EVT:
        if (param->adv_stop_cmpl.status != ESP_BT_STATUS_SUCCESS) {
            ESP_LOGE(TAG, "Advertising stop failed");
        }
        break;
    case ESP_GAP_BLE_UPDATE_CONN_PARAMS_EVT:
         ESP_LOGI(TAG, "update connection params status = %d, min_int = %d, max_int = %d,conn_int = %d,latency = %d, timeout = %d",
                  param->update_conn_params.status,
                  param->update_conn_params.min_int,
                  param->update_conn_params.max_int,
                  param->update_conn_params.conn_int,
                  param->update_conn_params.latency,
                  param->update_conn_params.timeout);
        break;
    default:
        break;
    }
}

// Handlers are declared in app_logic.h

static uint8_t *prepare_write_buf = NULL;
static int prepare_write_len = 0;

/**
 * @brief GATTS (GATT Server) profile event handler.
 * 
 * Manages service creation, characteristic addition, read/write requests,
 * and connection/disconnection events.
 */
static void gatts_profile_event_handler(esp_gatts_cb_event_t event, esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param) {
    switch (event) {
    case ESP_GATTS_REG_EVT:
        ESP_LOGI(TAG, "REGISTER_APP_EVT, status %d, app_id %d", param->reg.status, param->reg.app_id);
        gl_profile_tab[PROFILE_APP_ID].service_id.is_primary = true;
        gl_profile_tab[PROFILE_APP_ID].service_id.id.inst_id = 0x00;
        gl_profile_tab[PROFILE_APP_ID].service_id.id.uuid.len = ESP_UUID_LEN_16;
        gl_profile_tab[PROFILE_APP_ID].service_id.id.uuid.uuid.uuid16 = GATTS_SERVICE_UUID;

        esp_ble_gatts_create_service(gatts_if, &gl_profile_tab[PROFILE_APP_ID].service_id, 10);
        break;
    case ESP_GATTS_READ_EVT: {
        ESP_LOGI(TAG, "GATT_READ_EVT, conn_id %" PRIu32 ", trans_id %" PRIu32 ", handle %" PRIu32, (uint32_t)param->read.conn_id, (uint32_t)param->read.trans_id, (uint32_t)param->read.handle);
        esp_gatt_rsp_t rsp;
        memset(&rsp, 0, sizeof(esp_gatt_rsp_t));
        rsp.attr_value.handle = param->read.handle;
        rsp.attr_value.len = 4;
        rsp.attr_value.value[0] = 0xde;
        rsp.attr_value.value[1] = 0xed;
        rsp.attr_value.value[2] = 0xbe;
        rsp.attr_value.value[3] = 0xef;
        esp_ble_gatts_send_response(gatts_if, param->read.conn_id, param->read.trans_id, ESP_GATT_OK, &rsp);
        break;
    }
    case ESP_GATTS_WRITE_EVT: {
        if (!param->write.is_prep) {
            // Check if it's a binary command (0x01: PX, 0x02: FULL_FRAME, 0x03: FULL_ANIM, 0x04: FRAGMENTED)
            if (param->write.len > 0 && (param->write.value[0] >= 0x01 && param->write.value[0] <= 0x04)) {
                app_handle_binary_command(param->write.value, param->write.len);
            } else {
                char *data = malloc(param->write.len + 1);
                if (data) {
                    memcpy(data, param->write.value, param->write.len);
                    data[param->write.len] = '\0';
                    ESP_LOGI(TAG, "Received text command: %s", data);
                    app_handle_command(data);
                    free(data);
                }
            }
        } else {
            // Handle prepare write for large packets (exceeding MTU)
            if (prepare_write_buf == NULL) {
                prepare_write_buf = malloc(1024); // Max expected command size
                prepare_write_len = 0;
            }
            if (prepare_write_buf && (prepare_write_len + param->write.len <= 1024)) {
                memcpy(prepare_write_buf + prepare_write_len, param->write.value, param->write.len);
                prepare_write_len += param->write.len;
            }
        }
        if (param->write.need_rsp) {
            esp_gatt_rsp_t rsp;
            memset(&rsp, 0, sizeof(esp_gatt_rsp_t));
            rsp.attr_value.handle = param->write.handle;
            rsp.attr_value.len = param->write.len;
            rsp.attr_value.offset = param->write.offset;
            memcpy(rsp.attr_value.value, param->write.value, param->write.len);
            esp_ble_gatts_send_response(gatts_if, param->write.conn_id, param->write.trans_id, ESP_GATT_OK, &rsp);
        }
        break;
    }
    case ESP_GATTS_EXEC_WRITE_EVT:
        if (param->exec_write.exec_write_flag == ESP_GATT_PREP_WRITE_EXEC) {
            if (prepare_write_buf && prepare_write_len > 0) {
                // Support all binary command types in prepared writes
                if (prepare_write_buf[0] >= 0x01 && prepare_write_buf[0] <= 0x04) {
                    app_handle_binary_command(prepare_write_buf, prepare_write_len);
                } else {
                    char *data = malloc(prepare_write_len + 1);
                    if (data) {
                        memcpy(data, prepare_write_buf, prepare_write_len);
                        data[prepare_write_len] = '\0';
                        app_handle_command(data);
                        free(data);
                    }
                }
            }
        }
        if (prepare_write_buf) {
            free(prepare_write_buf);
            prepare_write_buf = NULL;
            prepare_write_len = 0;
        }
        break;
    case ESP_GATTS_MTU_EVT:
        ESP_LOGI(TAG, "ESP_GATTS_MTU_EVT, MTU %d", param->mtu.mtu);
        break;
    case ESP_GATTS_CONF_EVT:
        break;
    case ESP_GATTS_UNREG_EVT:
        break;
    case ESP_GATTS_CREATE_EVT:
        ESP_LOGI(TAG, "CREATE_SERVICE_EVT, status %d,  service_handle %d", param->create.status, param->create.service_handle);
        gl_profile_tab[PROFILE_APP_ID].service_handle = param->create.service_handle;
        
        // Command Characteristic
        gl_profile_tab[PROFILE_APP_ID].char_command_uuid.len = ESP_UUID_LEN_16;
        gl_profile_tab[PROFILE_APP_ID].char_command_uuid.uuid.uuid16 = GATTS_CHAR_COMMAND_UUID;
        
        esp_ble_gatts_start_service(gl_profile_tab[PROFILE_APP_ID].service_handle);
        
        esp_ble_gatts_add_char(gl_profile_tab[PROFILE_APP_ID].service_handle, &gl_profile_tab[PROFILE_APP_ID].char_command_uuid,
                                ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE,
                                ESP_GATT_CHAR_PROP_BIT_READ | ESP_GATT_CHAR_PROP_BIT_WRITE | ESP_GATT_CHAR_PROP_BIT_WRITE_NR,
                                &gatts_char_val, NULL);

        // Response Characteristic
        gl_profile_tab[PROFILE_APP_ID].char_response_uuid.len = ESP_UUID_LEN_16;
        gl_profile_tab[PROFILE_APP_ID].char_response_uuid.uuid.uuid16 = GATTS_CHAR_RESPONSE_UUID;
        
        esp_ble_gatts_add_char(gl_profile_tab[PROFILE_APP_ID].service_handle, &gl_profile_tab[PROFILE_APP_ID].char_response_uuid,
                                ESP_GATT_PERM_READ,
                                ESP_GATT_CHAR_PROP_BIT_READ | ESP_GATT_CHAR_PROP_BIT_NOTIFY,
                                &gatts_char_val, NULL);
        break;
    case ESP_GATTS_ADD_CHAR_EVT:
        ESP_LOGI(TAG, "ADD_CHAR_EVT, status %d,  attr_handle %d, service_handle %d",
                 param->add_char.status, param->add_char.attr_handle, param->add_char.service_handle);
        if (param->add_char.char_uuid.uuid.uuid16 == GATTS_CHAR_COMMAND_UUID) {
            gl_profile_tab[PROFILE_APP_ID].char_command_handle = param->add_char.attr_handle;
        } else if (param->add_char.char_uuid.uuid.uuid16 == GATTS_CHAR_RESPONSE_UUID) {
            gl_profile_tab[PROFILE_APP_ID].char_response_handle = param->add_char.attr_handle;
            // Add descriptor for Notify
            esp_bt_uuid_t desc_uuid = {.len = ESP_UUID_LEN_16, .uuid.uuid16 = ESP_GATT_UUID_CHAR_CLIENT_CONFIG};
            esp_ble_gatts_add_char_descr(gl_profile_tab[PROFILE_APP_ID].service_handle, &desc_uuid,
                                         ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE, NULL, NULL);
        }
        break;
    case ESP_GATTS_CONNECT_EVT:
        ESP_LOGI(TAG, "ESP_GATTS_CONNECT_EVT, conn_id %" PRIu32, (uint32_t)param->connect.conn_id);
        gl_profile_tab[PROFILE_APP_ID].conn_id = param->connect.conn_id;
        
        // Update connection parameters for better stability
        esp_ble_conn_update_params_t conn_params = {0};
        memcpy(conn_params.bda, param->connect.remote_bda, sizeof(esp_bd_addr_t));
        conn_params.min_int = 0x10;    // 20ms
        conn_params.max_int = 0x20;    // 40ms
        conn_params.latency = 0;
        conn_params.timeout = 400;     // 4s
        esp_ble_gap_update_conn_params(&conn_params);
        break;
    case ESP_GATTS_DISCONNECT_EVT:
        ESP_LOGI(TAG, "ESP_GATTS_DISCONNECT_EVT, disconnect_reason 0x%x", param->disconnect.reason);
        vTaskDelay(pdMS_TO_TICKS(500)); // Small delay before restarting advertising
        esp_ble_gap_start_advertising(&adv_params);
        gl_profile_tab[PROFILE_APP_ID].conn_id = 0xFFFF;
        break;
    default:
        break;
    }
}

/**
 * @brief Global GATTS event handler.
 * 
 * Dispatches events to the appropriate profile handler.
 */
static void gatts_event_handler(esp_gatts_cb_event_t event, esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param) {
    if (event == ESP_GATTS_REG_EVT) {
        if (param->reg.status == ESP_GATT_OK) {
            gl_profile_tab[param->reg.app_id].gatts_if = gatts_if;
        } else {
            ESP_LOGI(TAG, "Reg app failed, app_id %04x, status %d", param->reg.app_id, param->reg.status);
            return;
        }
    }

    for (int idx = 0; idx < PROFILE_NUM; idx++) {
        if (gatts_if == ESP_GATT_IF_NONE || gatts_if == gl_profile_tab[idx].gatts_if) {
            if (gl_profile_tab[idx].gatts_cb) {
                gl_profile_tab[idx].gatts_cb(event, gatts_if, param);
            }
        }
    }
}

static QueueHandle_t ble_resp_queue = NULL;

typedef struct {
    uint8_t *data;
    size_t len;
} ble_resp_msg_t;

/**
 * @brief Background task for sending BLE responses via notifications.
 * 
 * Consumes messages from the response queue and sends them to the connected client.
 */
static void ble_resp_task(void *pvParameters) {
    ble_resp_msg_t msg;
    ESP_LOGI(TAG, "BLE Response task started");
    while (1) {
        if (xQueueReceive(ble_resp_queue, &msg, portMAX_DELAY) == pdTRUE) {
            if (gl_profile_tab[PROFILE_APP_ID].conn_id != 0xFFFF) {
                esp_ble_gatts_send_indicate(gl_profile_tab[PROFILE_APP_ID].gatts_if, 
                                           gl_profile_tab[PROFILE_APP_ID].conn_id, 
                                           gl_profile_tab[PROFILE_APP_ID].char_response_handle, 
                                           (uint16_t)msg.len, msg.data, false);
            }
            free(msg.data);
            // Yield to allow btController to process the outgoing packet
            vTaskDelay(pdMS_TO_TICKS(10));
        }
    }
}

/**
 * @brief Initializes the BLE communication component.
 * 
 * Sets up the Bluetooth controller, Bluedroid stack, GATTS/GAP handlers,
 * and starts the response task.
 * 
 * @param device_name The BLE device name to advertise.
 * @return true if initialization was successful, false otherwise.
 */
bool ble_comm_init(const char *device_name) {
    esp_err_t ret;

    ble_resp_queue = xQueueCreate(10, sizeof(ble_resp_msg_t));
    if (ble_resp_queue == NULL) {
        ESP_LOGE(TAG, "Failed to create response queue");
        return false;
    }
    xTaskCreate(ble_resp_task, "ble_resp_task", 4096, NULL, 5, NULL);

    gl_profile_tab[PROFILE_APP_ID].conn_id = 0xFFFF; // Initialize as disconnected

    ESP_ERROR_CHECK(esp_bt_controller_mem_release(ESP_BT_MODE_CLASSIC_BT));

    esp_bt_controller_config_t bt_cfg = BT_CONTROLLER_INIT_CONFIG_DEFAULT();
    ret = esp_bt_controller_init(&bt_cfg);
    if (ret) {
        ESP_LOGE(TAG, "%s initialize controller failed: %s", __func__, esp_err_to_name(ret));
        return false;
    }

    ret = esp_bt_controller_enable(ESP_BT_MODE_BLE);
    if (ret) {
        ESP_LOGE(TAG, "%s enable controller failed: %s", __func__, esp_err_to_name(ret));
        return false;
    }

    ret = esp_bluedroid_init();
    if (ret) {
        ESP_LOGE(TAG, "%s init bluetooth failed: %s", __func__, esp_err_to_name(ret));
        return false;
    }

    ret = esp_bluedroid_enable();
    if (ret) {
        ESP_LOGE(TAG, "%s enable bluetooth failed: %s", __func__, esp_err_to_name(ret));
        return false;
    }

    ret = esp_ble_gatts_register_callback(gatts_event_handler);
    if (ret) {
        ESP_LOGE(TAG, "gatts register error, error code = %x", ret);
        return false;
    }

    ret = esp_ble_gap_register_callback(gap_event_handler);
    if (ret) {
        ESP_LOGE(TAG, "gap register error, error code = %x", ret);
        return false;
    }

    ret = esp_ble_gatts_app_register(PROFILE_APP_ID);
    if (ret) {
        ESP_LOGE(TAG, "gatts app register error, error code = %x", ret);
        return false;
    }

    esp_ble_gap_set_device_name(device_name);

    esp_ble_adv_data_t adv_data = {
        .set_scan_rsp = false,
        .include_name = true,
        .include_txpower = true,
        .min_interval = 0x0006,
        .max_interval = 0x0010,
        .appearance = 0x00,
        .manufacturer_len = 0,
        .p_manufacturer_data = NULL,
        .service_data_len = 0,
        .p_service_data = NULL,
        .service_uuid_len = 16,
        .p_service_uuid = adv_service_uuid128,
        .flag = (ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_BREDR_NOT_SPT),
    };
    esp_ble_gap_config_adv_data(&adv_data);

    return true;
}

/**
 * @brief Sends a string response back to the connected BLE client.
 * 
 * @param data The string to send.
 * @return true if the message was queued successfully.
 */
bool ble_comm_send_response(const char *data) {
    if (gl_profile_tab[PROFILE_APP_ID].conn_id == 0xFFFF || ble_resp_queue == NULL) return false;
    
    size_t len = strlen(data);
    return ble_comm_send_binary_response((const uint8_t *)data, len);
}

/**
 * @brief Sends a binary response back to the connected BLE client.
 * 
 * @param data Pointer to the binary data.
 * @param len Length of the data in bytes.
 * @return true if the message was queued successfully.
 */
bool ble_comm_send_binary_response(const uint8_t *data, size_t len) {
    if (gl_profile_tab[PROFILE_APP_ID].conn_id == 0xFFFF || ble_resp_queue == NULL || data == NULL || len == 0) return false;
    
    ble_resp_msg_t msg;
    msg.data = (uint8_t *)malloc(len);
    if (msg.data == NULL) return false;
    
    memcpy(msg.data, data, len);
    msg.len = len;
    
    if (xQueueSend(ble_resp_queue, &msg, 0) != pdTRUE) {
        free(msg.data);
        return false;
    }
    return true;
}

/**
 * @brief Checks if a BLE client is currently connected.
 * @return true if connected, false otherwise.
 */
bool ble_comm_is_connected(void) {
    return gl_profile_tab[PROFILE_APP_ID].conn_id != 0xFFFF;
}
