#include "snake_common.h"
#include "oled_display.h"
#include <string.h>

/**
 * @brief Draws the game over screen for the snake game.
 * 
 * Clears the display and shows a "GAME OVER" message.
 * 
 * @param game Pointer to the snake game state.
 */
void snake_draw_game_over(SnakeGameState* game) {
    oled_lock();
    oled_clear();
    // Simplified Game Over screen as requested
    oled_draw_string(10, 16, "GAME OVER", false);
    oled_unlock();
}
