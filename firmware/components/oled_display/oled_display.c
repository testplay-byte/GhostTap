#include <string.h>
#include <stdlib.h>
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#include "oled_display.h"
#include "font5x7.h"
#include "font8x16.h"
#include "attack_beacon.h"
#include "esp_log.h"
#include "driver/i2c_master.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

static const char *TAG = "OLED";

#include "freertos/semphr.h"

// Global display handles and buffers
static i2c_master_bus_handle_t bus_handle = NULL;
static i2c_master_dev_handle_t dev_handle = NULL;
static uint8_t display_buffer[OLED_WIDTH * OLED_PAGES];
static bool is_dirty = true;
static int dirty_min_page = 0;
static int dirty_max_page = OLED_PAGES - 1;
static SemaphoreHandle_t oled_mutex = NULL;

#define LOCK_OLED() if (oled_mutex) xSemaphoreTakeRecursive(oled_mutex, portMAX_DELAY)
#define UNLOCK_OLED() if (oled_mutex) xSemaphoreGiveRecursive(oled_mutex)

/**
 * @brief Lock the OLED display for exclusive access.
 * 
 * Uses a recursive mutex to ensure thread-safety during multi-step drawing operations.
 */
void oled_lock(void) {
    LOCK_OLED();
}

/**
 * @brief Unlock the OLED display.
 */
void oled_unlock(void) {
    UNLOCK_OLED();
}

/**
 * @brief Displays the current Beacon Swarm (SSID flood) configuration.
 * 
 * Retrieves the list of SSIDs from the beacon attack module and displays
 * the first few entries on the screen.
 */
void oled_show_swarm_config(void) {
    LOCK_OLED();
    oled_clear();
    
    oled_draw_string(2, 0, "SWARM CONFIG", false);
    oled_draw_line(0, 8, OLED_WIDTH, 8, 1.0f, true);
    
    char buffer[512];
    attack_beacon_get_ssids(buffer, sizeof(buffer));
    
    // Display first 3-4 SSIDs in a list
    char *token = strtok(buffer, ",");
    int y = 12;
    int count = 0;
    while (token != NULL && count < 3) {
        char line[20];
        snprintf(line, sizeof(line), "> %s", token);
        oled_draw_string(2, y, line, false);
        y += 10;
        count++;
        token = strtok(NULL, ",");
    }
    
    if (token != NULL) {
        oled_draw_string(2, y, "... (MORE)", false);
    } else if (count == 0) {
        oled_draw_string(10, 20, "NO SSIDS", false);
    }
    
    // Pulse animation at the bottom
    int pulse_x = (animation_tick % 20) < 10 ? 68 : 70;
    oled_set_pixel_internal(pulse_x, 38, true);

    UNLOCK_OLED();
}

// Animation state
uint32_t animation_tick = 0;

/**
 * @brief Internal helper to send a command byte to the SSD1306 controller.
 * 
 * @param cmd The command byte to send.
 * @return esp_err_t ESP_OK on success, or error code.
 */
static esp_err_t send_cmd(uint8_t cmd) {
    if (dev_handle == NULL) return ESP_ERR_INVALID_STATE;
    uint8_t data[2] = {0x00, cmd};
    return i2c_master_transmit(dev_handle, data, 2, 100); // 100ms timeout
}

/**
 * @brief Internal helper to send a data buffer to the SSD1306 controller.
 * 
 * Implements chunking to stay within I2C transmission limits and improve stability.
 * 
 * @param data Pointer to the data buffer.
 * @param len Length of the data to send.
 * @return esp_err_t ESP_OK on success, or error code.
 */
static esp_err_t send_data(uint8_t *data, size_t len) {
    if (dev_handle == NULL) return ESP_ERR_INVALID_STATE;
    if (len == 0) return ESP_OK;
    if (len > OLED_WIDTH * OLED_PAGES) return ESP_ERR_INVALID_SIZE;

    // Use a slightly larger buffer for the command byte + chunk
    static uint8_t i2c_transmission_buffer[129]; 
    size_t sent = 0;
    
    while (sent < len) {
        // Try sending larger chunks for better performance, but stay within I2C limits
        size_t to_send = (len - sent > 72) ? 72 : (len - sent); 
        i2c_transmission_buffer[0] = 0x40; // Data mode
        memcpy(i2c_transmission_buffer + 1, data + sent, to_send);
        
        esp_err_t ret = i2c_master_transmit(dev_handle, i2c_transmission_buffer, to_send + 1, 50); 
        if (ret != ESP_OK) return ret;
        
        sent += to_send;
    }
    return ESP_OK;
}

/**
 * @brief Initializes the OLED display using the provided I2C pins.
 * 
 * Sets up the I2C bus, adds the SSD1306 device, and performs the full 
 * hardware initialization sequence for a 72x40 resolution display.
 * 
 * @param sda_pin GPIO number for SDA.
 * @param scl_pin GPIO number for SCL.
 * @return true if initialization succeeded, false otherwise.
 */
bool oled_init(gpio_num_t sda_pin, gpio_num_t scl_pin) {
    ESP_LOGI(TAG, "Initializing OLED display (SDA: %d, SCL: %d)", sda_pin, scl_pin);
    
    if (oled_mutex == NULL) {
        oled_mutex = xSemaphoreCreateRecursiveMutex();
    }
    
    // 1. Initialize I2C Master Bus
    if (bus_handle == NULL) {
        i2c_master_bus_config_t i2c_mst_config = {
            .clk_source = I2C_CLK_SRC_DEFAULT,
            .i2c_port = I2C_MASTER_NUM,
            .scl_io_num = scl_pin,
            .sda_io_num = sda_pin,
            .glitch_ignore_cnt = 7,
            .flags.enable_internal_pullup = true,
        };
        esp_err_t ret = i2c_new_master_bus(&i2c_mst_config, &bus_handle);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG, "Failed to create I2C bus");
            return false;
        }
    }

    // 2. Add OLED Device to Bus
    if (dev_handle == NULL) {
        i2c_device_config_t dev_cfg = {
            .dev_addr_length = I2C_ADDR_BIT_LEN_7,
            .device_address = OLED_I2C_ADDRESS,
            .scl_speed_hz = I2C_MASTER_FREQ_HZ,
        };
        esp_err_t ret = i2c_master_bus_add_device(bus_handle, &dev_cfg, &dev_handle);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG, "Failed to add OLED device to I2C bus");
            return false;
        }
    }

    // SSD1306 Initialization for 72x40
    vTaskDelay(pdMS_TO_TICKS(100)); // Give OLED power time to stabilize
    
    // Attempt initialization with individual error checking
    #define SEND_CMD_CHECK(c) if (send_cmd(c) != ESP_OK) { ESP_LOGE(TAG, "Failed at cmd 0x%02X", (uint8_t)c); return false; }

    SEND_CMD_CHECK(0xAE); // Display Off
    SEND_CMD_CHECK(0xD5); // Set Display Clock Divide Ratio
    SEND_CMD_CHECK(0x80);
    SEND_CMD_CHECK(0xA8); // Set Multiplex Ratio
    SEND_CMD_CHECK(0x27); // 40 lines (0x27 = 39)
    SEND_CMD_CHECK(0xD3); // Set Display Offset
    SEND_CMD_CHECK(0x00);
    SEND_CMD_CHECK(0x40); // Set Display Start Line
    SEND_CMD_CHECK(0x8D); // Charge Pump
    SEND_CMD_CHECK(0x14); // Enable
    SEND_CMD_CHECK(0x20); // Memory Addressing Mode
    SEND_CMD_CHECK(0x00); // Horizontal
    
    // Column Address for 72x40
    SEND_CMD_CHECK(0x21); // Set Column Address
    SEND_CMD_CHECK(OLED_COLUMN_OFFSET);   // Start
    SEND_CMD_CHECK(OLED_COLUMN_OFFSET + OLED_WIDTH - 1); // End
    
    SEND_CMD_CHECK(0x22); // Set Page Address
    SEND_CMD_CHECK(0);    // Start
    SEND_CMD_CHECK(OLED_PAGES - 1); // End
    
    SEND_CMD_CHECK(0xA1); // Set Segment Re-map
    SEND_CMD_CHECK(0xC8); // Set COM Output Scan Direction
    SEND_CMD_CHECK(0xDA); // Set COM Pins Hardware Configuration
    SEND_CMD_CHECK(0x12);
    
    // Fix for 72x40 specific artifacts: ensure COM configuration is correct
    // Some 72x40 displays need 0x02 or 0x12 depending on the internal wiring
    SEND_CMD_CHECK(0xDA); 
    SEND_CMD_CHECK(0x12); 

    SEND_CMD_CHECK(0x81); // Set Contrast
    SEND_CMD_CHECK(0xFF); // Max Brightness
    SEND_CMD_CHECK(0xD9); // Set Pre-charge Period
    SEND_CMD_CHECK(0xF1);
    SEND_CMD_CHECK(0xDB); // Set VCOMH Deselect Level
    SEND_CMD_CHECK(0x40);
    SEND_CMD_CHECK(0xA4); // Entire Display On (Resume)
    SEND_CMD_CHECK(0xA6); // Normal Display
    SEND_CMD_CHECK(0xAF); // Display On

    oled_force_refresh();
    
    return true;
}

