idf_component_register(SRCS "ble_comm.c"
                    INCLUDE_DIRS "include"
                    REQUIRES bt nvs_flash
                    PRIV_REQUIRES app_logic)
