idf_component_register(SRCS "app_logic.c"
                    INCLUDE_DIRS "include"
                    PRIV_REQUIRES oled_display ble_comm snake_game wifi_controller attack_manager pcap_serializer driver)
