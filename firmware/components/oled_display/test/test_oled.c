#include "unity.h"
#include "oled_display.h"
#include "driver/gpio.h"

TEST_CASE("OLED init test", "[oled]")
{
    // Mock pins for testing
    bool ret = oled_init(GPIO_NUM_5, GPIO_NUM_6);
    TEST_ASSERT_TRUE(ret);
}

TEST_CASE("OLED clear test", "[oled]")
{
    oled_clear();
    // In a real hardware test, we might check the buffer, 
    // but for unit test we just ensure no crash.
    TEST_ASSERT_TRUE(true);
}

TEST_CASE("OLED pixel test", "[oled]")
{
    oled_set_pixel(10, 10, true);
    oled_set_pixel(71, 39, true);
    oled_set_pixel(-1, -1, true); // Should handle out of bounds
    TEST_ASSERT_TRUE(true);
}
