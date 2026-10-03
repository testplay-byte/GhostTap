/**
 * @file app_logic.c
 * @brief Core application logic for ESP32-C3 project.
 * 
 * This component manages:
 * - BLE command parsing and reassembly
 * - OLED animation modes and transitions
 * - A custom script engine for OLED effects
 * - Multi-target attack coordination
 * - System time and temperature monitoring
 */

#define LOG_LOCAL_LEVEL ESP_LOG_INFO
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <inttypes.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "app_logic.h"
#include "oled_display.h"
#include "esp_log.h"
#include "ble_comm.h"
#include "esp_timer.h"
#include <math.h>
#include "snake_1x1.h"
#include "snake_2x2.h"
#include "wifi_controller.h"
#include "attack.h"
#include "attack_handshake.h"
#include "esp_wifi.h"
#include "attack_beacon.h"
#include "pcap_serializer.h"
#include "driver/temperature_sensor.h"

#define TAG "APP_LOGIC"
#define CMD_QUEUE_SIZE 20
#define MAX_CMD_LEN 4096

/** @brief Current target SSID for display purposes */
static char current_target_ssid[33] = "TARGET";

/** @brief Command message types for the command queue */
typedef enum {
    CMD_TYPE_TEXT,   ///< Plain text command
    CMD_TYPE_BINARY  ///< Binary data (e.g., script, config)
} cmd_msg_type_t;

/** @brief Command message structure */
typedef struct {
    cmd_msg_type_t type;
    uint8_t data[MAX_CMD_LEN];
    size_t len;
} cmd_msg_t;

/** @brief Queue for processing commands received via BLE */
static QueueHandle_t cmd_queue = NULL;

/** @brief Application display modes */
typedef enum {
    MODE_STATIC,        ///< Static text or logo
    MODE_ANIM_BOOT,      ///< Boot sequence animation
    MODE_ANIM_STARS,     ///< Starfield effect
    MODE_ANIM_WAVES,     ///< Sine wave animation
    MODE_ANIM_VIZ,       ///< Visualizer mode
    MODE_CLOCK,          ///< Digital/Analog clock
    MODE_ANIM_DNA,       ///< DNA double helix animation
    MODE_ANIM_FIREWORKS, ///< Fireworks effect
    MODE_ANIM_TUNNEL,    ///< 3D Tunnel effect
    MODE_ANIM_SNOW,      ///< Falling snow effect
    MODE_ANIM_VORTEX,    ///< Spiral vortex effect
    MODE_ANIM_CUBE,      ///< 3D Rotating cube
    MODE_ANIM_RINGS,     ///< Concentric rings
    MODE_ANIM_GLITCH,    ///< Glitch art effect
    MODE_ANIM_RAIN,      ///< Matrix rain effect
    MODE_ANIM_PLASMA,    ///< Plasma fractal effect
    MODE_ANIM_CIRCLES,   ///< Growing/shrinking circles
    MODE_ANIM_NOISE,     ///< Perlin-like noise effect
    MODE_ANIM_SWARM,     ///< Particle swarm animation
    MODE_ANIM_WAVE2,     ///< Advanced wave patterns
    MODE_ANIM_METABALLS, ///< Metaballs effect
    MODE_ANIM_TEXT_ONCE, ///< Scrolling text once
    MODE_ANIM_SCRIPT,    ///< Script-controlled animation
    MODE_SNAKE_1X1,      ///< Classic Snake game (1x1 pixels)
    MODE_SNAKE_2X2,      ///< Large Snake game (2x2 pixels)
    MODE_SWARM_CONFIG,   ///< Swarm behavior configuration
    MODE_ADVANCED       ///< Advanced control mode
} app_mode_t;

/** @brief Configuration for text animations */
typedef struct {
    uint8_t mode;      // 0=Static, 1=Moving, 2=Typing
    uint8_t speed;     // 1-255
    uint16_t width;    // Total width of rendered text
    uint8_t height;    // Total height (usually 40)
    uint8_t data[MAX_CMD_LEN - 10]; // Data buffer
    uint16_t data_len;
} text_anim_config_t;

static text_anim_config_t text_anim_cfg;
static uint32_t text_anim_tick = 0;

static SnakeGameState snake_game;

/** @brief State for reassembling fragmented BLE messages */
typedef struct {
    uint8_t *buffer;           ///< Reassembly buffer
    size_t total_size;         ///< Expected total size
    size_t received_size;      ///< Bytes received so far
    uint8_t total_fragments;   ///< Total fragments expected
    uint8_t received_fragments; ///< Fragments received so far
    uint8_t data_type;         ///< Type of data being reassembled
    uint32_t last_activity_ms; ///< Last activity timestamp for timeout
    bool is_active;            ///< Is reassembly in progress?
} frag_reassembly_t;

static frag_reassembly_t frag_state = {0};

/**
 * @brief Resets the fragmentation reassembly state and frees the buffer.
 * 
 * Clears the reassembly state and releases any dynamically allocated memory 
 * used for reassembling fragmented BLE messages.
 */
static void reset_frag_state() {
    if (frag_state.buffer) {
        free(frag_state.buffer);
    }
    memset(&frag_state, 0, sizeof(frag_reassembly_t));
}

static int viz_mode = 0;

/** @brief Internal system time structure */
typedef struct {
    int hour;
    int minute;
    int second;
} app_time_t;

static app_time_t system_time = {12, 0, 0};
static uint64_t last_time_update_ms = 0;
static bool use_24h_format = true;

/**
 * @brief Updates the internal system time based on monotonic system time.
 * 
 * Calculates the elapsed time since the last update using `esp_timer_get_time()` 
 * and increments the internal `system_time` structure accordingly.
 */
static void update_system_time(void) {
    uint64_t now = esp_timer_get_time() / 1000;
    if (last_time_update_ms == 0) {
        last_time_update_ms = now;
        return;
    }
    
    uint64_t diff = now - last_time_update_ms;
    if (diff >= 1000) {
        int seconds_to_add = diff / 1000;
        system_time.second += seconds_to_add;
        last_time_update_ms += seconds_to_add * 1000;
        
        if (system_time.second >= 60) {
            system_time.minute += system_time.second / 60;
            system_time.second %= 60;
        }
        if (system_time.minute >= 60) {
            system_time.hour += system_time.minute / 60;
            system_time.minute %= 60;
        }
        if (system_time.hour >= 24) {
            system_time.hour %= 24;
        }
    }
}

static app_mode_t current_mode = MODE_STATIC;
static int clock_index = 0;
static TaskHandle_t anim_task_handle = NULL;

/** @brief Multi-attack modes */
typedef enum {
    MULTI_ATTACK_NONE,  ///< No multi-attack active
    MULTI_ATTACK_LIST,  ///< Attacking a list of specific indices
    MULTI_ATTACK_ALL    ///< Attacking all discovered networks
} multi_attack_mode_t;

static multi_attack_mode_t multi_attack_mode = MULTI_ATTACK_NONE;
static int multi_attack_indices[32];
static int multi_attack_count = 0;
static int multi_attack_current_idx = 0;
static attack_config_t multi_attack_config;
static char multi_attack_type[16];
static uint32_t multi_attack_start_time = 0;
static uint32_t multi_attack_step_start_time = 0;
static int multi_attack_total_duration = 0;

// --- Script Engine State ---
#define MAX_SCRIPT_LINES 100
#define MAX_LINE_LEN 64
static char script_lines[MAX_SCRIPT_LINES][MAX_LINE_LEN];
static int script_line_count = 0;

/** @brief Script variable structure */
typedef struct {
    char name[16];
    float value;
} script_var_t;

#define MAX_VARS 32
static script_var_t script_vars[MAX_VARS];
static int script_var_count = 0;

/**
 * @brief Retrieves a variable value from the script engine's variable table.
 * 
 * Searches the `script_vars` array for a variable with the matching name.
 * 
 * @param name The name of the variable to retrieve.
 * @return The float value of the variable, or 0.0f if not found.
 */
static float get_var(const char* name) {
    for(int i=0; i<script_var_count; i++) {
        if(strcmp(script_vars[i].name, name) == 0) return script_vars[i].value;
    }
    return 0.0f;
}

/**
 * @brief Sets or updates a variable value in the script engine's variable table.
 * 
 * If the variable exists, its value is updated. If it does not exist and space
 * is available, a new variable is created.
 * 
 * @param name The name of the variable to set.
 * @param val The new float value for the variable.
 */