/**
 * @brief Clears the internal display buffer and marks it as dirty.
 */
void oled_clear(void) {
    LOCK_OLED();
    memset(display_buffer, 0, sizeof(display_buffer));
    is_dirty = true;
    dirty_min_page = 0;
    dirty_max_page = OLED_PAGES - 1;
    UNLOCK_OLED();
}

/**
 * @brief Forces a full hardware re-initialization and display buffer push.
 * 
 * Used for recovering from I2C errors or clearing hardware-level artifacts.
 * Performs a deep clear of the entire 128x64 controller RAM.
 */
void oled_force_refresh(void) {
    LOCK_OLED();
    // Hardware reset sequence
    send_cmd(0xAE); // Display Off
    vTaskDelay(pdMS_TO_TICKS(1)); // Short pause
    
    // Re-initialize fundamental hardware settings
    send_cmd(0xD5); // Clock divide
    send_cmd(0x80);
    send_cmd(0xA8); // Multiplex
    send_cmd(0x27); // 40 lines
    send_cmd(0xD3); // Offset
    send_cmd(0x00);
    send_cmd(0x40); // Start line
    
    send_cmd(0x8D); // Charge Pump
    send_cmd(0x14); 
    
    send_cmd(0x20); // Memory Addressing Mode
    send_cmd(0x00); // Horizontal
    
    // 1. CLEAR ENTIRE CONTROLLER RAM (128x64 area) to remove artifacts
    // Set column range to full 128 columns
    send_cmd(0x21); 
    send_cmd(0);
    send_cmd(127);
    // Set page range to full 8 pages
    send_cmd(0x22);
    send_cmd(0);
    send_cmd(7);
    
    uint8_t zero_chunk[65] = {0};
    zero_chunk[0] = 0x40; // Data mode
    for (int i = 0; i < 16; i++) { // 16 * 64 bytes of data = 1024 bytes (128 * 8)
        i2c_master_transmit(dev_handle, zero_chunk, 65, 100);
        vTaskDelay(pdMS_TO_TICKS(1));
    }

    // 2. Reset Column and Page range to our visible 72x40 window
    send_cmd(0x21); // Set Column Address
    send_cmd(OLED_COLUMN_OFFSET);   // Start
    send_cmd(OLED_COLUMN_OFFSET + OLED_WIDTH - 1); // End
    
    send_cmd(0x22); // Set Page Address
    send_cmd(0);    // Start
    send_cmd(OLED_PAGES - 1); // End
    
    send_cmd(0xA1); // Segment re-map
    send_cmd(0xC8); // COM scan direction
    
    send_cmd(0xAF); // Display On
    vTaskDelay(pdMS_TO_TICKS(1)); // Short pause
    
    // Push the actual display buffer
    send_data(display_buffer, sizeof(display_buffer));

    is_dirty = false;
    dirty_min_page = OLED_PAGES - 1;
    dirty_max_page = 0;
    UNLOCK_OLED();
}

/**
 * @brief Updates the physical display with the contents of the buffer.
 * 
 * Optimized to only update the "dirty" page ranges to save I2C bandwidth.
 */
void oled_update(void) {
    LOCK_OLED();
    if (!is_dirty) {
        UNLOCK_OLED();
        return;
    }

    // ALWAYS reset the column range for every update.
    send_cmd(0x21); // Set Column Address
    send_cmd(OLED_COLUMN_OFFSET);   // Start
    send_cmd(OLED_COLUMN_OFFSET + OLED_WIDTH - 1); // End
    
    // Set Page Address range to exactly match the dirty range
    send_cmd(0x22); 
    send_cmd(dirty_min_page);
    send_cmd(dirty_max_page);
    
    int start_index = dirty_min_page * OLED_WIDTH;
    int end_index = (dirty_max_page + 1) * OLED_WIDTH;
    int length = end_index - start_index;

    if (length > 0) {
        send_data(&display_buffer[start_index], length);
    }

    is_dirty = false;
    dirty_min_page = OLED_PAGES - 1;
    dirty_max_page = 0;
    UNLOCK_OLED();
}

/**
 * @brief Internal pixel setter without mutex locking.
 * 
 * Tracks dirty pages for optimized updates.
 * 
 * @param x X coordinate (0 to OLED_WIDTH-1).
 * @param y Y coordinate (0 to OLED_HEIGHT-1).
 * @param on True to set pixel, False to clear.
 */
void oled_set_pixel_internal(int x, int y, bool on) {
    if (x < 0 || x >= OLED_WIDTH || y < 0 || y >= OLED_HEIGHT) return;
    int page = y / 8;
    int bit = y % 8;
    int index = page * OLED_WIDTH + x;
    uint8_t old_val = display_buffer[index];
    
    if (on) display_buffer[index] |= (1 << bit);
    else display_buffer[index] &= ~(1 << bit);
    
    if (display_buffer[index] != old_val) {
        is_dirty = true;
        if (page < dirty_min_page) dirty_min_page = page;
        if (page > dirty_max_page) dirty_max_page = page;
    }
}

/**
 * @brief Set or clear a pixel at (x, y).
 * 
 * @param x X coordinate.
 * @param y Y coordinate.
 * @param on True to set pixel, False to clear.
 */
void oled_set_pixel(int x, int y, bool on) {
    LOCK_OLED();
    oled_set_pixel_internal(x, y, on);
    UNLOCK_OLED();
}

/**
 * @brief Internal raw byte setter for direct buffer manipulation.
 * 
 * @param index Index in the display buffer.
 * @param val Byte value to set.
 */
void oled_set_raw_byte_internal(int index, uint8_t val) {
    if (index < 0 || index >= (OLED_WIDTH * OLED_PAGES)) return;
    if (display_buffer[index] != val) {
        display_buffer[index] = val;
        is_dirty = true;
        int page = index / OLED_WIDTH;
        if (page < dirty_min_page) dirty_min_page = page;
        if (page > dirty_max_page) dirty_max_page = page;
    }
}

/**
 * @brief Set a raw byte in the display buffer.
 * 
 * @param index Index in the buffer.
 * @param val Byte value.
 */
void oled_set_raw_byte(int index, uint8_t val) {
    LOCK_OLED();
    oled_set_raw_byte_internal(index, val);
    UNLOCK_OLED();
}

/**
 * @brief Draws a single character at (x, y).
 * 
 * @param x X coordinate.
 * @param y Y coordinate.
 * @param c Character to draw.
 * @param large True for 2x scaling, False for 1x.
 */
void oled_draw_char(int x, int y, char c, bool large) {
    oled_draw_char_with_effects(x, y, c, large, 0);
}

/**
 * @brief Draws a single character with optional effects.
 * 
 * @param x X coordinate.
 * @param y Y coordinate.
 * @param c Character to draw.
 * @param large True for 2x scaling.
 * @param effect 0: None, 1: Inverse, 2: Underline.
 */
