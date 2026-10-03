idf_component_register(SRCS "snake_1x1.c" "snake_2x2.c" "snake_common.c"
                    INCLUDE_DIRS "include"
                    REQUIRES oled_display esp_timer esp_hw_support)