static void set_var(const char* name, float val) {
    for(int i=0; i<script_var_count; i++) {
        if(strcmp(script_vars[i].name, name) == 0) {
            script_vars[i].value = val;
            return;
        }
    }
    if(script_var_count < MAX_VARS) {
        strncpy(script_vars[script_var_count].name, name, 15);
        script_vars[script_var_count].value = val;
        script_var_count++;
    }
}

// Global pointer for the expression parser
static const char* p_expr;

static float parse_expression();

/**
 * @brief Skips any whitespace characters at the current parser position.
 * 
 * Advances the global `p_expr` pointer past any space characters.
 */
static void skip_spaces() {
    while(*p_expr == ' ') p_expr++;
}

static float eval_expr(const char* expr);

/**
 * @brief Parses a factor in a script expression.
 * 
 * A factor can be a number, a variable, a parenthesized expression, 
 * or a built-in function call (sin, cos, abs, random, hour, minute, second).
 * 
 * @return The evaluated float value of the factor.
 */
static float parse_factor() {
    skip_spaces();
    float x = 0;
    if (*p_expr == '+') { p_expr++; return parse_factor(); }
    if (*p_expr == '-') { p_expr++; return -parse_factor(); }
    
    if (*p_expr == '(') {
        p_expr++;
        x = parse_expression();
        if (*p_expr == ')') p_expr++;
    } else if ((*p_expr >= '0' && *p_expr <= '9') || *p_expr == '.') {
        char* end;
        x = strtof(p_expr, &end);
        p_expr = end;
    } else if ((*p_expr >= 'a' && *p_expr <= 'z') || (*p_expr >= 'A' && *p_expr <= 'Z')) {
        const char* start = p_expr;
        while ((*p_expr >= 'a' && *p_expr <= 'z') || (*p_expr >= 'A' && *p_expr <= 'Z') || (*p_expr >= '0' && *p_expr <= '9') || *p_expr == '_') {
            p_expr++;
        }
        char name[32];
        int len = p_expr - start;
        if (len > 31) len = 31;
        strncpy(name, start, len);
        name[len] = 0;
        
        // Built-in functions
        if (strcmp(name, "sin") == 0) x = sinf(parse_factor());
        else if (strcmp(name, "cos") == 0) x = cosf(parse_factor());
        else if (strcmp(name, "abs") == 0) x = fabsf(parse_factor());
        else if (strcmp(name, "random") == 0) x = ((float)rand() / RAND_MAX) * parse_factor();
        else if (strcmp(name, "hour") == 0) { skip_spaces(); if(*p_expr == '(') { p_expr++; skip_spaces(); if(*p_expr == ')') p_expr++; } x = system_time.hour; }
        else if (strcmp(name, "minute") == 0) { skip_spaces(); if(*p_expr == '(') { p_expr++; skip_spaces(); if(*p_expr == ')') p_expr++; } x = system_time.minute; }
        else if (strcmp(name, "second") == 0) { skip_spaces(); if(*p_expr == '(') { p_expr++; skip_spaces(); if(*p_expr == ')') p_expr++; } x = system_time.second; }
        else x = get_var(name);
    }
    return x;
}

/**
 * @brief Parses a term in a script expression.
 * 
 * A term consists of one or more factors combined by multiplication (*) or division (/).
 * 
 * @return The evaluated float value of the term.
 */
static float parse_term() {
    float x = parse_factor();
    while (1) {
        skip_spaces();
        if (*p_expr == '*') { p_expr++; x *= parse_factor(); }
        else if (*p_expr == '/') { 
            p_expr++; 
            float d = parse_factor(); 
            if(d != 0) x /= d; 
        }
        else break;
    }
    return x;
}

/**
 * @brief Parses a full expression.
 * 
 * An expression consists of one or more terms combined by addition (+) or subtraction (-).
 * 
 * @return The evaluated float value of the expression.
 */
static float parse_expression() {
    float x = parse_term();
    while (1) {
        skip_spaces();
        if (*p_expr == '+') { p_expr++; x += parse_term(); }
        else if (*p_expr == '-') { p_expr++; x -= parse_term(); }
        else break;
    }
    return x;
}

/**
 * @brief Top-level entry point to evaluate a script expression string.
 * 
 * Sets the global parser pointer and begins recursive descent parsing.
 * 
 * @param expr The null-terminated expression string to evaluate.
 * @return The resulting float value of the evaluation.
 */
static float eval_expr(const char* expr) {
    p_expr = expr;
    return parse_expression();
}

/**
 * @brief Executes one frame of the script-controlled OLED animation.
 * 
 * Sets up built-in variables (w, h, t) and processes all lines in the current script buffer.
 * Supports drawing commands (clear, drawLine, drawRect, drawCircle, drawPixel) and 
 * variable assignments with expression evaluation.
 */
static void oled_run_script_frame() {
    // Reset vars but keep global tick
    script_var_count = 0;
    set_var("w", OLED_WIDTH);
    set_var("h", OLED_HEIGHT);
    set_var("t", animation_tick);
    
    oled_lock();
    oled_clear(); 
    
    for(int i=0; i<script_line_count; i++) {
        char line[MAX_LINE_LEN];
        strncpy(line, script_lines[i], MAX_LINE_LEN-1);
        line[MAX_LINE_LEN-1] = 0;
        
        // Remove trailing semicolon if any
        char* semi = strchr(line, ';');
        if(semi) *semi = 0;

        char* p = line;
        while(*p == ' ') p++;
        if(!*p) continue;

        // Check for built-in drawing functions
        if(strncmp(p, "clear()", 7) == 0) {
            oled_clear();
        } else if(strncmp(p, "drawLine(", 9) == 0) {
            char* args = p + 9;
            char* c1 = strchr(args, ','); if(!c1) continue; *c1 = 0;
            char* c2 = strchr(c1+1, ','); if(!c2) continue; *c2 = 0;
            char* c3 = strchr(c2+1, ','); if(!c3) continue; *c3 = 0;
            char* end = strchr(c3+1, ')'); if(!end) continue; *end = 0;
            oled_draw_line((int)eval_expr(args), (int)eval_expr(c1+1), (int)eval_expr(c2+1), (int)eval_expr(c3+1), 1.0f, true);
        } else if(strncmp(p, "drawRect(", 9) == 0) {
            char* args = p + 9;
            char* c1 = strchr(args, ','); if(!c1) continue; *c1 = 0;
            char* c2 = strchr(c1+1, ','); if(!c2) continue; *c2 = 0;
            char* c3 = strchr(c2+1, ','); if(!c3) continue; *c3 = 0;
            char* end = strchr(c3+1, ')'); if(!end) continue; *end = 0;
            oled_draw_rect((int)eval_expr(args), (int)eval_expr(c1+1), (int)eval_expr(c2+1), (int)eval_expr(c3+1), 1.0f, true);
        } else if(strncmp(p, "drawCircle(", 11) == 0) {
            char* args = p + 11;
            char* c1 = strchr(args, ','); if(!c1) continue; *c1 = 0;
            char* c2 = strchr(c1+1, ','); if(!c2) continue; *c2 = 0;
            char* end = strchr(c2+1, ')'); if(!end) continue; *end = 0;
            oled_draw_circle((int)eval_expr(args), (int)eval_expr(c1+1), (int)eval_expr(c2+1), 1.0f, true);
        } else if(strncmp(p, "drawPixel(", 10) == 0) {
            char* args = p + 10;
            char* c1 = strchr(args, ','); if(!c1) continue; *c1 = 0;
            char* end = strchr(c1+1, ')'); if(!end) continue; *end = 0;
            oled_set_pixel_internal((int)eval_expr(args), (int)eval_expr(c1+1), true);
        } else {
            // Check for assignment: [float] var = expr
            if(strncmp(p, "float ", 6) == 0) p += 6;
            else if(strncmp(p, "int ", 4) == 0) p += 4;
            
            char* eq = strchr(p, '=');
            if(eq) {
                *eq = 0;
                // Trim var name
                char* var_name = p;
                while(*var_name == ' ') var_name++;
                char* end_name = eq - 1;
                while(end_name > var_name && *end_name == ' ') { *end_name = 0; end_name--; }
                
                set_var(var_name, eval_expr(eq + 1));
            }
        }
    }
    oled_unlock();
}