void oled_draw_char_with_effects(int x, int y, char c, bool large, int effect) {
    if (c < 32 || c > 126) c = '?';
    const uint8_t *glyph = font5x7[c - 32];
    
    int w = large ? 10 : 5;
    int h = large ? 14 : 7;

    LOCK_OLED();
    // Boundary check for the character
    if (x >= OLED_WIDTH || y >= OLED_HEIGHT || x + w <= 0 || y + h <= 0) {
        UNLOCK_OLED();
        return;
    }

    // Background for inverse effect
    if (effect == 1) { // Inverse
        for (int col = 0; col < w; col++) {
            for (int row = 0; row < h; row++) {
                oled_set_pixel_internal(x + col, y + row, true);
            }
        }
    }

    for (int col = 0; col < 5; col++) {
        uint8_t line = glyph[col];
        for (int row = 0; row < 7; row++) {
            bool pixel_on = (line & (1 << row));
            
            if (effect == 1) pixel_on = !pixel_on; // Inverse logic

            if (pixel_on) {
                if (large) {
                    oled_set_pixel_internal(x + col * 2, y + row * 2, true);
                    oled_set_pixel_internal(x + col * 2 + 1, y + row * 2, true);
                    oled_set_pixel_internal(x + col * 2, y + row * 2 + 1, true);
                    oled_set_pixel_internal(x + col * 2 + 1, y + row * 2 + 1, true);
                } else {
                    oled_set_pixel_internal(x + col, y + row, true);
                }
            }
        }
    }

    if (effect == 2) { // Underline
        int underline_y = y + h;
        if (underline_y < OLED_HEIGHT) {
            for (int col = 0; col < w; col++) {
                oled_set_pixel_internal(x + col, underline_y, true);
            }
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Draws a string at (x, y).
 * 
 * @param x X coordinate.
 * @param y Y coordinate.
 * @param str String to draw.
 * @param large True for 2x scaling.
 */
void oled_draw_string(int x, int y, const char *str, bool large) {
    oled_draw_string_formatted(x, y, str, large, 0, 0, 0, 0);
}

/**
 * @brief Draws a string with advanced formatting and animations.
 * 
 * Supports text alignment, scrolling, bouncing, flashing, and typewriter effects.
 * 
 * @param x X coordinate (base).
 * @param y Y coordinate (base).
 * @param str String to draw.
 * @param large True for 2x scaling.
 * @param anim_type 0: None, 1: Scrolling, 2: Typewriter, 3: Floating, 4: Flash.
 * @param align 0: Left, 1: Center, 2: Right.
 * @param effect Character effect (see oled_draw_char_with_effects).
 * @param tick Current animation tick for timing.
 */
void oled_draw_string_formatted(int x, int y, const char *str, bool large, int anim_type, int align, int effect, uint32_t tick) {
    if (str == NULL) return;
    LOCK_OLED();
    int spacing = large ? 12 : 6;
    
    // Safety check for long strings to prevent hanging
    char safe_str[128];
    strncpy(safe_str, str, sizeof(safe_str) - 1);
    safe_str[sizeof(safe_str) - 1] = '\0';
    int safe_len = strlen(safe_str);
    
    // Calculate total width for alignment or scrolling
    int total_width = 0;
    const char *p = safe_str;
    while (*p) {
        total_width += spacing;
        p++;
    }

    int start_x = x;
    if (align == 1) { // Center
        start_x = (OLED_WIDTH - total_width) / 2;
    } else if (align == 2) { // Right
        start_x = OLED_WIDTH - total_width;
    }
    
    int cur_x = start_x;
    int cur_y = y;

    if (anim_type == 1) { // Scrolling
        int offset = (tick * 2) % (total_width + OLED_WIDTH);
        cur_x = OLED_WIDTH - offset;
    } else if (anim_type == 3) { // Floating/Bounce
        cur_y += (int)(3 * sinf(tick * 0.2f));
    } else if (anim_type == 4) { // Flash
        if ((tick / 10) % 2 != 0) return;
    }

    int chars_to_show = safe_len;
    if (anim_type == 2) { // Typewriter
        chars_to_show = (tick / 2) % (safe_len + 10);
        if (chars_to_show > safe_len) chars_to_show = safe_len;
    }

    p = safe_str;
    int char_count = 0;
    while (*p && char_count < chars_to_show) {
        if (*p == '\n') {
            cur_x = start_x;
            cur_y += (large ? 16 : 8);
            p++;
            char_count++;
            continue;
        }
        
        if (cur_x + spacing > OLED_WIDTH && anim_type != 1) {
            cur_x = start_x;
            cur_y += (large ? 16 : 8);
        }
        
        if (cur_y + (large ? 14 : 7) > OLED_HEIGHT && anim_type != 1) break;

        oled_draw_char_with_effects(cur_x, cur_y, *p++, large, effect);
        cur_x += spacing;
        char_count++;
    }
    UNLOCK_OLED();
}

/**
 * @brief Internal line drawing function without mutex locking.
 * 
 * Uses Bresenham's algorithm and supports custom line thickness.
 * 
 * @param x0 Start X.
 * @param y0 Start Y.
 * @param x1 End X.
 * @param y1 End Y.
 * @param thickness Line thickness in pixels.
 * @param on True to set pixels, False to clear.
 */
static void oled_draw_line_internal(int x0, int y0, int x1, int y1, float thickness, bool on) {
    int dx = abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
    int dy = -abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
    int err = dx + dy, e2;
    
    while (1) {
        if (thickness <= 1.0f) {
            oled_set_pixel_internal(x0, y0, on);
        } else {
            // Internal brush drawing for thicker lines
            float radius = thickness / 2.0f;
            int ir = (int)(radius + 0.5f);
            for (int i = -ir; i <= ir; i++) {
                for (int j = -ir; j <= ir; j++) {
                    if ((float)(i*i + j*j) <= radius * radius + 0.5f) {
                        oled_set_pixel_internal(x0 + i, y0 + j, on);
                    }
                }
            }
        }
        if (x0 == x1 && y0 == y1) break;
        e2 = 2 * err;
        if (e2 >= dy) { err += dy; x0 += sx; }
        if (e2 <= dx) { err += dx; y0 += sy; }
    }
}

/**
 * @brief Draws a line from (x0, y0) to (x1, y1).
 * 
 * @param x0 Start X.
 * @param y0 Start Y.
 * @param x1 End X.
 * @param y1 End Y.
 * @param thickness Line thickness.
 * @param on True to set pixels, False to clear.
 */
void oled_draw_line(int x0, int y0, int x1, int y1, float thickness, bool on) {
    LOCK_OLED();
    oled_draw_line_internal(x0, y0, x1, y1, thickness, on);
    UNLOCK_OLED();
}

/**
 * @brief Draws a rectangle.
 * 
 * @param x Top-left X.
 * @param y Top-left Y.
 * @param w Width.
 * @param h Height.
 * @param thickness Border thickness.
 * @param on True to set pixels, False to clear.
 */
void oled_draw_rect(int x, int y, int w, int h, float thickness, bool on) {
    if (w <= 0 || h <= 0) return;
    LOCK_OLED();
    oled_draw_line_internal(x, y, x + w - 1, y, thickness, on);
    oled_draw_line_internal(x, y + h - 1, x + w - 1, y + h - 1, thickness, on);
    oled_draw_line_internal(x, y, x, y + h - 1, thickness, on);
    oled_draw_line_internal(x + w - 1, y, x + w - 1, y + h - 1, thickness, on);
    UNLOCK_OLED();
}

/**
 * @brief Draws a circle using the Midpoint Circle Algorithm.
 * 
 * Supports custom border thickness.
 * 
 * @param x Center X.
 * @param y Center Y.
 * @param r Radius.
 * @param thickness Border thickness.
 * @param on True to set pixels, False to clear.
 */
void oled_draw_circle(int x, int y, int r, float thickness, bool on) {
    int dx = -r, dy = 0, err = 2 - 2 * r;
    LOCK_OLED();
    do {
        if (thickness <= 1.0f) {
            oled_set_pixel_internal(x - dx, y + dy, on);
            oled_set_pixel_internal(x + dx, y + dy, on);
            oled_set_pixel_internal(x - dx, y - dy, on);
            oled_set_pixel_internal(x + dx, y - dy, on);
            oled_set_pixel_internal(x - dy, y + dx, on);
            oled_set_pixel_internal(x + dy, y + dx, on);
            oled_set_pixel_internal(x - dy, y - dx, on);
            oled_set_pixel_internal(x + dy, y - dx, on);
        } else {
            float radius = thickness / 2.0f;
            int ir = (int)(radius + 0.5f);
            int pts[8][2] = {
                {x - dx, y + dy}, {x + dx, y + dy}, {x - dx, y - dy}, {x + dx, y - dy},
                {x - dy, y + dx}, {x + dy, y + dx}, {x - dy, y - dx}, {x + dy, y - dx}
            };
            for (int p = 0; p < 8; p++) {
                for (int i = -ir; i <= ir; i++) {
                    for (int j = -ir; j <= ir; j++) {
                        if ((float)(i*i + j*j) <= radius * radius + 0.5f) {
                            oled_set_pixel_internal(pts[p][0] + i, pts[p][1] + j, on);
                        }
                    }
                }
            }
        }
        r = err;
        if (r <= dy) err += ++dy * 2 + 1;
        if (r > dx || err > dy) err += ++dx * 2 + 1;
    } while (dx < 0);
    UNLOCK_OLED();
}

/**
 * @brief Draws a solid circular brush at (x, y).
 * 
 * @param x X coordinate.
 * @param y Y coordinate.
 * @param size Diameter of the brush.
 * @param on True to set pixels, False to clear.
 */
void oled_draw_brush(int x, int y, float size, bool on) {
    LOCK_OLED();
    if (size <= 1.0f) {
        oled_set_pixel_internal(x, y, on);
    } else {
        float radius = size / 2.0f;
        int ir = (int)(radius + 0.5f);
        for (int i = -ir; i <= ir; i++) {
            for (int j = -ir; j <= ir; j++) {
                if ((float)(i*i + j*j) <= radius * radius + 0.5f) {
                    oled_set_pixel_internal(x + i, y + j, on);
                }
            }
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a modern tech-style ready screen.
 * 
 * Shows corner brackets, a "READY" status message, a pulsing "WAITING CMD" 
 * indicator, and animated side bars. Used as the default idle state UI.
 */
void oled_show_ready_screen(void) {
    LOCK_OLED();
    oled_clear();
    
    // Modern tech-style UI
    // Corner brackets
    int s = 6; 
    oled_draw_line(0, 0, s, 0, 1.0f, true);
    oled_draw_line(0, 0, 0, s, 1.0f, true);
    oled_draw_line(OLED_WIDTH-1-s, 0, OLED_WIDTH-1, 0, 1.0f, true);
    oled_draw_line(OLED_WIDTH-1, 0, OLED_WIDTH-1, s, 1.0f, true);
    oled_draw_line(0, OLED_HEIGHT-1, s, OLED_HEIGHT-1, 1.0f, true);
    oled_draw_line(0, OLED_HEIGHT-1-s, 0, OLED_HEIGHT-1, 1.0f, true);
    oled_draw_line(OLED_WIDTH-1-s, OLED_HEIGHT-1, OLED_WIDTH-1, OLED_HEIGHT-1, 1.0f, true);
    oled_draw_line(OLED_WIDTH-1, OLED_HEIGHT-1-s, OLED_WIDTH-1, OLED_HEIGHT-1, 1.0f, true);

    // Center content
    oled_draw_string(10, 8, "READY", true);
    
    // Pulse effect for "WAITING CMD"
    bool pulse = (animation_tick / 20) % 2 == 0;
    const char *msg = pulse ? "WAITING CMD" : " WAITING CMD ";
    int w_x = (OLED_WIDTH - (strlen(msg) * 6)) / 2;
    if (w_x < 0) w_x = 0;
    oled_draw_string(w_x, 26, msg, false);
    
    // Small animated side-bar
    int bar_h = 4;
    int bar_y = (animation_tick % (OLED_HEIGHT - bar_h));
    oled_draw_line(OLED_WIDTH-2, bar_y, OLED_WIDTH-2, bar_y + bar_h, 1.0f, true);
    oled_draw_line(1, bar_y, 1, bar_y + bar_h, 1.0f, true);

    UNLOCK_OLED();
}

/**
 * @brief Displays a tech-style boot animation with orbiting particles.
 */
void oled_show_boot_screen(void) {
    LOCK_OLED();
    oled_clear();
    int cx = 36, cy = 18;
    float time = animation_tick * 0.1f;
    
    for (int a = 0; a < 360; a += 15) {
        float rad = a * (M_PI / 180.0f);
        int x = cx + (int)(18 * cosf(rad) * cosf(time * 0.5f));
        int y = cy + (int)(6 * sinf(rad));
        oled_set_pixel_internal(x, y, true);
    }
    for (int a = 0; a < 360; a += 15) {
        float rad = a * (M_PI / 180.0f);
        int x = cx + (int)(6 * sinf(rad));
        int y = cy + (int)(16 * cosf(rad) * sinf(time * 0.7f));
        oled_set_pixel_internal(x, y, true);
    }
    int core_r = 3 + (int)(2 * sinf(time * 2.0f));
    for (int i = 0; i < 4; i++) {
        float a = time + i * (M_PI / 2);
        oled_draw_line(cx, cy, cx + core_r * cosf(a), cy + core_r * sinf(a), 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a 3D starfield projection animation.
 */
void oled_show_starfield(void) {
    LOCK_OLED();
    oled_clear();
    static struct { float x, y, z; } stars[20];
    static bool stars_init = false;
    if (!stars_init) {
        for (int i = 0; i < 20; i++) {
            stars[i].x = (rand() % 100) - 50;
            stars[i].y = (rand() % 100) - 50;
            stars[i].z = (rand() % 100) + 1;
        }
        stars_init = true;
    }
    for (int i = 0; i < 20; i++) {
        stars[i].z -= 1.5f;
        if (stars[i].z <= 0) {
            stars[i].x = (rand() % 100) - 50;
            stars[i].y = (rand() % 100) - 50;
            stars[i].z = 100;
        }
        int px = (OLED_WIDTH / 2) + (int)(stars[i].x * 50 / stars[i].z);
        int py = (OLED_HEIGHT / 2) + (int)(stars[i].y * 50 / stars[i].z);
        if (px >= 0 && px < OLED_WIDTH && py >= 0 && py < OLED_HEIGHT) {
            oled_set_pixel_internal(px, py, true);
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays an animated sine wave effect.
 */
void oled_show_waves(void) {
    LOCK_OLED();
    oled_clear();
    for (int x = 0; x < OLED_WIDTH; x++) {
        float phase = animation_tick * 0.1f;
        int y1 = 20 + (int)(8 * sinf(x * 0.15f + phase));
        int y2 = 20 + (int)(6 * sinf(x * 0.1f - phase * 0.8f));
        oled_set_pixel_internal(x, y1, true);
        oled_set_pixel_internal(x, y2, true);
        if (x % 2 == 0) oled_draw_line(x, y1, x, y2, 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a bar-style audio visualizer animation.
 * 
 * @param mode 0: Random, 1: Wave, 2: Peak meter.
 */
void oled_show_visualizer(int mode) {
    LOCK_OLED();
    oled_clear();
    static int heights[12];
    int bars = 12;
    int bar_w = 4;
    int spacing = 2;

    for (int i = 0; i < bars; i++) {
        int target;
        if (mode == 0) { // Random/Normal
            target = rand() % 30;
        } else if (mode == 1) { // Wave form
            target = 15 + (int)(15 * sinf(i * 0.5f + animation_tick * 0.2f));
        } else { // Peak meter (mode 2)
            target = (animation_tick + i * 5) % 35;
            if (target > 30) target = 30;
        }

        if (heights[i] < target) heights[i] += 2;
        else if (heights[i] > target) heights[i] -= 1;
        
        int x = i * (bar_w + spacing);
        if (mode == 2) { // Centered
             oled_draw_rect(x, (OLED_HEIGHT - heights[i])/2, bar_w, heights[i], 1, true);
        } else {
             oled_draw_rect(x, OLED_HEIGHT - heights[i], bar_w, heights[i], 1, true);
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a watch face (analog or digital).
 * 
 * Supports multiple styles: Minimal Analog, Cyberpunk, Big Hour, Modernist.
 * 
 * @param index Watch face style index.
 * @param h Hours.
 * @param m Minutes.
 * @param s Seconds.
 * @param tick Current system tick.
 */
void oled_show_clock(int index, int h, int m, int s, uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    int cx = OLED_WIDTH / 2;
    int cy = OLED_HEIGHT / 2;

    int h_disp = h % 12;
    if (h_disp == 0) h_disp = 12;
    
    // Remaining Unique Watch Faces
    switch(index) {
        case 1: // Minimal Analog - Fixed Edges
        {
            // Hour Indicators on Edges
            for (int i = 0; i < 12; i++) {
                float a = (i * M_PI / 6.0f) - M_PI / 2.0f;
                float dx = cosf(a);
                float dy = sinf(a);
                
                float x_edge, y_edge;
                // Project to rectangle edges (35.5, 19.5 half-extents)
                if (fabsf(dx * 19.5f) > fabsf(dy * 35.5f)) {
                    // Hits left or right
                    x_edge = (dx > 0) ? 71.0f : 0.0f;
                    y_edge = 19.5f + (dy * ((dx > 0) ? 35.5f : -35.5f) / dx);
                } else {
                    // Hits top or bottom
                    y_edge = (dy > 0) ? 39.0f : 0.0f;
                    x_edge = 35.5f + (dx * ((dy > 0) ? 19.5f : -19.5f) / dy);
                }
                
                // Gap of 2 pixels, length of 6 pixels
                int ix_start = (int)(x_edge - 2 * dx);
                int iy_start = (int)(y_edge - 2 * dy);
                int ix_end = (int)(x_edge - 8 * dx);
                int iy_end = (int)(y_edge - 8 * dy);
                oled_draw_line(ix_start, iy_start, ix_end, iy_end, 1, true);
            }

            // Hands - Long and Hour is Bold
            float ma = (m * M_PI / 30.0f) - M_PI / 2.0f;
            float ha = ((h % 12) * M_PI / 6.0f) + (m * M_PI / 360.0f) - M_PI / 2.0f;
            
            // Minute hand (Long)
            oled_draw_line(cx, cy, cx + (int)(18 * cosf(ma)), cy + (int)(18 * sinf(ma)), 1, true);
            
            // Hour hand (Bold and Long-ish) - Triple draw for bold
            oled_draw_line(cx, cy, cx + (int)(12 * cosf(ha)), cy + (int)(12 * sinf(ha)), 1, true);
            oled_draw_line(cx + 1, cy, cx + 1 + (int)(12 * cosf(ha)), cy + (int)(12 * sinf(ha)), 1, true);
            oled_draw_line(cx, cy + 1, cx + (int)(12 * cosf(ha)), cy + 1 + (int)(12 * sinf(ha)), 1, true);

            // Seconds Filling/Disappearing Border Trail (2-minute cycle)
            int perimeter = 220; 
            int totalLen;
            if (m % 2 == 0) {
                totalLen = (s * perimeter) / 60;
            } else {
                totalLen = ((60 - s) * perimeter) / 60;
            }

            int currentLen = 0;
            // Top
            if (currentLen < totalLen) {
                int len = (totalLen - currentLen > 71) ? 71 : (totalLen - currentLen);
                oled_draw_line(0, 0, len, 0, 1, true);
                currentLen += 71;
            }
            // Right
            if (currentLen < totalLen) {
                int len = (totalLen - currentLen > 39) ? 39 : (totalLen - currentLen);
                oled_draw_line(71, 0, 71, len, 1, true);
                currentLen += 39;
            }
            // Bottom
            if (currentLen < totalLen) {
                int len = (totalLen - currentLen > 71) ? 71 : (totalLen - currentLen);
                oled_draw_line(71, 39, 71 - len, 39, 1, true);
                currentLen += 71;
            }
            // Left
            if (currentLen < totalLen) {
                int len = (totalLen - currentLen > 39) ? 39 : (totalLen - currentLen);
                oled_draw_line(0, 39, 0, 39 - len, 1, true);
            }
            break;
        }
        case 2: // Cyberpunk / Tech - Fixed Formatting
        {
            char buf[16];
            // Border on outer pixels
            oled_draw_rect(0, 0, 72, 40, 1, true);
            oled_draw_line(0, 24, 71, 24, 1, true);
            
            // Tight spacing for time
            snprintf(buf, sizeof(buf), "%d", h_disp);
            oled_draw_string(8, 5, buf, true);
            oled_draw_string(30, 5, ":", true);
            snprintf(buf, sizeof(buf), "%d", m);
            oled_draw_string(42, 5, buf, true);
            
            snprintf(buf, sizeof(buf), "SEC %02d", s);
            oled_draw_string(35, 26, buf, false);
            
            // Glitch effect
            if (tick % 20 < 2) {
                int ry = rand() % 40;
                oled_draw_line(0, ry, 71, ry, 1, true);
            }
            break;
        }
        case 11: // Big Hour - Bold
        {
            char buf[16];
            snprintf(buf, sizeof(buf), "%d", h_disp);
            // Bold effect
            oled_draw_string(5, 5, buf, true);
            oled_draw_string(6, 5, buf, true);
            oled_draw_string(5, 6, buf, true);
            
            snprintf(buf, sizeof(buf), "%02d", m);
            oled_draw_string(45, 22, buf, true);
            
            // Diagonal line
            oled_draw_line(0, 39, 71, 0, 1, true);
            break;
        }
        case 12: // Modernist - Dynamic Sizing
        {
            char buf[16];
            snprintf(buf, sizeof(buf), "%d:%02d", h_disp, m);
            // Dynamic centering
            int len = strlen(buf);
            int spacing = 12;
            int total_w = len * spacing;
            int x_off = (72 - total_w) / 2;
            
            // Draw bold (offset by 1)
            oled_draw_string(x_off, 12, buf, true);
            oled_draw_string(x_off + 1, 12, buf, true);
            break;
        }
    }
    UNLOCK_OLED();
}

// --- New 10 Beautiful Animations ---

/**
 * @brief Displays a DNA double-helix animation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_dna(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    for (int y = 0; y < OLED_HEIGHT; y += 2) {
        float angle = y * 0.2f + tick * 0.15f;
        int x1 = (OLED_WIDTH / 2) + (int)(15 * sinf(angle));
        int x2 = (OLED_WIDTH / 2) - (int)(15 * sinf(angle));
        oled_set_pixel_internal(x1, y, true);
        oled_set_pixel_internal(x2, y, true);
        if (y % 8 == 0) oled_draw_line(x1, y, x2, y, 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a fireworks explosion animation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_fireworks(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    static float fx, fy, fv;
    static int phase = 0;
    if (phase == 0) {
        fx = rand() % OLED_WIDTH; fy = OLED_HEIGHT; fv = 2.0f + (rand() % 10) / 5.0f; phase = 1;
    }
    if (phase == 1) {
        fy -= fv; oled_set_pixel_internal(fx, fy, true);
        if (fy < 10 + rand() % 10) phase = 2;
    } else {
        for (int i = 0; i < 12; i++) {
            float a = i * M_PI / 6 + tick * 0.1f;
            float r = (tick % 20) * 1.5f;
            oled_set_pixel_internal(fx + r * cosf(a), fy + r * sinf(a), true);
        }
        if (tick % 20 == 0) phase = 0;
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a zooming tunnel animation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_tunnel(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    int cx = OLED_WIDTH / 2, cy = OLED_HEIGHT / 2;
    for (int i = 0; i < 4; i++) {
        int r = (tick + i * 15) % 60;
        if (r < 40) oled_draw_circle(cx, cy, r, 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a falling snow animation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_snow(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    static int flakes[15][2];
    static bool init = false;
    if (!init) {
        for (int i = 0; i < 15; i++) { flakes[i][0] = rand() % OLED_WIDTH; flakes[i][1] = rand() % OLED_HEIGHT; }
        init = true;
    }
    for (int i = 0; i < 15; i++) {
        oled_set_pixel_internal(flakes[i][0], flakes[i][1], true);
        if (tick % 2 == 0) {
            flakes[i][1]++;
            flakes[i][0] += (rand() % 3 - 1);
            if (flakes[i][1] >= OLED_HEIGHT) { flakes[i][1] = 0; flakes[i][0] = rand() % OLED_WIDTH; }
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a spiral vortex animation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_vortex(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    int cx = OLED_WIDTH / 2, cy = OLED_HEIGHT / 2;
    for (int i = 0; i < 20; i++) {
        float a = i * 0.5f + tick * 0.2f;
        float r = i * 1.5f;
        oled_set_pixel_internal(cx + r * cosf(a), cy + r * sinf(a), true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a rotating 3D wireframe cube.
 * 
 * @param tick Animation tick.
 */
void oled_anim_cube(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    int cx = OLED_WIDTH / 2, cy = OLED_HEIGHT / 2;
    float a = tick * 0.05f;
    int sz = 12;
    int x[8], y[8];
    float pts[8][3] = {{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    for(int i=0; i<8; i++) {
        float px = pts[i][0], py = pts[i][1], pz = pts[i][2];
        float nx = px*cosf(a) - pz*sinf(a);
        float nz = px*sinf(a) + pz*cosf(a);
        float ny = py*cosf(a*0.7f) - nz*sinf(a*0.7f);
        x[i] = cx + (int)(nx * sz); y[i] = cy + (int)(ny * sz);
    }
    for(int i=0; i<4; i++) {
        oled_draw_line(x[i], y[i], x[(i+1)%4], y[(i+1)%4], 1, true);
        oled_draw_line(x[i+4], y[i+4], x[((i+1)%4)+4], y[((i+1)%4)+4], 1, true);
        oled_draw_line(x[i], y[i], x[i+4], y[i+4], 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays concentric pulsating rings.
 * 
 * @param tick Animation tick.
 */
void oled_anim_rings(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    int cx = OLED_WIDTH/2, cy = OLED_HEIGHT/2;
    for(int i=0; i<3; i++) {
        int r = 10 + i*8 + (int)(3*sinf(tick*0.1f + i));
        oled_draw_circle(cx, cy, r, 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a "glitch" effect with flickering rectangles and text.
 * 
 * @param tick Animation tick.
 */
void oled_anim_glitch(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    if (tick % 5 == 0) {
        for(int i=0; i<5; i++) oled_draw_rect(rand()%OLED_WIDTH, rand()%OLED_HEIGHT, rand()%20, 2, 1, true);
    } else {
        oled_draw_string(15, 12, "ERROR", true);
        if (tick % 2 == 0) oled_draw_line(0, rand()%OLED_HEIGHT, OLED_WIDTH, rand()%OLED_HEIGHT, 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a Matrix-style falling rain animation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_rain(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    static int drops[12];
    static bool init = false;
    if (!init) { for(int i=0; i<12; i++) drops[i] = rand() % 40; init = true; }
    for (int i = 0; i < 12; i++) {
        int x = i * 6 + 2;
        oled_draw_line(x, drops[i], x, drops[i] + 4, 1, true);
        if (tick % 2 == 0) {
            drops[i] = (drops[i] + 2) % 40;
            if (drops[i] == 0) x = i * 6 + 2;
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a plasma-like interference pattern.
 * 
 * @param tick Animation tick.
 */
void oled_anim_plasma(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    for(int x=0; x<72; x+=2) {
        for(int y=0; y<40; y+=2) {
            float v = sinf(x*0.1f + tick*0.1f) + sinf(y*0.1f + tick*0.1f) + sinf((x+y)*0.1f + tick*0.1f);
            if (v > 1.0f) oled_set_pixel_internal(x, y, true);
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays expanding concentric circles.
 * 
 * @param tick Animation tick.
 */
void oled_anim_circles(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    for(int i=0; i<4; i++) {
        int r = (tick + i*10) % 40;
        oled_draw_circle(36, 20, r, 1, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays random pixel noise.
 * 
 * @param tick Animation tick.
 */
void oled_anim_noise(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    for(int i=0; i<100; i++) oled_set_pixel_internal(rand()%72, rand()%40, true);
    UNLOCK_OLED();
}

/**
 * @brief Displays a swarm of particles following a target.
 * 
 * @param tick Animation tick.
 */
void oled_anim_swarm(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    static float px[10], py[10];
    static bool init = false;
    if (!init) { for(int i=0; i<10; i++) { px[i]=rand()%72; py[i]=rand()%40; } init = true; }
    float tx = 36 + 20*cosf(tick*0.05f), ty = 20 + 15*sinf(tick*0.07f);
    oled_set_pixel_internal(tx, ty, true);
    for(int i=0; i<10; i++) {
        px[i] += (tx - px[i]) * 0.1f + (rand()%3-1);
        py[i] += (ty - py[i]) * 0.1f + (rand()%3-1);
        oled_set_pixel_internal(px[i], py[i], true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a complex wave animation with modulation.
 * 
 * @param tick Animation tick.
 */
void oled_anim_wave2(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    for(int x=0; x<72; x++) {
        int y = 20 + 10 * sinf(x*0.1f + tick*0.2f) * cosf(tick*0.05f);
        oled_set_pixel_internal(x, y, true);
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays animated metaballs effect.
 * 
 * @param tick Animation tick.
 */
void oled_anim_metaballs(uint32_t tick) {
    LOCK_OLED();
    oled_clear();
    float x1 = 36 + 15*cosf(tick*0.1f), y1 = 20 + 10*sinf(tick*0.15f);
    float x2 = 36 + 15*sinf(tick*0.08f), y2 = 20 + 10*cosf(tick*0.12f);
    for(int x=0; x<72; x+=3) {
        for(int y=0; y<40; y+=3) {
            float d1 = (x-x1)*(x-x1) + (y-y1)*(y-y1);
            float d2 = (x-x2)*(x-x2) + (y-y2)*(y-y2);
            if (100.0f/d1 + 100.0f/d2 > 0.5f) oled_draw_rect(x, y, 2, 2, 1, true);
        }
    }
    UNLOCK_OLED();
}


// --- Advanced Mode / Security Tools UI ---

static char current_ap_ssid[64] = "";
static bool scroll_finished = false;
static uint32_t last_ap_switch_tick = 0;

/**
 * @brief Draws a string with horizontal scrolling if it exceeds display width.
 * 
 * Automatically centers strings that fit within the display. If the string is wider, 
 * it scrolls from right to left.
 * 
 * @param y Vertical position.
 * @param str String to draw.
 * @param large True for 2x scaling.
 * @param offset Pointer to scroll offset state (incremented internally).
 * @return true if scrolling has reached the end or the string fits perfectly.
 */
bool oled_draw_string_scrolling(int y, const char *str, bool large, int *offset) {
    int char_width = large ? 12 : 6;
    int str_len = strlen(str);
    int total_width = str_len * char_width;
    
    if (total_width <= OLED_WIDTH) {
        oled_draw_string((OLED_WIDTH - total_width) / 2, y, str, large);
        return true; 
    }
    
    // Scroll logic: Stop when last character is visible
    int max_scroll = total_width - OLED_WIDTH;
    int current_scroll = (*offset / 2);
    
    if (current_scroll >= max_scroll) {
        current_scroll = max_scroll;
        oled_draw_string(-current_scroll, y, str, large);
        return true; // Return true and don't reset offset to stop scrolling
    }
    
    oled_draw_string(-current_scroll, y, str, large);
    (*offset)++;
    return false;
}

/**
 * @brief Displays a radar-style scanning animation.
 * 
 * Shows a central radar sweep with pulsing dots and the current channel.
 * 
 * @param channel The current Wi-Fi channel being scanned.
 */
void oled_show_scanning(uint8_t channel) {
    LOCK_OLED();
    oled_clear();
    
    // Channel indicator as dot pattern
    char ch_buf[4];
    snprintf(ch_buf, sizeof(ch_buf), "%d", channel);
    
    // Tech-style scanning UI
    // Central reticle
    int cx = 36, cy = 18;
    oled_draw_circle(cx, cy, 14, 1.0f, true);
    oled_draw_circle(cx, cy, 4, 1.0f, true);
    
    // Rotating sweep
    float a = animation_tick * 0.2f;
    int lx = cx + (int)(14 * cosf(a));
    int ly = cy + (int)(14 * sinf(a));
    oled_draw_line(cx, cy, lx, ly, 1.0f, true);
    
    // Channel number large
    oled_draw_string(60, 10, "CH", false);
    oled_draw_string(80, 5, ch_buf, true);
    
    // Bottom status text
    oled_draw_string(5, 36, "SCANNING...", false);
    
    // Decorative bars
    int bar_h = (animation_tick % 20);
    oled_draw_rect(120, 40 - bar_h, 4, bar_h, 1.0f, true);
    UNLOCK_OLED();
}

/**
 * @brief Draws a single large digit with a vertical transition effect.
 * 
 * Used for the attack countdown timer. Scales standard 5x7 font to 15x28.
 * 
 * @param x X coordinate.
 * @param y Y coordinate.
 * @param digit Character to draw (must be a digit or 'I', 'N', 'F').
 * @param transition Progress of the transition (0.0 to 1.0).
 */
void oled_draw_large_digit(int x, int y, char digit, float transition) {
    if (digit < 32) return;
    const uint8_t *glyph = font5x7[digit - 32];
    
    // Scale 5x7 to roughly 15x28 for 3-digit support
    int scale_x = 3;
    int scale_y = 4;
    
    for (int col = 0; col < 5; col++) {
        uint8_t line = glyph[col];
        for (int row = 0; row < 7; row++) {
            if (line & (1 << row)) {
                // Apply vertical transition offset
                int py = y + row * scale_y + (int)((1.0f - transition) * 12.0f);
                for (int sx = 0; sx < scale_x; sx++) {
                    for (int sy = 0; sy < scale_y; sy++) {
                        // Fade effect during transition
                        if (transition > 0.8f || (rand() % 100 < transition * 100)) {
                            oled_set_pixel_internal(x + col * scale_x + sx, py + sy, true);
                        }
                    }
                }
            }
        }
    }
}

/**
 * @brief Displays the status of an ongoing attack.
 * 
 * Supports different UI layouts for Deauth, Beacon Swarm, and standard attacks.
 * Includes animated transmission waves for Beacon Swarm and large countdown 
 * digits for standard attacks.
 * 
 * @param attack_type String describing the attack (e.g., "DEAUTH", "BEACON").
 * @param target_ssid SSID of the target network or "SWARM".
 * @param status Current status message.
 * @param progress Attack progress (0-100).
 * @param remaining_sec Seconds remaining in the attack (-1 for infinite).
 */
void oled_show_attack_status(const char *attack_type, const char *target_ssid, const char *status, uint8_t progress, int remaining_sec) {
    LOCK_OLED();
    oled_clear();
    
    bool is_deauth = (attack_type && (strstr(attack_type, "DEAUTH") || strstr(attack_type, "DOS")));
    bool is_swarm = (target_ssid && strcmp(target_ssid, "SWARM") == 0);
    
    if (is_swarm) {
        // --- Modern Beacon Swarm UI ---
        // Settings Button Icon at top-left (Gear style)
        oled_draw_circle(4, 4, 3, 1.0f, true);
        oled_set_pixel_internal(4, 4, true);
        oled_set_pixel_internal(4, 2, true);
        oled_set_pixel_internal(4, 6, true);
        oled_set_pixel_internal(2, 4, true);
        oled_set_pixel_internal(6, 4, true);

        oled_draw_string(14, 0, "BEACON SWARM", false);
        oled_draw_line(0, 8, OLED_WIDTH, 8, 1.0f, true);

        // Visual feedback: Transmitting antenna
        int ax = OLED_WIDTH / 2;
        int ay = 25;
        oled_draw_line(ax, ay, ax, ay + 8, 1.0f, true); // Mast
        oled_draw_circle(ax, ay, 2, 1.0f, true);       // Top

        // Animated waves
        int wave = (animation_tick / 4) % 3;
        for (int i = 0; i <= wave; i++) {
            int r = 6 + i * 5;
            // Draw arcs manually since we don't have an arc function
            // We'll just draw small circles or lines
            oled_draw_circle(ax, ay, r, 0.5f, true);
        }

        oled_draw_string(10, 32, "FLOODING...", false);

    } else if (is_deauth) {
        // --- Simplified & Clean Deauth UI ---
        // Header (centered and subtle)
        oled_draw_string(2, 0, "DEAUTH ATTACK", false);
        oled_draw_line(0, 8, OLED_WIDTH, 8, 1.0f, true);
        
        // Target SSID prominently in the center
        int ssid_len = strlen(target_ssid);
        if (ssid_len > 0) {
            int x = (OLED_WIDTH - (ssid_len * 6)) / 2;
            if (x < 0) x = 0;
            oled_draw_string(x, 14, target_ssid, false);
        }
        
        // Status indicator
        bool blink = (animation_tick / 10) % 2 == 0;
        if (blink) {
            oled_draw_string(10, 26, "SENDING PKTS", false);
        }
        
        // No progress bar for deauth as it's infinite/app-controlled
        
    } else if (strncmp(attack_type, "HS:", 3) == 0) {
        // --- Handshake Attack UI (Big & Bold) ---
        uint8_t mask = (uint8_t)atoi(attack_type + 3);
        
        // 1. Target SSID (Top, Large)
        int ssid_len = strlen(target_ssid);
        int x_ssid = (OLED_WIDTH - (ssid_len * 12)) / 2;
        if (x_ssid < 0) x_ssid = 0;
        oled_draw_string(x_ssid, 0, target_ssid, true);
        
        oled_draw_line(0, 18, OLED_WIDTH, 18, 1.0f, true);

        // 2. Handshake Status (Middle, Large)
        // Format: "1 2 3 4"
        char hs_buf[16] = "";
        char *p = hs_buf;
        // M1
        if (mask & 1) { *p++ = '1'; *p++ = ' '; } else { *p++ = '-'; *p++ = ' '; }
        // M2
        if (mask & 2) { *p++ = '2'; *p++ = ' '; } else { *p++ = '-'; *p++ = ' '; }
        // M3
        if (mask & 4) { *p++ = '3'; *p++ = ' '; } else { *p++ = '-'; *p++ = ' '; }
        // M4
        if (mask & 8) { *p++ = '4'; } else { *p++ = '-'; }
        *p = 0;
        
        // Center "1 2 3 4" -> 7 chars * 12px = 84px. (128-84)/2 = 22.
        oled_draw_string(22, 24, hs_buf, true);
        
        // 3. Footer (Progress/Timer)
        int bar_y = 50;
        int bar_width = (OLED_WIDTH * progress) / 100;
        // Draw simple filled bar
        for(int i=0; i<bar_width; i++) {
             oled_draw_line(i, bar_y, i, bar_y+3, 1.0f, true);
        }
        
        // Timer below
        char time_buf[16];
        if (remaining_sec >= 0) snprintf(time_buf, sizeof(time_buf), "%ds", remaining_sec);
        else snprintf(time_buf, sizeof(time_buf), "INF");
        
        int t_len = strlen(time_buf);
        oled_draw_string((OLED_WIDTH - t_len*6)/2, 56, time_buf, false);

    } else {
        // --- Standard Attack UI (Countdown) ---
        // Top section: Alternating Connected network name and Attack Type every 5 seconds (150 ticks at 30fps)
        if ((animation_tick / 150) % 2 == 0) {
            oled_draw_string(0, 0, target_ssid, false);
        } else {
            oled_draw_string(0, 0, attack_type, false);
        }
        oled_draw_line(0, 8, OLED_WIDTH, 8, 1.0f, true);
        
        // Center section: Large 3-digit countdown timer (000-999)
        static int last_sec = -1;
        static float digit_transition = 1.0f;
        
        if (remaining_sec != last_sec) {
            last_sec = remaining_sec;
            digit_transition = 0.0f; 
        }
        
        if (digit_transition < 1.0f) {
            digit_transition += 0.15f; 
        }
        
        char time_buf[12];
        int display_sec = remaining_sec;
        bool is_infinite = (remaining_sec < 0);
        
        if (is_infinite) {
            snprintf(time_buf, sizeof(time_buf), "INF");
        } else {
            if (display_sec > 999) display_sec = 999;
            if (display_sec < 0) display_sec = 0;
            
            if (display_sec >= 100) {
                snprintf(time_buf, sizeof(time_buf), "%03d", display_sec);
            } else if (display_sec >= 10) {
                snprintf(time_buf, sizeof(time_buf), " %02d", display_sec);
            } else {
                snprintf(time_buf, sizeof(time_buf), "  %d", display_sec);
            }
        }
        
        // Position digits to fit 3 in 72px width
        int start_x = 10;
        int spacing = 18; 
        
        if (is_infinite) {
            oled_draw_large_digit(start_x, 10, 'I', digit_transition);
            oled_draw_large_digit(start_x + spacing, 10, 'N', digit_transition);
            oled_draw_large_digit(start_x + spacing * 2, 10, 'F', digit_transition);
        } else {
            if (time_buf[0] != ' ') oled_draw_large_digit(start_x, 10, time_buf[0], digit_transition);
            if (time_buf[1] != ' ') oled_draw_large_digit(start_x + spacing, 10, time_buf[1], digit_transition);
            if (time_buf[2] != ' ') oled_draw_large_digit(start_x + spacing * 2, 10, time_buf[2], digit_transition);
        }
        
        // Bottom section: Progress bar
        int bar_y = 38;
        int bar_width = (OLED_WIDTH * progress) / 100;
        for (int i = 0; i < bar_width; i++) {
            oled_set_pixel_internal(i, bar_y, true);
            oled_set_pixel_internal(i, bar_y + 1, true);
        }
    }
    UNLOCK_OLED();
}

/**
 * @brief Displays a list of detected Access Points.
 * 
 * Shows the SSID with horizontal scrolling, RSSI, and security status.
 * Includes decorative header and selection indicators.
 * 
 * @param ssid SSID of the AP.
 * @param rssi Signal strength in dBm.
 * @param secure True if the network is encrypted.
 * @param index Current selection index.
 * @param total Total number of APs in the list.
 */
void oled_show_ap_list(const char *ssid, int rssi, bool secure, int index, int total) {
    LOCK_OLED();
    static int ap_ssid_offset = 0;
    
    // Check if we need to switch SSID
    if (strcmp(ssid, current_ap_ssid) != 0) {
        // oled_start_transition(); // Removed to simplify
        strncpy(current_ap_ssid, ssid, sizeof(current_ap_ssid));
        ap_ssid_offset = 0;
        scroll_finished = false;
        last_ap_switch_tick = animation_tick;
    }

    oled_clear();
    
    // 1. Improved Top Header
    // Decorative side bars
    oled_draw_line(0, 0, 10, 0, 1.0f, true);
    oled_draw_line(OLED_WIDTH - 11, 0, OLED_WIDTH - 1, 0, 1.0f, true);
    oled_draw_line(0, 0, 0, 3, 1.0f, true);
    oled_draw_line(OLED_WIDTH - 1, 0, OLED_WIDTH - 1, 3, 1.0f, true);
    
    char buf[32];
    snprintf(buf, sizeof(buf), "%d/%d", index + 1, total);
    // Center the count
    int count_x = (OLED_WIDTH - (strlen(buf) * 6)) / 2;
    oled_draw_string(count_x, 0, buf, false);
    
    // Header separator line
    oled_draw_line(0, 9, OLED_WIDTH, 9, 1.0f, true);
    
    // 2. Enlarged SSID Display
    // Ensure "Big & Bold" look by forcing large font and maximizing width usage
    
    // Unlock momentarily if we need to call other locking functions to avoid deadlock
    // assuming non-recursive mutex.
    UNLOCK_OLED(); 
    
    int y_pos = 20;
    // Use large font (true) which is approx 12px wide per char
    
    // Let's use a direct sliding implementation for the SSID to ensure it fills the screen nicely.
    int char_w = 12; // Large font width
    int len = strlen(current_ap_ssid);
    int total_w = len * char_w;
    
    if (total_w <= OLED_WIDTH) {
        // Center it if it fits
        int x = (OLED_WIDTH - total_w) / 2;
        oled_draw_string_formatted(x, y_pos, current_ap_ssid, true, 0, 0, 0, 0);
        scroll_finished = true;
    } else {
        // Scroll it
        // Speed: 1 pixel per tick?
        int scroll_range = total_w - OLED_WIDTH + 24; // Add some padding at end
        int current_scroll = (ap_ssid_offset / 2); // Slow down
        
        if (current_scroll > scroll_range) {
             current_scroll = scroll_range;
             scroll_finished = true;
        } else {
             ap_ssid_offset++;
             scroll_finished = false;
        }
        
        // Draw visible portion
        oled_draw_string_formatted(-current_scroll, y_pos, current_ap_ssid, true, 0, 0, 0, 0);
    }
    
    LOCK_OLED(); // Re-lock for the rest of the drawing
    
    // 3. Selection indicators
    oled_draw_line(0, 10, 0, 39, 1.0f, true);
    oled_draw_line(OLED_WIDTH-1, 10, OLED_WIDTH-1, 39, 1.0f, true);
    UNLOCK_OLED();
}

/**
 * @brief Displays the final result of an attack.
 * 
 * Shows a "SUCCESS" or "FAILED" message along with an animated checkmark or 'X'.
 * Supports special "COMPLETED" status for DoS/Deauth attacks.
 * 
 * @param success True if the attack goal was achieved.
 * @param message Detail message to display (scrolled).
 * @param attack_type Type of attack performed.
 */
void oled_show_result(bool success, const char *message, const char *attack_type) {
    LOCK_OLED();
    oled_clear();
    
    bool is_dos = (attack_type && (strstr(attack_type, "DOS") || strstr(attack_type, "Deauth")));
    
    if (success) {
        if (is_dos) {
            oled_draw_string(5, 5, "COMPLETED", true);
        } else {
            oled_draw_string(12, 5, "SUCCESS", true);
        }
        // Animated checkmark
        oled_draw_line(30, 25, 35, 30, 1.0f, true);
        oled_draw_line(35, 30, 45, 20, 1.0f, true);
    } else {
        if (is_dos) {
            oled_draw_string(5, 5, "COMPLETED", true); // DEAUTH always shows COMPLETED
        } else {
            oled_draw_string(15, 5, "FAILED", true);
        }
        // Animated X
        oled_draw_line(30, 20, 42, 32, 1.0f, true);
        oled_draw_line(42, 20, 30, 32, 1.0f, true);
    }
    
    static int msg_offset = 0;
    oled_draw_string_scrolling(34, message, false, &msg_offset);
    UNLOCK_OLED();
}

/**
 * @brief Displays a countdown timer with a circular progress bar.
 * 
 * Used for system timeouts or delay periods.
 * 
 * @param seconds_remaining Seconds left until timeout.
 * @param total_seconds Initial duration for the circular bar calculation.
 */
void oled_show_countdown(int seconds_remaining, int total_seconds) {
    LOCK_OLED();
    oled_clear();
    oled_draw_string(10, 2, "TIMEOUT", false);
    char buf[16];
    snprintf(buf, sizeof(buf), "%02d", seconds_remaining);
    oled_draw_string(24, 12, buf, true);
    
    // Circular countdown bar
    float angle = (2.0f * M_PI * seconds_remaining) / total_seconds;
    int cx = 36, cy = 30, r = 8;
    for (float a = 0; a < angle; a += 0.1f) {
        oled_set_pixel_internal(cx + (int)(r * cosf(a - M_PI/2)), cy + (int)(r * sinf(a - M_PI/2)), true);
    }
    UNLOCK_OLED();
}

