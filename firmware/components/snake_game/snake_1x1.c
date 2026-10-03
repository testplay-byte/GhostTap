#include "snake_1x1.h"
#include "oled_display.h"
#include "esp_timer.h"
#include "esp_random.h"
#include <string.h>
#include <stdlib.h>

#define MOVE_STEP 1

/**
 * @brief Generates a new food position that is not on the snake's body.
 * 
 * @param game Pointer to the snake game state.
 */
static void game_generate_food(SnakeGameState* game) {
    bool on_snake;
    Point new_food;
    do {
        on_snake = false;
        new_food.x = (esp_random() % (GRID_W / 2)) * 2;
        new_food.y = (esp_random() % (GRID_H / 2)) * 2;
        for (uint16_t i = 0; i < game->snake_length; i++) {
            if (game->snake[i].x == new_food.x && game->snake[i].y == new_food.y) {
                on_snake = true;
                break;
            }
        }
    } while (on_snake);
    game->food = new_food;
}

/**
 * @brief Checks if a position is safe (within bounds and optionally not on the snake's body).
 * 
 * @param game Pointer to the snake game state.
 * @param pos The position to check.
 * @param check_body True to check for collisions with the snake's body.
 * @return true if the position is safe, false otherwise.
 */
static bool game_is_safe_position(const SnakeGameState* game, Point pos, bool check_body) {
    if (pos.x >= GRID_W || pos.y >= GRID_H) return false;
    if (check_body) {
        for (uint16_t i = 0; i < game->snake_length - 1; i++) {
            if (game->snake[i].x == pos.x && game->snake[i].y == pos.y) return false;
        }
    }
    return true;
}