/**
 * @brief Renders a single frame of the "text once" animation.
 * 
 * Supports three modes:
 * - Static: Centers the text in the display.
 * - Moving: Scrolls the text horizontally across the screen.
 * - Typing: Reveals the text character by character (pixel by pixel).
 */
static void oled_show_text_once_animation(void) {
    if (text_anim_cfg.width == 0) return;

    // Clear buffer manually
    oled_lock();
    for (int i = 0; i < (OLED_WIDTH * OLED_PAGES); i++) {
        oled_set_raw_byte_internal(i, 0);
    }
    
    int pages = OLED_PAGES; 
    
    if (text_anim_cfg.mode == 0) { // Static mode
        int x_offset = (OLED_WIDTH - text_anim_cfg.width) / 2;
        for (int x = 0; x < OLED_WIDTH; x++) {
            int src_x = x - x_offset;
            if (src_x >= 0 && src_x < text_anim_cfg.width) {
                for (int p = 0; p < pages; p++) {
                    uint8_t byte = text_anim_cfg.data[src_x * pages + p];
                    oled_set_raw_byte_internal(p * OLED_WIDTH + x, byte);
                }
            }
        }
    } else if (text_anim_cfg.mode == 1) { // Moving (scrolling) mode
        int total_range = text_anim_cfg.width + OLED_WIDTH;
        // Speed scaling: app sends 20-255. 
        uint32_t speed = (text_anim_cfg.speed > 0) ? text_anim_cfg.speed : 100;
        // current_pos in 1/100th of a pixel for smooth movement
        uint32_t current_pos_scaled = (text_anim_tick * speed); 
        int current_pos = (current_pos_scaled / 100) % total_range;
        int x_offset = OLED_WIDTH - current_pos;

        for (int x = 0; x < OLED_WIDTH; x++) {
            int src_x = x - x_offset;
            if (src_x >= 0 && src_x < text_anim_cfg.width) {
                for (int p = 0; p < pages; p++) {
                    uint8_t byte = text_anim_cfg.data[src_x * pages + p];
                    oled_set_raw_byte_internal(p * OLED_WIDTH + x, byte);
                }
            }
        }
        text_anim_tick++;
    } else if (text_anim_cfg.mode == 2) { // Typing (reveal) mode
        int total_steps = text_anim_cfg.width;
        uint32_t speed = (text_anim_cfg.speed > 0) ? text_anim_cfg.speed : 100;
        
        // reveal_width in pixels
        int reveal_width = (text_anim_tick * speed / 200);
        if (reveal_width > total_steps + 40) { 
            text_anim_tick = 0;
            reveal_width = 0;
        }
        
        int x_offset = (OLED_WIDTH - text_anim_cfg.width) / 2;
        if (x_offset < 0) x_offset = 0;

        for (int x = 0; x < OLED_WIDTH; x++) {
            int src_x = x - x_offset;
            if (src_x >= 0 && src_x < reveal_width && src_x < text_anim_cfg.width) {
                for (int p = 0; p < pages; p++) {
                    uint8_t byte = text_anim_cfg.data[src_x * pages + p];
                    oled_set_raw_byte_internal(p * OLED_WIDTH + x, byte);
                }
            }
        }
        text_anim_tick++;
    }
    oled_unlock();
}

static uint32_t attack_start_time = 0;
static int current_attack_duration = 60;
static uint32_t attack_finish_display_time = 0;

/**
 * @brief Transitions the application into Advanced Mode.
 * 
 * Resets the attack state, clears the display, and prepares the UI for 
 * scanning or attack management.
 */
static void enter_advanced_mode() {
    ESP_LOGI(TAG, "Entering Advanced Mode");
    // Always initialize/reset attack state when entering advanced mode
    attack_reset();
    
    current_mode = MODE_ADVANCED;
    attack_finish_display_time = 0;
    attack_start_time = 0;
    
    // We don't scan here anymore to avoid blocking the command processor.
    // The App will send a SCAN command when it's ready.
    oled_clear();
}

/**
 * @brief Exits Advanced Mode and returns to the default clock mode.
 * 
 * Stops any active attacks and resets the UI mode to `MODE_CLOCK`.
 */
static void exit_advanced_mode() {
    ESP_LOGI(TAG, "Exiting Advanced Mode");
    attack_stop();
    current_mode = MODE_CLOCK;
}

/**
 * @brief Renders a single frame of the Advanced Mode UI.
 * 
 * This UI handles:
 * - Active attack status (elapsed time, progress bar, target SSID).
 * - Multi-target attack rotation logic for sequential and all-network attacks.
 * - Scanning status and AP list display (when not attacking).
 * - Post-attack result display (Success/Timeout/Completed).
 * - Transitioning between different sub-states within Advanced Mode.
 */
