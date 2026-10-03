#include "snake_2x2.h"
#include "oled_display.h"
#include "esp_timer.h"
#include "esp_random.h"
#include <string.h>
#include <stdlib.h>

#define SNAKE_SIZE 2
#define FOOD_SIZE 2
#define MOVE_STEP 3

/**
 * @brief Checks if two rectangles intersect.
 * 
 * @param x1 X coordinate of the first rectangle.
 * @param y1 Y coordinate of the first rectangle.
 * @param w1 Width of the first rectangle.
 * @param h1 Height of the first rectangle.
 * @param x2 X coordinate of the second rectangle.
 * @param y2 Y coordinate of the second rectangle.
 * @param w2 Width of the second rectangle.
 * @param h2 Height of the second rectangle.
 * @return true if they intersect, false otherwise.
 */
static bool rect_intersect(uint8_t x1, uint8_t y1, uint8_t w1, uint8_t h1, uint8_t x2, uint8_t y2, uint8_t w2, uint8_t h2) {
    return x2 < x1 + w1 && x2 + w2 > x1 && y2 < y1 + h1 && y2 + h2 > y1;
}

/**
 * @brief Generates a new food position for the 2x2 game mode.
 * 
 * The food is placed on a grid defined by MOVE_STEP to ensure alignment with
 * snake movement. It avoids overlapping with the current snake body.
 * 
 * @param game Pointer to the snake game state.
 */
static void game_generate_food(SnakeGameState* game) {
    bool on_snake;
    Point new_food;
    const uint8_t max_slots_x = (GRID_W - SNAKE_SIZE) / MOVE_STEP;
    const uint8_t max_slots_y = (GRID_H - SNAKE_SIZE) / MOVE_STEP;
    do {
        on_snake = false;
        uint8_t slot_x = (esp_random() % (max_slots_x - 1)) + 1;
        uint8_t slot_y = (esp_random() % (max_slots_y - 1)) + 1;
        new_food.x = slot_x * MOVE_STEP;
        new_food.y = slot_y * MOVE_STEP;
        for (uint16_t i = 0; i < game->snake_length; i++) {
            if (rect_intersect(new_food.x, new_food.y, FOOD_SIZE, FOOD_SIZE, game->snake[i].x, game->snake[i].y, SNAKE_SIZE, SNAKE_SIZE)) {
                on_snake = true;
                break;
            }
        }
    } while (on_snake);
    game->food = new_food;
}

/**
 * @brief Checks if a 2x2 position is safe.
 * 
 * @param game Pointer to the snake game state.
 * @param pos The position to check.
 * @param check_body True to check for body collisions.
 * @return true if safe, false otherwise.
 */
static bool game_is_safe_position(const SnakeGameState* game, Point pos, bool check_body) {
    if (pos.x + SNAKE_SIZE > GRID_W || pos.y + SNAKE_SIZE > GRID_H) return false;
    if (check_body) {
        for (uint16_t i = 0; i < game->snake_length - 1; i++) {
            if (rect_intersect(pos.x, pos.y, SNAKE_SIZE, SNAKE_SIZE, game->snake[i].x, game->snake[i].y, SNAKE_SIZE, SNAKE_SIZE)) return false;
        }
    }
    return true;
}

/**
 * @brief BFS pathfinding for the 2x2 game mode.
 * 
 * @param game Pointer to the snake game state.
 * @return The first move direction, or DIR_NONE.
 */