/**
 * @brief Uses Breadth-First Search to find the shortest path to the food.
 * 
 * @param game Pointer to the snake game state.
 * @return The direction of the first move in the shortest path, or DIR_NONE if no path exists.
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
    const Point moves[] = {{2, 0}, {-2, 0}, {0, -2}, {0, 2}};
    while (front != rear) {
        BFSNode current = queue[front];
        front = (front + 1) % 800;
        if (current.pos.x == game->food.x && current.pos.y == game->food.y) return current.first_move;
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
 * @brief Simulates a series of moves to evaluate the safety and potential score of a direction.
 * 
 * @param game Pointer to the snake game state.
 * @param start_dir The initial direction to simulate.
 * @param lookahead_steps Number of steps to look ahead.
 * @return A SimResult structure containing simulation results.
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
            case DIR_RIGHT: next_head.x += 2; break;
            case DIR_LEFT:  next_head.x -= 2; break;
            case DIR_UP:    next_head.y -= 2; break;
            case DIR_DOWN:  next_head.y += 2; break;
            default: break;
        }
        if (next_head.x >= GRID_W || next_head.y >= GRID_H) { res.safe = false; res.steps = i; return res; }
        for (uint16_t j = 0; j < sim_len - 1; j++) {
            if (sim_snake[j].x == next_head.x && sim_snake[j].y == next_head.y) { res.safe = false; res.steps = i; return res; }
        }
        uint16_t sim_limit = (sim_len < MAX_SNAKE_LENGTH - 1) ? sim_len : MAX_SNAKE_LENGTH - 1;
        for (int16_t j = sim_limit; j > 0; j--) sim_snake[j] = sim_snake[j-1];
        sim_snake[0] = next_head;
        if (next_head.x == sim_food.x && next_head.y == sim_food.y) res.score += 10;
        res.steps = i + 1;
        if (!(next_head.x == sim_food.x && next_head.y == sim_food.y)) {
            if (sim_food.x > next_head.x && current_dir != DIR_LEFT) current_dir = DIR_RIGHT;
            else if (sim_food.x < next_head.x && current_dir != DIR_RIGHT) current_dir = DIR_LEFT;
            else if (sim_food.y > next_head.y && current_dir != DIR_UP) current_dir = DIR_DOWN;
            else if (sim_food.y < next_head.y && current_dir != DIR_DOWN) current_dir = DIR_UP;
        }
    }
    return res;
}

/**
 * @brief Fallback AI logic when BFS fails to find a path to the food.
 * 
 * Evaluates all possible moves using simulation and picks the best one.
 * 
 * @param game Pointer to the snake game state.
 * @return The best direction to move.
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
            case DIR_RIGHT: next.x += 2; break;
            case DIR_LEFT:  next.x -= 2; break;
            case DIR_UP:    next.y -= 2; break;
            case DIR_DOWN:  next.y += 2; break;
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
 * @brief Initializes the 1x1 snake game.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_1x1_init(SnakeGameState* game) {
    memset(game, 0, sizeof(SnakeGameState));
    game->snake[0] = (Point){20, 20}; game->snake[1] = (Point){19, 20}; game->snake[2] = (Point){18, 20};
    game->snake[3] = (Point){17, 20}; game->snake[4] = (Point){16, 20}; game->snake[5] = (Point){15, 20};
    game->snake_length = 6;
    game->direction = DIR_RIGHT; game->next_direction = DIR_RIGHT;
    game->update_interval_ms = 25; game->last_update_ms = esp_timer_get_time() / 1000;
    game->lookahead_steps = 300;
    game_generate_food(game);
    game->food_visible = true;
}

/**
 * @brief Updates the 1x1 snake game state.
 * 
 * Handles movement, food consumption, collisions, and AI decision making.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_1x1_update(SnakeGameState* game) {
    if (game->game_over) return;
    if (game->snake[0].x % 2 == 0 && game->snake[0].y % 2 == 0) {
        Direction bfs_move = game_bfs_to_food(game);
        game->direction = (bfs_move != DIR_NONE) ? bfs_move : game_fallback_ai(game);
    }
    Point new_head = game->snake[0];
    switch (game->direction) {
        case DIR_RIGHT: new_head.x += MOVE_STEP; break;
        case DIR_LEFT:  new_head.x -= MOVE_STEP; break;
        case DIR_UP:    new_head.y -= MOVE_STEP; break;
        case DIR_DOWN:  new_head.y += MOVE_STEP; break;
        default: break;
    }
    if (new_head.x == game->food.x && new_head.y == game->food.y) {
        game->score += 10; game->growth_pending += 2;
        game_generate_food(game);
        if (game->score > game->high_score) game->high_score = game->score;
    }
    if (game->growth_pending > 0 && game->snake_length < MAX_SNAKE_LENGTH - 1) {
        game->snake_length++; game->growth_pending--;
    }
    for (int16_t i = game->snake_length - 1; i > 0; i--) game->snake[i] = game->snake[i - 1];
    game->snake[0] = new_head;
    if (new_head.x >= GRID_W || new_head.y >= GRID_H) { 
        game->game_over = true; 
        game->game_over_time_ms = esp_timer_get_time() / 1000;
        return; 
    }
    for (uint16_t i = 1; i < game->snake_length; i++) {
        if (new_head.x == game->snake[i].x && new_head.y == game->snake[i].y) { 
            game->game_over = true; 
            game->game_over_time_ms = esp_timer_get_time() / 1000;
            return; 
        }
    }
    uint32_t now_ms = esp_timer_get_time() / 1000;
    game->blink_counter = (now_ms / 150) % 4;
}

/**
 * @brief Draws the current state of the 1x1 snake game.
 * 
 * Renders individual snake segments and interpolates pixels between them to
 * create a smooth, connected body. Food is rendered with a pulsing animation
 * that changes shape based on the blink counter.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_1x1_draw(SnakeGameState* game) {
    if (game->game_over) {
        snake_draw_game_over(game);
        return;
    }
    oled_lock();
    oled_clear();
    for (uint16_t i = 0; i < game->snake_length; i++) {
        if (game->snake[i].x < GRID_W && game->snake[i].y < GRID_H) oled_set_pixel_internal(game->snake[i].x, game->snake[i].y, true);
        if (i < game->snake_length - 1) {
            uint8_t mid_x = (game->snake[i].x + game->snake[i+1].x) / 2;
            uint8_t mid_y = (game->snake[i].y + game->snake[i+1].y) / 2;
            if (mid_x < GRID_W && mid_y < GRID_H) oled_set_pixel_internal(mid_x, mid_y, true);
        }
    }
    if (game->food_visible) {
        uint8_t fx = game->food.x, fy = game->food.y;
        switch (game->blink_counter) {
            case 0: oled_set_pixel_internal(fx, fy, true); break;
            case 1: case 3:
                oled_set_pixel_internal(fx, fy, true);
                if (fx > 0) oled_set_pixel_internal(fx - 1, fy, true);
                if (fx < GRID_W - 1) oled_set_pixel_internal(fx + 1, fy, true);
                if (fy > 0) oled_set_pixel_internal(fx, fy - 1, true);
                if (fy < GRID_H - 1) oled_set_pixel_internal(fx, fy + 1, true);
                break;
            case 2:
                for (int8_t dx = -1; dx <= 1; dx++)
                    for (int8_t dy = -1; dy <= 1; dy++)
                        if (fx+dx >= 0 && fx+dx < GRID_W && fy+dy >= 0 && fy+dy < GRID_H)
                            oled_set_pixel_internal(fx+dx, fy+dy, true);
                break;
        }
    }
    oled_unlock();
}