static void oled_draw_advanced_frame() {
    const attack_status_t *status = attack_get_status();
    
    if (status->state == RUNNING) {
        uint32_t now = esp_timer_get_time() / 1000;
        uint32_t elapsed = 0;
        int remaining = 0;
        int progress = 0;

        // Handle multi-target attack UI logic
        if (multi_attack_mode != MULTI_ATTACK_NONE) {
            elapsed = (now - multi_attack_start_time) / 1000;
            remaining = (multi_attack_total_duration > 0) ? (multi_attack_total_duration - elapsed) : -1;
            progress = (multi_attack_total_duration > 0) ? (elapsed * 100 / multi_attack_total_duration) : (animation_tick % 100);
            
            // Internal target rotation check (every multi_attack_config.timeout seconds)
            uint32_t step_elapsed = (now - multi_attack_step_start_time) / 1000;
            if (step_elapsed >= multi_attack_config.timeout) {
                ESP_LOGI(TAG, "Rotating multi-attack UI target after %d seconds", multi_attack_config.timeout);
                multi_attack_step_start_time = now;
                
                if (multi_attack_mode == MULTI_ATTACK_LIST) {
                    multi_attack_current_idx = (multi_attack_current_idx + 1) % multi_attack_count;
                    
                    // Always restart attack for next target
                    const wifictl_ap_records_t *aps = wifictl_get_ap_records();
                    int target_idx = multi_attack_indices[multi_attack_current_idx];
                    if (target_idx >= 0 && target_idx < aps->count) {
                        attack_config_t step_config = multi_attack_config;
                        step_config.ap_record = &aps->records[target_idx];
                        attack_start(&step_config);
                    }
                } else if (multi_attack_mode == MULTI_ATTACK_ALL) {
                    const wifictl_ap_records_t *aps = wifictl_get_ap_records();
                    multi_attack_current_idx++;
                    
                    if (aps->count > 0 && multi_attack_current_idx < aps->count) {
                        // Always restart attack for next target
                        attack_config_t step_config = multi_attack_config;
                        step_config.ap_record = &aps->records[multi_attack_current_idx];
                        attack_start(&step_config);
                    } else {
                        ESP_LOGI(TAG, "ALL mode cycle complete - scanning again");
                        wifictl_scan_nearby_aps();
                        multi_attack_current_idx = -1;
                    }
                }
            }
        } else {
            // Handle single-target attack UI logic
            if (attack_start_time == 0) attack_start_time = now;
            elapsed = (now - attack_start_time) / 1000;
            remaining = (current_attack_duration > 0) ? (current_attack_duration - elapsed) : -1;
            progress = (current_attack_duration > 0) ? (elapsed * 100 / current_attack_duration) : (animation_tick % 100);
        }

        if (remaining < 0 && (multi_attack_total_duration > 0 || current_attack_duration > 0)) remaining = 0;
        if (progress > 100) progress = 100;
        
        char type_buf[32];
        const char *type_str = "ATTACK";
        switch(status->type) {
            case ATTACK_TYPE_DOS: type_str = "DEAUTH"; break;
            case ATTACK_TYPE_HANDSHAKE: 
                // Encode EAPOL mask into the type string for the display to parse
                snprintf(type_buf, sizeof(type_buf), "HS:%u", attack_handshake_get_eapol_mask());
                type_str = type_buf; 
                break;
            case ATTACK_TYPE_PMKID: type_str = "PMKID"; break;
            default: type_str = "ATTACK"; break;
        }
        
        const char *target_ssid = current_target_ssid;
        if (status->type == ATTACK_TYPE_BEACON) {
            target_ssid = "SWARM";
        } else if (multi_attack_mode != MULTI_ATTACK_NONE) {
            const wifictl_ap_records_t *aps = wifictl_get_ap_records();
            if (multi_attack_mode == MULTI_ATTACK_LIST) {
                int target_idx = multi_attack_indices[multi_attack_current_idx];
                if (target_idx >= 0 && target_idx < aps->count) {
                    target_ssid = (const char *)aps->records[target_idx].ssid;
                }
            } else if (multi_attack_mode == MULTI_ATTACK_ALL) {
                if (multi_attack_current_idx >= 0 && multi_attack_current_idx < aps->count) {
                    target_ssid = (const char *)aps->records[multi_attack_current_idx].ssid;
                }
            }
        }

        oled_show_attack_status(type_str, target_ssid, "RUNNING", progress, remaining);
        attack_finish_display_time = 0;
    } 
    else if (status->state == FINISHED || status->state == TIMEOUT) {
        // Handle post-attack display state
        uint32_t now = esp_timer_get_time() / 1000;
        if (attack_finish_display_time == 0) {
            attack_finish_display_time = now;
            
            if (status->type == ATTACK_TYPE_DOS) {
                ble_comm_send_response("ATTACK_COMPLETED");
            } else {
                ble_comm_send_response(status->state == FINISHED ? "ATTACK_SUCCESS" : "ATTACK_TIMEOUT");
            }
            multi_attack_mode = MULTI_ATTACK_NONE;
        }

        const char *type_str = "ATTACK";
        const char *result_str = (status->state == FINISHED) ? "SUCCESS" : "TIMEOUT";
        
        switch(status->type) {
            case ATTACK_TYPE_DOS: 
                type_str = "DEAUTH"; 
                result_str = "COMPLETED";
                break;
            case ATTACK_TYPE_HANDSHAKE: type_str = "HANDSHAKE"; break;
            case ATTACK_TYPE_PMKID: type_str = "PMKID"; break;
            default: type_str = "ATTACK"; break;
        }
        
        oled_show_attack_status(type_str, "DONE", result_str, 100, 0);

        // After 5 seconds, reset attack state but STAY in MODE_ADVANCED
        if (now - attack_finish_display_time > 5000) {
            ESP_LOGI(TAG, "Attack display timeout - returning to READY state");
            attack_finish_display_time = 0;
            attack_start_time = 0;
            attack_reset(); 
            oled_clear(); 
        }
    }
    else if (wifictl_is_scanning()) {
        // Show scanning progress on the screen
        uint8_t primary = 0;
        wifi_second_chan_t second;
        esp_wifi_get_channel(&primary, &second);
        oled_show_scanning(primary);
    }
    else if (multi_attack_mode == MULTI_ATTACK_ALL && status->state == READY) {
        // Handle logic for starting the next target in "ALL" mode
        const wifictl_ap_records_t *aps = wifictl_get_ap_records();
        if (aps->count > 0) {
            // Always use sequential rotation managed by app_logic for consistent timing (6s per target)
            // This ensures all attack types (DOS, BEACON, etc.) behave consistently with the UI
            multi_attack_current_idx = 0;
            multi_attack_config.ap_record = &aps->records[0];
            ESP_LOGI(TAG, "Scan finished, starting ALL mode sequential attack on %d APs", aps->count);
            attack_start(&multi_attack_config);
        } else {
            ESP_LOGW(TAG, "Scan finished in ALL mode but no APs found - scanning again");
            wifictl_scan_nearby_aps();
        }
    }
    else {
        // Default state in Advanced Mode: Show a rotating list of scanned APs
        const wifictl_ap_records_t *aps = wifictl_get_ap_records();
        if (aps->count > 0) {
             static int ap_index = 0;
             static uint32_t last_switch = 0;
             uint32_t now = esp_timer_get_time() / 1000;
             
             if (now - last_switch > 4000) {
                 ap_index = (ap_index + 1) % aps->count;
                 last_switch = now;
             }
             
             const wifi_ap_record_t *ap = &aps->records[ap_index];
             bool secure = ap->authmode != WIFI_AUTH_OPEN;
             oled_show_ap_list((char *)ap->ssid, ap->rssi, secure, ap_index, aps->count);
        } else {
            oled_show_ready_screen();
        }
    }
}

/**
 * @brief FreeRTOS task responsible for rendering all OLED animations and UI frames.
 * 
 * Runs at a steady frame rate (approx. 30 FPS). It manages the display state 
 * machine, calling the appropriate rendering function for the current mode, 
 * updating the system time, and handling game logic for the Snake games.
 * 
 * @param pvParameters Task parameters (unused).
 */
static void anim_task(void *pvParameters) {
    while (1) {
        update_system_time();

        if (current_mode == MODE_STATIC) {
            vTaskDelay(pdMS_TO_TICKS(100));
            continue;
        }

        switch (current_mode) {
            case MODE_ANIM_BOOT: oled_show_boot_screen(); break;
            case MODE_ANIM_STARS: oled_show_starfield(); break;
            case MODE_ANIM_WAVES: oled_show_waves(); break;
            case MODE_ANIM_VIZ: oled_show_visualizer(viz_mode); break;
            case MODE_CLOCK: {
                int display_hour = system_time.hour;
                if (!use_24h_format) {
                    display_hour %= 12;
                    if (display_hour == 0) display_hour = 12;
                }
                oled_show_clock(clock_index, display_hour, system_time.minute, system_time.second, animation_tick); 
                break;
            }
            case MODE_ANIM_DNA: oled_anim_dna(animation_tick); break;
            case MODE_ANIM_FIREWORKS: oled_anim_fireworks(animation_tick); break;
            case MODE_ANIM_TUNNEL: oled_anim_tunnel(animation_tick); break;
            case MODE_ANIM_SNOW: oled_anim_snow(animation_tick); break;
            case MODE_ANIM_VORTEX: oled_anim_vortex(animation_tick); break;
            case MODE_ANIM_CUBE: oled_anim_cube(animation_tick); break;
            case MODE_ANIM_RINGS: oled_anim_rings(animation_tick); break;
            case MODE_ANIM_GLITCH: oled_anim_glitch(animation_tick); break;
            case MODE_ANIM_RAIN: oled_anim_rain(animation_tick); break;
            case MODE_ANIM_PLASMA: oled_anim_plasma(animation_tick); break;
            case MODE_ANIM_CIRCLES: oled_anim_circles(animation_tick); break;
            case MODE_ANIM_NOISE: oled_anim_noise(animation_tick); break;
            case MODE_ANIM_SWARM: oled_anim_swarm(animation_tick); break;
            case MODE_ANIM_WAVE2: oled_anim_wave2(animation_tick); break;
            case MODE_ANIM_METABALLS: oled_anim_metaballs(animation_tick); break;
            case MODE_ANIM_TEXT_ONCE: oled_show_text_once_animation(); break;
            case MODE_ANIM_SCRIPT: oled_run_script_frame(); break;
            case MODE_SNAKE_1X1: {
                uint32_t now = esp_timer_get_time() / 1000;
                if (snake_game.game_over) {
                    if (now - snake_game.game_over_time_ms >= 3000) {
                        snake_1x1_init(&snake_game);
                    }
                } else if (now - snake_game.last_update_ms >= snake_game.update_interval_ms) {
                    snake_1x1_update(&snake_game);
                    snake_game.last_update_ms = now;
                }
                snake_1x1_draw(&snake_game);
                break;
            }
            case MODE_SNAKE_2X2: {
                uint32_t now = esp_timer_get_time() / 1000;
                if (snake_game.game_over) {
                    if (now - snake_game.game_over_time_ms >= 3000) {
                        snake_2x2_init(&snake_game);
                    }
                } else if (now - snake_game.last_update_ms >= snake_game.update_interval_ms) {
                    snake_2x2_update(&snake_game);
                    snake_game.last_update_ms = now;
                }
                snake_2x2_draw(&snake_game);
                break;
            }
            case MODE_SWARM_CONFIG: oled_show_swarm_config(); break;
            case MODE_ADVANCED: oled_draw_advanced_frame(); break;
            default: break;
        }
        oled_update();
        animation_tick++;
        vTaskDelay(pdMS_TO_TICKS(30));
    }
}

/**
 * @brief Internal handler for binary commands received via BLE.
 * 
 * Processes various binary command types:
 * - 0x01 (PX_BIN): Batch pixel drawing.
 * - 0x02 (FULL_FRAME_BIN): Direct raw buffer updates.
 * - 0x03 (FULL_ANIM_BIN): High-speed text animation configuration and data.
 * - 0x04 (FRAGMENTED_ANIM_BIN): Reassembly of large binary payloads split into fragments.
 * 
 * @param data Pointer to the binary data buffer.
 * @param len Length of the data in bytes.
 */
