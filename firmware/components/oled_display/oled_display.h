#ifndef OLED_DISPLAY_H
#define OLED_DISPLAY_H

#include <stdint.h>
#include <stdbool.h>
#include "esp_err.h"
#include "driver/gpio.h"

// Display dimensions
#define OLED_WIDTH  72
#define OLED_HEIGHT 40
#define OLED_PAGES  (OLED_HEIGHT / 8)
#define OLED_COLUMN_OFFSET 28 // Offset for 72x40 display centered in 128-column RAM

// I2C Configuration
#define OLED_I2C_ADDRESS 0x3C
#define I2C_MASTER_NUM I2C_NUM_0
#define I2C_MASTER_FREQ_HZ 400000

// Initialization
extern uint32_t animation_tick;

bool oled_init(gpio_num_t sda_pin, gpio_num_t scl_pin);

// Core Functions
void oled_lock(void);
void oled_unlock(void);
void oled_clear(void);
void oled_update(void);
void oled_force_refresh(void);
void oled_set_pixel(int x, int y, bool on);
void oled_set_pixel_internal(int x, int y, bool on);
void oled_set_raw_byte(int index, uint8_t val);
void oled_set_raw_byte_internal(int index, uint8_t val);

// Text Functions
void oled_draw_char(int x, int y, char c, bool large);
void oled_draw_char_with_effects(int x, int y, char c, bool large, int effect);
void oled_draw_string(int x, int y, const char *str, bool large);
void oled_draw_string_formatted(int x, int y, const char *str, bool large, int anim_type, int align, int effect, uint32_t tick);

// Drawing Functions
void oled_draw_line(int x0, int y0, int x1, int y1, float thickness, bool on);
void oled_draw_rect(int x, int y, int w, int h, float thickness, bool on);
void oled_draw_circle(int x, int y, int r, float thickness, bool on);
void oled_draw_brush(int x, int y, float size, bool on);

// Animation Screens
void oled_show_boot_screen(void);
void oled_show_starfield(void);
void oled_show_waves(void);
void oled_show_visualizer(int mode);
void oled_show_clock(int index, int h, int m, int s, uint32_t tick);
void oled_show_ready_screen(void);

// New 10 Beautiful Animations
void oled_anim_dna(uint32_t tick);
void oled_anim_fireworks(uint32_t tick);
void oled_anim_tunnel(uint32_t tick);
void oled_anim_snow(uint32_t tick);
void oled_anim_vortex(uint32_t tick);
void oled_anim_cube(uint32_t tick);
void oled_anim_rings(uint32_t tick);
void oled_anim_glitch(uint32_t tick);

// 20 More New Special Animations
void oled_anim_rain(uint32_t tick);
void oled_anim_plasma(uint32_t tick);
void oled_anim_circles(uint32_t tick);
void oled_anim_noise(uint32_t tick);
void oled_anim_swarm(uint32_t tick);
void oled_anim_wave2(uint32_t tick);
void oled_anim_metaballs(uint32_t tick);

// Advanced Mode / Security Tools UI
void oled_show_scanning(uint8_t channel);
void oled_show_ap_list(const char *ssid, int rssi, bool secure, int index, int total);
void oled_show_attack_status(const char *attack_type, const char *target_ssid, const char *status, uint8_t progress, int remaining_sec);
void oled_show_result(bool success, const char *message, const char *attack_type);
void oled_show_countdown(int seconds_remaining, int total_seconds);
void oled_show_swarm_config(void);
bool oled_draw_string_scrolling(int y, const char *str, bool large, int *offset);

#endif // OLED_DISPLAY_H
