#ifndef SNAKE_COMMON_H
#define SNAKE_COMMON_H

#include <stdint.h>
#include <stdbool.h>

#define GRID_W 72
#define GRID_H 40
#define MAX_SNAKE_LENGTH 750

typedef enum {
    GAME_MODE_AI = 0,
    GAME_MODE_MANUAL = 1
} GameMode;

typedef enum {
    DIR_RIGHT = 0,
    DIR_LEFT = 1,
    DIR_UP = 2,
    DIR_DOWN = 3,
    DIR_NONE = 255
} Direction;

typedef struct {
    uint8_t x;
    uint8_t y;
} Point;

typedef struct {
    bool safe;
    uint16_t steps;
    uint16_t score;
} SimResult;

typedef struct {
    Point snake[MAX_SNAKE_LENGTH];
    uint16_t snake_length;
    Point food;
    Direction direction;
    Direction next_direction;
    
    uint16_t score;
    uint16_t high_score;
    uint32_t moves;
    
    GameMode mode;
    bool game_over;
    bool paused;
    
    uint16_t lookahead_steps;
    uint32_t last_update_ms;
    uint32_t update_interval_ms;
    uint32_t game_over_time_ms;
    
    bool food_visible;
    uint8_t blink_counter;
    uint16_t growth_pending;
} SnakeGameState;

void snake_draw_game_over(SnakeGameState* game);

#endif