static void app_process_binary_internal(const uint8_t *data, size_t len) {
    if (len < 1) return;
    
    uint8_t cmd_type = data[0];
    if (cmd_type == 0x01) { // PX_BIN: Batch pixel drawing
        current_mode = MODE_STATIC;
        int pixels = (len - 1) / 3;
        int count = 0;
        
        oled_lock(); // Lock for the entire batch to improve performance
        for (int i = 0; i < pixels; i++) {
            int offset = 1 + i * 3;
            // Basic overflow protection for data access
            if (offset + 2 >= len) break;
            
            uint8_t x = data[offset];
            uint8_t y = data[offset + 1];
            uint8_t on = data[offset + 2];
            
            if (x < OLED_WIDTH && y < OLED_HEIGHT) {
                // Use internal version to avoid repeated locking/unlocking overhead
                oled_set_pixel_internal(x, y, on != 0);
                count++;
            }
        }
        if (count > 0) {
            oled_update();
        }
        oled_unlock();
    } else if (cmd_type == 0x02) { // FULL_FRAME_BIN: Direct raw buffer update
        current_mode = MODE_STATIC;
        if (len >= 361) { // 1 byte type + 360 bytes raw OLED data
            oled_lock();
            for (int i = 0; i < 360; i++) {
                oled_set_raw_byte_internal(i, data[i + 1]);
            }
            oled_update();
            oled_unlock();
        }
    } else if (cmd_type == 0x03) { // FULL_ANIM_BIN: High-speed text animation data
        if (len >= 6) {
            text_anim_cfg.mode = data[1];
            text_anim_cfg.speed = data[2];
            text_anim_cfg.width = (data[3] << 8) | data[4];
            text_anim_cfg.height = data[5];
            
            size_t pixel_data_len = len - 6;
            if (pixel_data_len > sizeof(text_anim_cfg.data)) {
                pixel_data_len = sizeof(text_anim_cfg.data);
            }
            
            memcpy(text_anim_cfg.data, &data[6], pixel_data_len);
            text_anim_cfg.data_len = (uint16_t)pixel_data_len;
            text_anim_tick = 0;
            current_mode = MODE_ANIM_TEXT_ONCE;
            ESP_LOGI(TAG, "Recv Anim: mode=%d, speed=%d, width=%d", 
                     text_anim_cfg.mode, text_anim_cfg.speed, text_anim_cfg.width);
        }
    } else if (cmd_type == 0x04) { // FRAGMENTED_ANIM_BIN: Reassembles large binary payloads
        if (len < 5) return;
        
        uint8_t frag_id = data[1];
        uint8_t total_frags = data[2];
        uint8_t data_type = data[3];
        const uint8_t *frag_data = &data[4];
        size_t frag_len = len - 4;
        
        uint32_t now = esp_timer_get_time() / 1000;
        
        // Timeout handling (5 seconds) to clean up stale partial transfers
        if (frag_state.is_active && (now - frag_state.last_activity_ms > 5000)) {
            ESP_LOGW(TAG, "Fragmentation timeout, resetting");
            reset_frag_state();
        }
        
        // Start new reassembly session
        if (!frag_state.is_active) {
            frag_state.is_active = true;
            frag_state.total_fragments = total_frags;
            frag_state.received_fragments = 0;
            frag_state.data_type = data_type;
            frag_state.received_size = 0;
            // Max buffer size for reassembly (4KB) - adjust if larger scripts/anims are needed
            frag_state.buffer = malloc(4096);
            if (!frag_state.buffer) {
                ESP_LOGE(TAG, "Failed to allocate reassembly buffer");
                reset_frag_state();
                return;
            }
        }
        
        // Basic validation of incoming fragment
        if (frag_id >= total_frags || data_type != frag_state.data_type) {
            ESP_LOGE(TAG, "Invalid fragment header: id=%d, total=%d, type=%d", frag_id, total_frags, data_type);
            reset_frag_state();
            return;
        }
        
        // Store fragment into the reassembly buffer using calculated offset
        // Android/iOS app uses a fixed 240 byte chunk size for BLE MTU compatibility
        size_t offset = frag_id * 240;
        if (offset + frag_len > 4096) {
            ESP_LOGE(TAG, "Fragmented data exceeds buffer size");
            reset_frag_state();
            return;
        }
        
        memcpy(frag_state.buffer + offset, frag_data, frag_len);
        frag_state.received_fragments++;
        frag_state.last_activity_ms = now;
        
        // Track the actual total size once the last fragment is received
        if (frag_id == total_frags - 1) {
            frag_state.total_size = offset + frag_len;
        }
        
        // Check if all fragments have arrived
        if (frag_state.received_fragments == frag_state.total_fragments) {
            ESP_LOGI(TAG, "Reassembly complete: %d fragments, %zu bytes", 
                     frag_state.total_fragments, frag_state.total_size);
            
            // Construct a temporary buffer to pass to the recursive processor
            uint8_t *complete_cmd = malloc(frag_state.total_size + 1);
            if (complete_cmd) {
                complete_cmd[0] = frag_state.data_type;
                memcpy(complete_cmd + 1, frag_state.buffer, frag_state.total_size);
                app_process_binary_internal(complete_cmd, frag_state.total_size + 1);
                free(complete_cmd);
            }
            
            reset_frag_state();
        }
    }
}

/**
 * @brief Internal handler for text commands received via BLE.
 * 
 * Parses and executes human-readable commands for:
 * - Mode switching (MODE:ADVANCED, MODE:NORMAL)
 * - Wi-Fi scanning (SCAN)
 * - Stopping attacks (STOP)
 * - Manual deauthentication (DEAUTH)
 * - PCAP management (SAVE_PCAP, DELETE_PCAP)
 * - Swarm SSID updates (SWARM_SSIDS:)
 * - Starting specific attacks (ATTACK:)
 * - Multi-target attack coordination
 * - Game and animation selection
 * - System time synchronization (SYNC_TIME:)
 * - OLED script management (SCRIPT_START, SCRIPT_LINE, SCRIPT_RUN)
 * 
 * @param cmd Null-terminated command string.
 */
