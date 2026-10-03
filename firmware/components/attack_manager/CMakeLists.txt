idf_component_register(SRCS "attack.c" "attack_dos.c" "attack_handshake.c" "attack_method.c" "attack_pmkid.c" "attack_beacon.c"
                    INCLUDE_DIRS .
                    REQUIRES esp_wifi esp_event nvs_flash esp_timer wifi_controller wsl_bypasser frame_analyzer pcap_serializer hccapx_serializer
                    PRIV_REQUIRES ble_comm)