static Direction game_bfs_to_food(SnakeGameState* game) {
    typedef struct { Point pos; uint8_t first_move; } BFSNode;
    static BFSNode queue[800];
    uint16_t front = 0, rear = 0;
    static bool visited[GRID_H][GRID_W];
    memset(visited, 0, sizeof(visited));
    queue[rear].pos = game->snake[0];
    queue[rear].first_move = DIR_NONE;
    rear++;
    visited[game->snake[0].y][game->snake[0].x] = true;
    const Point moves[] = {{MOVE_STEP, 0}, {-MOVE_STEP, 0}, {0, -MOVE_STEP}, {0, MOVE_STEP}};
    while (front != rear) {
        BFSNode current = queue[front];
        front = (front + 1) % 800;
        if (rect_intersect(current.pos.x, current.pos.y, SNAKE_SIZE, SNAKE_SIZE, game->food.x, game->food.y, FOOD_SIZE, FOOD_SIZE)) return current.first_move;
        for (uint8_t dir = 0; dir < 4; dir++) {
            Point next = { current.pos.x + moves[dir].x, current.pos.y + moves[dir].y };
            if (next.x < GRID_W && next.y < GRID_H && !visited[next.y][next.x] && game_is_safe_position(game, next, true)) {
                visited[next.y][next.x] = true;
                queue[rear].pos = next;
                queue[rear].first_move = (current.first_move == DIR_NONE) ? dir : current.first_move;
                rear = (rear + 1) % 800;
                if (rear == front) break;
            }
        }
    }
    return DIR_NONE;
}

/**
 * @brief Simulates moves for the 2x2 game mode.
 * 
 * @param game Pointer to the snake game state.
 * @param start_dir Initial direction.
 * @param lookahead_steps Steps to simulate.
 * @return Simulation result.
 */
static SimResult game_simulate_move(const SnakeGameState* game, Direction start_dir, uint16_t lookahead_steps) {
    SimResult res = { .safe = true, .steps = 0, .score = 0 };
    static Point sim_snake[MAX_SNAKE_LENGTH];
    uint16_t sim_len = game->snake_length;
    memcpy(sim_snake, game->snake, sizeof(Point) * sim_len);
    Point sim_food = game->food;
    Direction current_dir = start_dir;
    for (uint16_t i = 0; i < lookahead_steps; i++) {
        Point next_head = sim_snake[0];
        switch (current_dir) {
            case DIR_RIGHT: next_head.x += MOVE_STEP; break;
            case DIR_LEFT:  next_head.x -= MOVE_STEP; break;
            case DIR_UP:    next_head.y -= MOVE_STEP; break;
            case DIR_DOWN:  next_head.y += MOVE_STEP; break;
            default: break;
        }
        if (next_head.x + SNAKE_SIZE > GRID_W || next_head.y + SNAKE_SIZE > GRID_H) { res.safe = false; res.steps = i; return res; }
        for (uint16_t j = 0; j < sim_len - 1; j++) {
            if (rect_intersect(next_head.x, next_head.y, SNAKE_SIZE, SNAKE_SIZE, sim_snake[j].x, sim_snake[j].y, SNAKE_SIZE, SNAKE_SIZE)) { res.safe = false; res.steps = i; return res; }
        }
        uint16_t sim_limit = (sim_len < MAX_SNAKE_LENGTH - 1) ? sim_len : MAX_SNAKE_LENGTH - 1;
        for (int16_t j = sim_limit; j > 0; j--) sim_snake[j] = sim_snake[j-1];
        sim_snake[0] = next_head;
        if (rect_intersect(next_head.x, next_head.y, SNAKE_SIZE, SNAKE_SIZE, sim_food.x, sim_food.y, FOOD_SIZE, FOOD_SIZE)) res.score += 10;
        res.steps = i + 1;
        if (!rect_intersect(next_head.x, next_head.y, SNAKE_SIZE, SNAKE_SIZE, sim_food.x, sim_food.y, FOOD_SIZE, FOOD_SIZE)) {
            if (sim_food.x > next_head.x && current_dir != DIR_LEFT) current_dir = DIR_RIGHT;
            else if (sim_food.x < next_head.x && current_dir != DIR_RIGHT) current_dir = DIR_LEFT;
            else if (sim_food.y > next_head.y && current_dir != DIR_UP) current_dir = DIR_DOWN;
            else if (sim_food.y < next_head.y && current_dir != DIR_DOWN) current_dir = DIR_UP;
        }
    }
    return res;
}