static void app_process_text_internal(const char *cmd) {
    ESP_LOGI(TAG, "RECV CMD: %s", cmd);
    if (strncmp(cmd, "MODE:", 5) == 0) {
        if (strcmp(cmd, "MODE:ADVANCED") == 0) enter_advanced_mode();
        else if (strcmp(cmd, "MODE:NORMAL") == 0) exit_advanced_mode();
        return;
    } else if (strcmp(cmd, "SCAN") == 0) {
        // Trigger a nearby AP scan and propagate results back to the app via BLE
        if (current_mode == MODE_ADVANCED) {
            ESP_LOGI(TAG, "SCAN command received - starting BLE response propagation");
            wifictl_scan_nearby_aps();
            
            // Iterate through discovered networks and send details one by one
            const wifictl_ap_records_t *aps = wifictl_get_ap_records();
            if (aps->count > 0) {
                // Format: SCAN_RESULT:TOTAL_COUNT:CURRENT_INDEX:SSID:BSSID:RSSI:CHANNEL:IS_SECURE
                for (int i = 0; i < aps->count; i++) {
                    char buf[128];
                    const wifi_ap_record_t *ap = &aps->records[i];
                    snprintf(buf, sizeof(buf), "SCAN_RESULT:%d:%d:%s:%02x-%02x-%02x-%02x-%02x-%02x:%d:%d:%d", 
                             aps->count, i, ap->ssid, 
                             ap->bssid[0], ap->bssid[1], ap->bssid[2],
                             ap->bssid[3], ap->bssid[4], ap->bssid[5],
                             ap->rssi, ap->primary, ap->authmode != WIFI_AUTH_OPEN);
                    ble_comm_send_response(buf);
                    vTaskDelay(pdMS_TO_TICKS(50)); // Small delay to prevent flooding the BLE buffer
                }
            } else {
                ble_comm_send_response("SCAN_RESULT:0:0:NO_NETWORKS:00-00-00-00-00-00:0:0:0");
            }
        }
        return;
    } else if (strcmp(cmd, "STOP") == 0) {
        // Immediate termination of any active attack or multi-target rotation
        ESP_LOGI(TAG, "Stop attack requested");
        multi_attack_mode = MULTI_ATTACK_NONE;
        attack_stop();
        return;
    } else if (strcmp(cmd, "DEAUTH") == 0) {
        // Manual trigger for a single deauth frame (used in handshake capture tuning)
        ESP_LOGI(TAG, "Manual deauth requested");
        attack_handshake_request_deauth();
        return;
    } else if (strncmp(cmd, "SAVE_PCAP", 9) == 0) {
        // Request to download a captured PCAP file (handshake or traffic) over BLE
        if (current_mode == MODE_ADVANCED) {
            int slot = 0;
            if (cmd[9] == ':') {
                slot = atoi(&cmd[10]);
            }
            
            if (slot < 0 || slot >= 2) slot = 0;

            uint8_t *pcap_buf = pcap_serializer_get_buffer_at(slot);
            unsigned pcap_size = pcap_serializer_get_size_at(slot);
            
            if (pcap_buf != NULL && pcap_size > 0) {
                // Inform the app about the file size and slot
                char start_msg[64];
                snprintf(start_msg, sizeof(start_msg), "FILE_START:%u:%d", pcap_size, slot);
                ble_comm_send_response(start_msg);
                vTaskDelay(pdMS_TO_TICKS(100)); // Wait for app to prepare its reception buffer
                
                // Send file in small chunks to fit within BLE MTU and prevent congestion
                const size_t chunk_size = 200;
                for (size_t i = 0; i < pcap_size; i += chunk_size) {
                    size_t remaining = pcap_size - i;
                    size_t current_chunk = (remaining < chunk_size) ? remaining : chunk_size;
                    
                    uint8_t *payload = malloc(current_chunk + 10);
                    if (payload) {
                        memcpy(payload, "FILE_DATA:", 10);
                        memcpy(payload + 10, pcap_buf + i, current_chunk);
                        ble_comm_send_binary_response(payload, current_chunk + 10);
                        free(payload);
                    }
                    vTaskDelay(pdMS_TO_TICKS(30)); // Delay between chunks for stability
                }
                
                vTaskDelay(pdMS_TO_TICKS(100));
                ble_comm_send_response("FILE_END");
            } else {
                ble_comm_send_response("FILE_ERROR:EMPTY");
            }
        }
        return;
    } else if (strncmp(cmd, "SWARM_SSIDS:", 12) == 0) {
        // Update the list of SSIDs used for beacon flooding (Swarm mode)
        const char *ssids = cmd + 12;
        attack_beacon_set_ssids(ssids);
        ESP_LOGI(TAG, "Swarm SSIDs updated from BLE");
        ble_comm_send_response("SWARM_SSIDS_UPDATED");
        return;
    } else if (strncmp(cmd, "ATTACK:", 7) == 0) {
        // Main attack configuration command
        if (current_mode == MODE_ADVANCED) {
            char type[16] = {0};
            int method = 0;
            char index_str[64] = {0};
            int duration = 60;
            
            // Reset multi-attack state before applying new config
            multi_attack_mode = MULTI_ATTACK_NONE;
            multi_attack_count = 0;
            multi_attack_current_idx = 0;

            // Format: ATTACK:TYPE:METHOD:INDEX:DURATION
            if (sscanf(cmd, "ATTACK:%[^:]:%d:%[^:]:%d", type, &method, index_str, &duration) >= 3) {
                ESP_LOGI(TAG, "Parsed Attack: Type=%s, Method=%d, Index=%s, Duration=%d", type, method, index_str, duration);
                
                attack_config_t config = {0};
                config.method = method;
                config.timeout = duration;
                
                // Map text type to internal enum
                if (strcmp(type, "DOS") == 0) config.type = ATTACK_TYPE_DOS;
                else if (strcmp(type, "HANDSHAKE") == 0) config.type = ATTACK_TYPE_HANDSHAKE;
                else if (strcmp(type, "BEACON") == 0) config.type = ATTACK_TYPE_BEACON;
                else if (strcmp(type, "PMKID") == 0) config.type = ATTACK_TYPE_PMKID;
                
                current_attack_duration = duration;
                attack_start_time = 0; // Reset for OLED progress tracking
                multi_attack_start_time = esp_timer_get_time() / 1000;
                multi_attack_step_start_time = multi_attack_start_time;
                multi_attack_total_duration = duration;
                
                if (strcmp(index_str, "ALL") == 0) {
                    // Attack all discovered networks sequentially
                    multi_attack_mode = MULTI_ATTACK_ALL;
                    multi_attack_config = config;
                    multi_attack_config.timeout = 6; // 6 seconds per target for UI rotation visibility
                    strncpy(multi_attack_type, type, 15);
                    ESP_LOGI(TAG, "Starting ALL networks attack loop - scanning first");
                    wifictl_scan_nearby_aps();
                } else if (strchr(index_str, ',')) {
                    // Attack a specific list of indices (e.g., "1,3,5")
                    multi_attack_mode = MULTI_ATTACK_LIST;
                    multi_attack_config = config;
                    multi_attack_config.timeout = 6; // Rotation interval for UI
                    strncpy(multi_attack_type, type, 15);
                    
                    char *p = index_str;
                    multi_attack_count = 0;
                    while (p && multi_attack_count < 32) {
                        multi_attack_indices[multi_attack_count++] = atoi(p);
                        p = strchr(p, ',');
                        if (p) p++;
                    }
                    
                    ESP_LOGI(TAG, "Starting Multi-target attack: %d targets", multi_attack_count);
                    const wifictl_ap_records_t *aps = wifictl_get_ap_records();
                    
                    // Always use sequential rotation managed by app_logic for consistent timing
                    int target_idx = multi_attack_indices[0];
                    if (target_idx >= 0 && target_idx < aps->count) {
                        attack_config_t step_config = multi_attack_config;
                        step_config.ap_record = &aps->records[target_idx];
                        attack_start(&step_config);
                    }
                } else {
                    // Single target attack
                    int index = atoi(index_str);
                    if (config.type != ATTACK_TYPE_BEACON) {
                        const wifictl_ap_records_t *aps = wifictl_get_ap_records();
                        if (index >= 0 && index < aps->count) {
                            config.ap_record = &aps->records[index];
                            strncpy(current_target_ssid, (char*)config.ap_record->ssid, 32);
                            current_target_ssid[32] = '\0';
                            ESP_LOGI(TAG, "Starting targeted attack: Index=%d, SSID='%s', BSSID=%02x:%02x:%02x:%02x:%02x:%02x", 
                                     index, current_target_ssid,
                                     config.ap_record->bssid[0], config.ap_record->bssid[1], config.ap_record->bssid[2],
                                     config.ap_record->bssid[3], config.ap_record->bssid[4], config.ap_record->bssid[5]);
                            attack_start(&config);
                        } else {
                            ESP_LOGE(TAG, "Attack target index out of bounds! index=%d, count=%d. Check if scan results match App list.", index, aps->count);
                        }
                    } else {
                        // Beacon attacks don't require a specific target from scan list
                        ESP_LOGI(TAG, "Starting Beacon attack");
                        attack_start(&config);
                    }
                }
            } else {
                ESP_LOGE(TAG, "Failed to parse ATTACK command: %s", cmd);
            }
        }
        return;
    }

    if (strncmp(cmd, "CLEAR", 5) == 0) {
        // Clear the OLED display and force a refresh to hardware
        current_mode = MODE_STATIC;
        oled_clear();
        oled_force_refresh(); // Ensure the hardware controller is fully cleared
        // Force a yield to allow Bluetooth controller to handle its internal maintenance
        vTaskDelay(pdMS_TO_TICKS(5));
    } else if (strcmp(cmd, "HB") == 0) {
        // Heartbeat command - used to maintain connection and check responsiveness
        // A "READY" response is sent by the cmd_processor_task automatically
    } else if (strncmp(cmd, "PX:", 3) == 0) {
        // Batch pixel drawing command from text protocol
        current_mode = MODE_STATIC;
        const char *p = cmd + 3;
        int x, y, on;
        int count = 0;
        oled_lock();
        while (p && *p) {
            if (sscanf(p, "%d,%d,%d", &x, &y, &on) == 3) {
                oled_set_pixel_internal(x, y, on != 0);
                count++;
                p = strchr(p, ';');
                if (p) p++;
            } else {
                break;
            }
        }
        if (count > 0) {
            oled_update();
        }
        oled_unlock();
    } else if (strncmp(cmd, "PIXEL:", 6) == 0) {
        // Single pixel update
        current_mode = MODE_STATIC;
        int x, y, on;
        if (sscanf(cmd, "PIXEL:%d:%d:%d", &x, &y, &on) == 3) {
            oled_set_pixel(x, y, on != 0);
            oled_update();
        }
    } else if (strncmp(cmd, "BRUSH:", 6) == 0) {
        // Circle brush drawing
        current_mode = MODE_STATIC;
        int x, y, on;
        float size;
        if (sscanf(cmd, "BRUSH:%d:%d:%f:%d", &x, &y, &size, &on) == 4) {
            oled_draw_brush(x, y, size, on != 0);
            oled_update();
        }
    } else if (strncmp(cmd, "RECT:", 5) == 0) {
        // Rectangle drawing
        int x, y, w, h;
        float size;
        if (sscanf(cmd, "RECT:%d:%d:%d:%d:%f", &x, &y, &w, &h, &size) == 5) {
            current_mode = MODE_STATIC;
            oled_draw_rect(x, y, w, h, size, true);
            oled_update();
        }
    } else if (strncmp(cmd, "CIRCLE:", 7) == 0) {
        // Circle drawing
        int x, y, r;
        float size;
        if (sscanf(cmd, "CIRCLE:%d:%d:%d:%f", &x, &y, &r, &size) == 4) {
            current_mode = MODE_STATIC;
            oled_draw_circle(x, y, r, size, true);
            oled_update();
        }
    } else if (strncmp(cmd, "LINE:", 5) == 0) {
        // Line drawing
        int x0, y0, x1, y1;
        float size;
        if (sscanf(cmd, "LINE:%d:%d:%d:%d:%f", &x0, &y0, &x1, &y1, &size) == 5) {
            current_mode = MODE_STATIC;
            oled_draw_line(x0, y0, x1, y1, size, true);
            oled_update();
        }
    } else if (strncmp(cmd, "ROW:", 4) == 0) {
        // Raw row update using hex-encoded bytes
        int y;
        char hex[32];
        if (sscanf(cmd, "ROW:%d:%31s", &y, hex) == 2) {
            current_mode = MODE_STATIC;
            oled_lock();
            for (int i = 0; i < 9; i++) {
                char byte_hex[3] = {hex[i*2], hex[i*2+1], '\0'};
                uint8_t byte_val = (uint8_t)strtol(byte_hex, NULL, 16);
                for (int bit = 0; bit < 8; bit++) {
                    oled_set_pixel_internal(i * 8 + bit, y, (byte_val >> (7 - bit)) & 0x01);
                }
            }
            // Note: oled_update() is omitted here for batch efficiency; app sends "UPDATE" command at end
            oled_unlock();
        }
    } else if (strncmp(cmd, "GET_SWARM", 9) == 0) {
        // Retrieve current swarm SSID list for editing in the app
        char *buffer = malloc(1024);
        if (buffer) {
            strcpy(buffer, "SWARM:");
            attack_beacon_get_ssids(buffer + 6, 1018);
            ble_comm_send_response(buffer);
            free(buffer);
        }
        return;
    } else if (strncmp(cmd, "SWARM_VIEW", 10) == 0) {
        // Switch to swarm configuration display mode
        current_mode = MODE_SWARM_CONFIG;
        return;
    } else if (strncmp(cmd, "SET_SWARM:", 10) == 0) {
        // Apply new SSID list for beacon flooding
        const char *ssids = cmd + 10;
        attack_beacon_set_ssids(ssids);
        ble_comm_send_response("SWARM_UPDATED");
        return;
    } else if (strncmp(cmd, "BUF:", 4) == 0) {
        // Bulk raw buffer update using hex encoding
        int offset;
        char hex[257];
        if (sscanf(cmd, "BUF:%d:%256s", &offset, hex) == 2) {
            current_mode = MODE_STATIC;
            int len = strlen(hex) / 2;
            oled_lock();
            for (int i = 0; i < len; i++) {
                char byte_hex[3] = {hex[i*2], hex[i*2+1], '\0'};
                uint8_t byte_val = (uint8_t)strtol(byte_hex, NULL, 16);
                if (offset + i < (OLED_WIDTH * OLED_HEIGHT / 8)) {
                    oled_set_raw_byte_internal(offset + i, byte_val);
                }
            }
            oled_unlock();
        }
    } else if (strncmp(cmd, "UPDATE", 6) == 0) {
        // Manually trigger an OLED hardware refresh
        oled_update();
    } else if (strncmp(cmd, "TIME:FMT:", 9) == 0) {
        // Configure clock display format (12h/24h)
        if (strstr(cmd, "24")) use_24h_format = true;
        else if (strstr(cmd, "12")) use_24h_format = false;
    } else if (strncmp(cmd, "BOOT_ANIM:", 10) == 0) {
        // Select and start a specific background animation
        if (strstr(cmd, "STARS")) current_mode = MODE_ANIM_STARS;
        else if (strstr(cmd, "WAVES")) current_mode = MODE_ANIM_WAVES;
        else if (strstr(cmd, "DNA")) current_mode = MODE_ANIM_DNA;
        else if (strstr(cmd, "FIREWORKS")) current_mode = MODE_ANIM_FIREWORKS;
        else if (strstr(cmd, "TUNNEL")) current_mode = MODE_ANIM_TUNNEL;
        else if (strstr(cmd, "SNOW")) current_mode = MODE_ANIM_SNOW;
        else if (strstr(cmd, "VORTEX")) current_mode = MODE_ANIM_VORTEX;
        else if (strstr(cmd, "CUBE")) current_mode = MODE_ANIM_CUBE;
        else if (strstr(cmd, "RINGS")) current_mode = MODE_ANIM_RINGS;
        else if (strstr(cmd, "GLITCH")) current_mode = MODE_ANIM_GLITCH;
        else if (strstr(cmd, "RAIN")) current_mode = MODE_ANIM_RAIN;
        else if (strstr(cmd, "PLASMA")) current_mode = MODE_ANIM_PLASMA;
        else if (strstr(cmd, "CIRCLES")) current_mode = MODE_ANIM_CIRCLES;
        else if (strstr(cmd, "NOISE")) current_mode = MODE_ANIM_NOISE;
        else if (strstr(cmd, "SWARM")) current_mode = MODE_ANIM_SWARM;
        else if (strstr(cmd, "WAVE2")) current_mode = MODE_ANIM_WAVE2;
        else if (strstr(cmd, "METABALLS")) current_mode = MODE_ANIM_METABALLS;
        animation_tick = 0; // Reset animation tick when changing mode for smooth start
    } else if (strncmp(cmd, "SNAKE:", 6) == 0) {
        // Launch a Snake game variant
        if (strstr(cmd, "1X1")) {
            current_mode = MODE_SNAKE_1X1;
            snake_1x1_init(&snake_game);
        } else if (strstr(cmd, "2X2")) {
            current_mode = MODE_SNAKE_2X2;
            snake_2x2_init(&snake_game);
        }
        animation_tick = 0;
    } else if (strncmp(cmd, "MUSIC:VIZ:", 10) == 0) {
        // Select and start an audio visualizer animation
        if (strstr(cmd, "BARS")) viz_mode = 0;
        else if (strstr(cmd, "WAVE")) viz_mode = 1;
        else if (strstr(cmd, "PEAK")) viz_mode = 2;
        current_mode = MODE_ANIM_VIZ;
        animation_tick = 0;
    } else if (strncmp(cmd, "CLOCK:", 6) == 0) {
        // Select and start a specific clock face
        if (strstr(cmd, "MINIMAL")) clock_index = 1;
        else if (strstr(cmd, "CYBER")) clock_index = 2;
        else if (strstr(cmd, "BIG_HOUR")) clock_index = 11;
        else if (strstr(cmd, "MODERN")) clock_index = 12;
        current_mode = MODE_CLOCK;
        animation_tick = 0;
        oled_force_refresh(); // Force refresh to clear previous content
    } else if (strncmp(cmd, "SYNC_TIME:", 10) == 0) {
        // Sync internal clock with the connected app
        if (current_mode == MODE_ADVANCED) {
            // Performance optimization: skip sync during active attacks
            ESP_LOGI(TAG, "Time sync skipped in Advanced Mode for performance");
        } else {
            int h, m, s;
            if (sscanf(cmd, "SYNC_TIME:%d:%d:%d", &h, &m, &s) == 3) {
                app_set_time(h, m, s);
            }
        }
    } else if (strcmp(cmd, "SCRIPT_START") == 0) {
        // Begin receiving lines for a custom OLED animation script
        current_mode = MODE_STATIC;
        script_line_count = 0;
        ESP_LOGI(TAG, "Script Start");
    } else if (strncmp(cmd, "SCRIPT_LINE:", 12) == 0) {
        // Add a line to the current script buffer
        if (script_line_count < MAX_SCRIPT_LINES) {
            const char* line = cmd + 12;
            strncpy(script_lines[script_line_count], line, MAX_LINE_LEN - 1);
            script_lines[script_line_count][MAX_LINE_LEN - 1] = 0;
            script_line_count++;
        }
    } else if (strcmp(cmd, "SCRIPT_RUN") == 0) {
        // Execute the received script
        current_mode = MODE_ANIM_SCRIPT;
        animation_tick = 0;
        ESP_LOGI(TAG, "Script Run: %d lines", script_line_count);
    } else {
        ESP_LOGW(TAG, "Unknown or unsupported command: %s", cmd);
        return;
    }
}

