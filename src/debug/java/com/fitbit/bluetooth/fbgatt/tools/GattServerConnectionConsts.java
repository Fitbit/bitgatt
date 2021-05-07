/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.tools;

/**
 * Holder class for all the constant values used by the plugin.
 */
public class GattServerConnectionConsts {
    // Indicates a command passed
    public static final String PASS_STATUS = "pass";

    // Indicates a command failed
    public static final String FAIL_STATUS = "fail";

    // JSON Keys
    public static final String COMMAND_KEY = "command";
    public static final String STATUS_KEY = "status";
    public static final String RESULT_KEY = "result";
    public static final String ERROR_KEY = "error";

    // JSON Result object keys
    public static final String RESULT_RSSI_KEY = "rssi";
    public static final String RESULT_SERVICE_UUID_KEY = "service_uuid";
    public static final String RESULT_SERVICE_TYPE_KEY = "type";
    public static final String RESULT_CHARACTERISTIC_UUID_KEY = "characteristic_UUID";
    public static final String RESULT_DESCRIPTOR_UUID_KEY = "descriptor_uuid";
    public static final String RESULT_PERMISSIONS_KEY = "permissions";
    public static final String RESULT_PROPERTIES_KEY = "properties";
    public static final String RESULT_VALUE_KEY = "value";
    public static final String RESULT_CHARACTERISTIC_VALUE = "characteristic_value";
    public static final String RESULT_DESCRIPTOR_VALUE = "descriptor_value";
    public static final String GATT_SERVER_CHARACTERISTIC_READ_REQUEST_VALUE = "server_characteristic_read_request";
    public static final String GATT_SERVER_CHARACTERISTIC_WRITE_REQUEST_VALUE = "server_characteristic_write_request";
    public static final String GATT_SERVER_DESCRIPTOR_READ_REQUEST_VALUE = "server_descriptor_read_request";
    public static final String GATT_SERVER_DESCRIPTOR_WRITE_REQUEST_VALUE = "server_descriptor_write_request";
    public static final String GATT_SERVER_CONNECTION_STATE_CHANGE_VALUE = "server_connection_state_change";
    public static final String GATT_SERVER_MTU_CHANGE_VALUE = "server_mtu_change_value";
    public static final String GATT_CLIENT_CONNECTION_STATE_CHANGED = "gatt_client_connection_state_changed";
    public static final String GATT_CLIENT_CHARACTERISTIC_CHANGED = "gatt_client_characteristic_changed";
    public static final String GATT_CLIENT_PERIPHERAL_DEVICE_PROPERTIES_CHANGED = "gatt_client_peripheral_properties_changed";
    public static final String GATT_CLIENT_DISCOVERED_SERVICES = "gatt_client_discovered_services";
    public static final String GATT_CLIENT_CHANGED_PHY = "gatt_client_changed_phy";
    public static final String GATT_CLIENT_CHANGED_MTU = "gatt_client_changed_mtu";

    //  JSON Keys
    public static final String JSON_NEW_MTU_VALUE_KEY = "new_mtu_value";
    public static final String JSON_SERVER_IS_CONNECTED_KEY = "server_is_connected";
    public static final String JSON_SERVICE_UUID_KEY = "service_uuid";
    public static final String JSON_CHARACTERISTIC_UUID_KEY = "characteristic_uuid";
    public static final String JSON_DESCRIPTOR_UUID_KEY = "descriptor_uuid";
    public static final String JSON_OFFSET_KEY = "offset";
    public static final String JSON_REQUEST_ID_KEY = "requestId";
    public static final String JSON_DATA_KEY = "data";

    public enum DeviceType {
        DEVICE_TYPE_CLASSIC("classic"), DEVICE_TYPE_DUAL("dual"), DEVICE_TYPE_LE("low energy"), DEVICE_TYPE_UNKNOWN("unknown");

        private final String value;

        DeviceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