/**
 * @brief Fallback AI for the 2x2 game mode.
 * 
 * @param game Pointer to the snake game state.
 * @return Best direction.
 */
static Direction game_fallback_ai(SnakeGameState* game) {
    Point head = game->snake[0];
    Direction best_dir = game->direction;
    int32_t max_score = -2000000;
    for (uint8_t d = 0; d < 4; d++) {
        Direction dir = (Direction)d;
        if ((dir == DIR_RIGHT && game->direction == DIR_LEFT) || (dir == DIR_LEFT && game->direction == DIR_RIGHT) ||
            (dir == DIR_UP && game->direction == DIR_DOWN) || (dir == DIR_DOWN && game->direction == DIR_UP)) continue;
        Point next = head;
        switch (dir) {
            case DIR_RIGHT: next.x += MOVE_STEP; break;
            case DIR_LEFT:  next.x -= MOVE_STEP; break;
            case DIR_UP:    next.y -= MOVE_STEP; break;
            case DIR_DOWN:  next.y += MOVE_STEP; break;
            default: break;
        }
        if (!game_is_safe_position(game, next, true)) continue;
        SimResult sim = game_simulate_move(game, dir, game->lookahead_steps);
        int32_t score = 0;
        if (sim.safe) {
            score += 10000; score += sim.score * 100;
            int32_t dist = abs((int16_t)next.x - (int16_t)game->food.x) + abs((int16_t)next.y - (int16_t)game->food.y);
            score -= dist;
        } else score = sim.steps;
        if (score > max_score) { max_score = score; best_dir = dir; }
    }
    return best_dir;
}