/**
 * @brief FreeRTOS task responsible for processing commands from the queue.
 * 
 * Continuously waits for new command messages in `cmd_queue`. Messages are 
 * routed to either `app_process_binary_internal` or `app_process_text_internal` 
 * based on their type. After processing, a "READY" response is sent via BLE 
 * to acknowledge completion, and the message memory is freed.
 * 
 * @param pvParameters Task parameters (unused).
 */
static void cmd_processor_task(void *pvParameters) {
    cmd_msg_t *msg;
    ESP_LOGI(TAG, "Command processor task started");
    while (1) {
        // Wait for a command to arrive in the queue
        if (xQueueReceive(cmd_queue, &msg, portMAX_DELAY) == pdTRUE) {
            // Route based on command type
            if (msg->type == CMD_TYPE_BINARY) {
                app_process_binary_internal(msg->data, msg->len);
            } else {
                msg->data[msg->len] = '\0'; // Ensure null-termination for text commands
                app_process_text_internal((const char *)msg->data);
            }
            // Always send READY after processing a command (binary or text) to acknowledge completion
            ble_comm_send_response("READY");
            
            // Free the message after processing to prevent memory leaks
            free(msg);
            
            // Increase yield time to 5ms to give btController more room for BLE maintenance
            vTaskDelay(pdMS_TO_TICKS(5));
        }
    }
}