/**
 * @brief Initializes the 2x2 snake game.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_2x2_init(SnakeGameState* game) {
    memset(game, 0, sizeof(SnakeGameState));
    const uint8_t startX = 21, startY = 21;
    game->snake[0] = (Point){startX, startY};
    game->snake[1] = (Point){startX - MOVE_STEP, startY};
    game->snake[2] = (Point){startX - (MOVE_STEP * 2), startY};
    game->snake_length = 3;
    game->direction = DIR_RIGHT; game->next_direction = DIR_RIGHT;
    game->update_interval_ms = 80; game->last_update_ms = esp_timer_get_time() / 1000;
    game->lookahead_steps = 500;
    game_generate_food(game);
    game->food_visible = true;
}

/**
 * @brief Updates the 2x2 snake game state.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_2x2_update(SnakeGameState* game) {
    if (game->game_over) return;
    Direction bfs_move = game_bfs_to_food(game);
    game->direction = (bfs_move != DIR_NONE) ? bfs_move : game_fallback_ai(game);
    Point new_head = game->snake[0];
    switch (game->direction) {
        case DIR_RIGHT: new_head.x += MOVE_STEP; break;
        case DIR_LEFT:  new_head.x -= MOVE_STEP; break;
        case DIR_UP:    new_head.y -= MOVE_STEP; break;
        case DIR_DOWN:  new_head.y += MOVE_STEP; break;
        default: break;
    }
    bool food_eaten = rect_intersect(new_head.x, new_head.y, SNAKE_SIZE, SNAKE_SIZE, game->food.x, game->food.y, FOOD_SIZE, FOOD_SIZE);
    if (game->growth_pending > 0 && game->snake_length < MAX_SNAKE_LENGTH - 1) {
        game->snake_length++; game->growth_pending--;
    }
    for (int16_t i = game->snake_length - 1; i > 0; i--) game->snake[i] = game->snake[i - 1];
    game->snake[0] = new_head;
    if (food_eaten) {
        game->score += 10; game->growth_pending += 1;
        game_generate_food(game);
        if (game->score > game->high_score) game->high_score = game->score;
    }
    if (new_head.x + SNAKE_SIZE > GRID_W || new_head.y + SNAKE_SIZE > GRID_H) { 
        game->game_over = true; 
        game->game_over_time_ms = esp_timer_get_time() / 1000;
        return; 
    }
    for (uint16_t i = 1; i < game->snake_length; i++) {
        if (rect_intersect(new_head.x, new_head.y, SNAKE_SIZE, SNAKE_SIZE, game->snake[i].x, game->snake[i].y, SNAKE_SIZE, SNAKE_SIZE)) { 
            game->game_over = true; 
            game->game_over_time_ms = esp_timer_get_time() / 1000;
            return; 
        }
    }
    uint32_t now_ms = esp_timer_get_time() / 1000;
    game->blink_counter = (now_ms / 200) % 4;
}

/**
 * @brief Draws the 2x2 snake game.
 * 
 * Renders the snake segments and fills gaps between them for a continuous look.
 * Also handles food rendering with a multi-stage blinking animation based on 
 * the game's blink counter.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_2x2_draw(SnakeGameState* game) {
    if (game->game_over) {
        snake_draw_game_over(game);
        return;
    }
    oled_lock();
    oled_clear();
    for (uint16_t i = 0; i < game->snake_length; i++) {
        const Point curr = game->snake[i];
        for (uint8_t bx = 0; bx < SNAKE_SIZE; bx++)
            for (uint8_t by = 0; by < SNAKE_SIZE; by++)
                oled_set_pixel_internal(curr.x + bx, curr.y + by, true);
        if (i < game->snake_length - 1) {
            const Point next = game->snake[i + 1];
            uint8_t gap_x, gap_y, gap_w, gap_h;
            if (next.x > curr.x) { gap_x = curr.x + SNAKE_SIZE; gap_y = curr.y; gap_w = next.x - gap_x; gap_h = SNAKE_SIZE; }
            else if (next.x < curr.x) { gap_x = next.x + SNAKE_SIZE; gap_y = curr.y; gap_w = curr.x - gap_x; gap_h = SNAKE_SIZE; }
            else if (next.y > curr.y) { gap_x = curr.x; gap_y = curr.y + SNAKE_SIZE; gap_w = SNAKE_SIZE; gap_h = next.y - gap_y; }
            else { gap_x = curr.x; gap_y = next.y + SNAKE_SIZE; gap_w = SNAKE_SIZE; gap_h = curr.y - gap_y; }
            for (uint8_t gx = 0; gx < gap_w; gx++)
                for (uint8_t gy = 0; gy < gap_h; gy++)
                    oled_set_pixel_internal(gap_x + gx, gap_y + gy, true);
        }
    }
    if (game->food_visible) {
        uint8_t fx = game->food.x, fy = game->food.y;
        switch (game->blink_counter) {
            case 0: for (uint8_t bx = 0; bx < 2; bx++) for (uint8_t by = 0; by < 2; by++) oled_set_pixel_internal(fx + bx, fy + by, true); break;
            case 1: case 3:
                for (int8_t bx = 0; bx < 2; bx++) for (int8_t by = -1; by < 3; by++) { int16_t dy = (int16_t)fy + by; if (fx + bx < GRID_W && dy >= 0 && dy < GRID_H) oled_set_pixel_internal(fx + bx, (uint8_t)dy, true); }
                for (int8_t bx = -1; bx < 3; bx++) for (int8_t by = 0; by < 2; by++) { int16_t dx = (int16_t)fx + bx; if (dx >= 0 && dx < GRID_W && fy + by < GRID_H) oled_set_pixel_internal((uint8_t)dx, fy + by, true); }
                break;
            case 2:
                for (int8_t bx = -1; bx < 3; bx++) for (int8_t by = -1; by < 3; by++) { int16_t dx = (int16_t)fx + bx; int16_t dy = (int16_t)fy + by; if (dx >= 0 && dx < GRID_W && dy >= 0 && dy < GRID_H) oled_set_pixel_internal((uint8_t)dx, (uint8_t)dy, true); }
                break;
        }
    }
    oled_unlock();
}