/**
 * @brief Public interface to queue a binary command for processing.
 * 
 * Allocates a new command message and sends it to the command processor queue.
 * Includes flow control to drop commands if the queue is full.
 * 
 * @param data Pointer to binary data to be processed.
 * @param len Length of the binary data.
 */
void app_handle_binary_command(const uint8_t *data, size_t len) {
    if (cmd_queue == NULL || data == NULL || len == 0) return;
    
    // Check for overflow - if queue is nearly full, skip less critical binary commands
    if (uxQueueSpacesAvailable(cmd_queue) == 0) {
        ESP_LOGW(TAG, "Command queue full, dropping binary command for flow control");
        return;
    }

    cmd_msg_t *msg = (cmd_msg_t *)malloc(sizeof(cmd_msg_t));
    if (msg == NULL) {
        ESP_LOGE(TAG, "Failed to allocate memory for binary command");
        return;
    }

    msg->type = CMD_TYPE_BINARY;
    msg->len = (len > MAX_CMD_LEN) ? MAX_CMD_LEN : len;
    memcpy(msg->data, data, msg->len);
    
    // Use a short timeout to avoid dropping commands under heavy load while still being non-blocking
    if (xQueueSend(cmd_queue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
        ESP_LOGW(TAG, "Command queue full, dropping binary command after timeout");
        free(msg);
    }
}

/**
 * @brief Public interface to queue a text command for processing.
 * 
 * Allocates a new command message for the provided text string and sends 
 * it to the command processor queue.
 * 
 * @param cmd Null-terminated text command string.
 */
void app_handle_command(const char *cmd) {
    if (cmd_queue == NULL || cmd == NULL) return;
    
    cmd_msg_t *msg = (cmd_msg_t *)malloc(sizeof(cmd_msg_t));
    if (msg == NULL) {
        ESP_LOGE(TAG, "Failed to allocate memory for text command");
        return;
    }

    msg->type = CMD_TYPE_TEXT;
    msg->len = strlen(cmd);
    if (msg->len >= MAX_CMD_LEN) msg->len = MAX_CMD_LEN - 1;
    memcpy(msg->data, cmd, msg->len);
    msg->data[msg->len] = '\0';
    
    // Use a short timeout to avoid dropping commands under heavy load
    if (xQueueSend(cmd_queue, &msg, pdMS_TO_TICKS(10)) != pdTRUE) {
        ESP_LOGW(TAG, "Command queue full, dropping text command: %s", cmd);
        free(msg);
    }
}


/**
 * @brief Manually sets the internal system time and updates the last update timestamp.
 * 
 * @param h Hours (0-23).
 * @param m Minutes (0-59).
 * @param s Seconds (0-59).
 */
void app_set_time(int h, int m, int s) {
    system_time.hour = h % 24;
    system_time.minute = m % 60;
    system_time.second = s % 60;
    last_time_update_ms = esp_timer_get_time() / 1000;
    ESP_LOGI(TAG, "Time synced: %02d:%02d:%02d", system_time.hour, system_time.minute, system_time.second);
}

/**
 * @brief FreeRTOS task for monitoring internal SoC temperature.
 * 
 * Installs and enables the built-in temperature sensor, then periodically 
 * reads the temperature and sends it over BLE (TEMP:XX.X) if a connection 
 * is active. Updates every 2 seconds.
 * 
 * @param pvParameters Task parameters (unused).
 */
static void temperature_task(void *pvParameters) {
    temperature_sensor_handle_t temp_handle = NULL;
    temperature_sensor_config_t temp_sensor_config = TEMPERATURE_SENSOR_CONFIG_DEFAULT(10, 80);
    
    esp_err_t err = temperature_sensor_install(&temp_sensor_config, &temp_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to install temperature sensor: %s", esp_err_to_name(err));
        vTaskDelete(NULL);
        return;
    }

    err = temperature_sensor_enable(temp_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to enable temperature sensor: %s", esp_err_to_name(err));
        temperature_sensor_uninstall(temp_handle);
        vTaskDelete(NULL);
        return;
    }

    float tsens_out;
    char temp_str[32];

    while (1) {
        // Only read and send if BLE is connected to save power/cycles
        if (ble_comm_is_connected()) {
            err = temperature_sensor_get_celsius(temp_handle, &tsens_out);
            if (err == ESP_OK) {
                // Send in format TEMP:XX.X
                // The app will handle color-coding based on the value
                snprintf(temp_str, sizeof(temp_str), "TEMP:%.1f", tsens_out);
                ble_comm_send_response(temp_str);
            } else {
                ESP_LOGW(TAG, "Failed to get temperature: %s", esp_err_to_name(err));
            }
        }
        vTaskDelay(pdMS_TO_TICKS(2000)); // 2 seconds update frequency as requested
    }
}

/**
 * @brief Initializes the application logic component.
 * 
 * This includes:
 * - Initializing Wi-Fi controller and attack manager.
 * - Loading saved SSIDs for beacon attacks.
 * - Creating the command processing queue.
 * - Starting the command processor, animation, and temperature monitoring tasks.
 */
void app_logic_init(void) {
    wifictl_init();
    attack_init();
    attack_beacon_load_ssids();
    cmd_queue = xQueueCreate(CMD_QUEUE_SIZE, sizeof(cmd_msg_t*));
    if (cmd_queue == NULL) {
        ESP_LOGE(TAG, "Failed to create command queue");
        return;
    }
    
    // Create processor task with reasonable priority
    // Priority 8 is above anim_task (5) but low enough to avoid excessive starvation
    xTaskCreate(cmd_processor_task, "cmd_proc_task", 4096, NULL, 8, NULL);
    
    xTaskCreate(anim_task, "anim_task", 4096, NULL, 5, &anim_task_handle);
    xTaskCreate(temperature_task, "temp_task", 2048, NULL, 4, NULL);
    ESP_LOGI(TAG, "Application logic initialized with queue and tasks");
}

