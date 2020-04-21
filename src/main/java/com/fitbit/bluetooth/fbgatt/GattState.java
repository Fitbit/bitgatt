/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import java.util.Locale;

/**
 * All possible states for a gatt instance
 *
 * Created by iowens on 10/17/17.
 */

public enum GattState {
    /*
     * The application has no client_ifs that is is aware of ( tracker may still be connected
     * to phone )
     */
    DISCONNECTED(StateType.IDLE),
    /*
     * The gatt instance is in the process of un-registering it's client_if
     */
    DISCONNECTING(StateType.IN_PROGRESS),
    /*
     * Gatt server is closed, this really ought never to be used
     */
    SERVER_CLOSED(StateType.IDLE),
    /*
     * Failure un-registering the client_if, the gatt is resting, but is in an error state
     */
    FAILURE_DISCONNECTING(StateType.ERROR),
    /*
     * The bluetooth adapter is set to state off, we can not perform any actions in this state
     */
    BT_OFF(StateType.ERROR),
    /*
     * Waiting for a direct or indirect connection
     */
    CONNECTING(StateType.IN_PROGRESS),
    /*
     * We failed to connect / obtain a client_if
     */
    FAILURE_CONNECTING(StateType.ERROR),
    /*
     * The underlying bluetooth api resulted in a crash when trying to connect
     */
    FAILURE_CONNECTING_WITH_SYSTEM_CRASH(StateType.ERROR),
    /*
     * We successfully established a connection
     */
    CONNECTED(StateType.IDLE),
    /*
     * The default state for a connected and idle gatt
     */
    IDLE(StateType.IDLE),
    /*
     * The state was set successfully
     */
    GATT_CONNECTION_STATE_SET_SUCCESSFULLY(StateType.IDLE),
    /*
     * The state was not set ( will happen if you are trying to set a state that is already the state )
     */
    GATT_CONNECTION_STATE_SET_FAILURE(StateType.ERROR),
    /*
     * The state is being set presently
     */
    GATT_CONNECTION_STATE_SET_IN_PROGRESS(StateType.IN_PROGRESS),
    /*
     * The refresh gatt call was a success
     */
    REFRESH_GATT_SUCCESS(StateType.IDLE),
    /*
     * The refresh gatt call failed
     */
    REFRESH_GATT_FAILURE(StateType.ERROR),
    /*
     * The refresh gatt call is in progress
     */
    REFRESH_GATT_IN_PROGRESS(StateType.IN_PROGRESS),
    /*
     * The gatt instance is now closed and should not be used again
     */
    CLOSED(StateType.IDLE),
    /*
     * The gatt instance is busy discovering services
     */
    DISCOVERING(StateType.IN_PROGRESS),
    /*
     * The gatt instance was successful discovering services
     */
    DISCOVERY_SUCCESS(StateType.IDLE),
    /*
     * The gatt instance failed to complete discovery
     */
    DISCOVERY_FAILURE(StateType.ERROR),
    /*
     * Creating a bond record
     */
    CREATING_BOND(StateType.IN_PROGRESS),
    /*
     * Creating the bond record failed
     */
    CREATE_BOND_FAILURE(StateType.ERROR),
    /*
     * Creating the bond record was successful
     */
    CREATE_BOND_SUCCESS(StateType.IDLE),
    /*
     * The gatt instance timed out before completing discovery
     */
    ADDING_SERVICE(StateType.IN_PROGRESS),
    /*
     * We were successful adding a service to our gatt server
     */
    ADD_SERVICE_SUCCESS(StateType.IDLE),
    /*
     * We failed to add the service to our gatt server
     */
    ADD_SERVICE_FAILURE(StateType.ERROR),
    /*
     * Adding gatt server service characteristic to our gatt server
     */
    ADDING_SERVICE_CHARACTERISTIC(StateType.IN_PROGRESS),
    /*
     * We were successful adding a service characteristic to our gatt server
     */
    ADD_SERVICE_CHARACTERISTIC_SUCCESS(StateType.IDLE),
    /*
     * We failed to add the service characteristic to our gatt server
     */
    ADD_SERVICE_CHARACTERISTIC_FAILURE(StateType.ERROR),
    /*
     * Adding gatt server service characteristic to our gatt server
     */
    ADDING_SERVICE_CHARACTERISTIC_DESCRIPTOR(StateType.IN_PROGRESS),
    /*
     * We were successful adding a service characteristic to our gatt server
     */
    ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_SUCCESS(StateType.IDLE),
    /*
     * We failed to add the service characteristic to our gatt server
     */
    ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_FAILURE(StateType.ERROR),
    /*
     * We are in the process of writing to a characteristic
     */
    WRITING_CHARACTERISTIC(StateType.IN_PROGRESS),
    /*
     * We were successful in writing to the characteristic
     */
    WRITE_CHARACTERISTIC_SUCCESS(StateType.IDLE),
    /*
     * We failed to write to the characteristic
     */
    WRITE_CHARACTERISTIC_FAILURE(StateType.ERROR),
    /*
     * We are in the process of reading from the characteristic
     */
    READING_CHARACTERISTIC(StateType.IN_PROGRESS),
    /*
     * We were successful reading the characteristic
     */
    READ_CHARACTERISTIC_SUCCESS(StateType.IDLE),
    /*
     * We failed in reading the characteristic
     */
    READ_CHARACTERISTIC_FAILURE(StateType.ERROR),
    /*
     * We are in the process of writing to the descriptor
     */
    WRITING_DESCRIPTOR(StateType.IN_PROGRESS),
    /*
     * We were successful in writing to the descriptor
     */
    WRITE_DESCRIPTOR_SUCCESS(StateType.IDLE),
    /*
     * We failed to write to the descriptor
     */
    WRITE_DESCRIPTOR_FAILURE(StateType.ERROR),
    /*
     * We are in the process of reading from the descriptor
     */
    READING_DESCRIPTOR(StateType.IN_PROGRESS),
    /*
     * We succeeded in reading the descriptor
     */
    READ_DESCRIPTOR_SUCCESS(StateType.IDLE),
    /*
     * We failed in reading the descriptor
     */
    READ_DESCRIPTOR_FAILURE(StateType.ERROR),
    /*
     * We are in the process of reading the RSSI of the remote device
     */
    READING_RSSI(StateType.IN_PROGRESS),
    /*
     * We succeeded in reading the RSSI
     */
    READ_RSSI_SUCCESS(StateType.IDLE),
    /*
     * We failed in reading the RSSI
     */
    READ_RSSI_FAILURE(StateType.ERROR),
    /*
     * We are in the process of requesting a new MTU
     */
    REQUESTING_MTU(StateType.IN_PROGRESS),
    /*
     * We were successful requesting a new MTU
     */
    REQUEST_MTU_SUCCESS(StateType.IDLE),
    /*
     * We failed in requesting a new MTU
     */
    REQUEST_MTU_FAILURE(StateType.ERROR),
    /*
     * We are in the process of enabling characteristic notifications
     */
    ENABLING_CHARACTERISTIC_NOTIFICATION(StateType.IN_PROGRESS),
    /*
     * We successfully enabled characteristic notifications
     */
    ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS(StateType.IDLE),
    /*
     * We failed enabling characteristic notifications
     */
    ENABLE_CHARACTERISTIC_NOTIFICATION_FAILURE(StateType.ERROR),
    /*
    * We are in the process of disabling characteristic notifications
    */
    DISABLING_CHARACTERISTIC_NOTIFICATION(StateType.IN_PROGRESS),
    /*
     * We successfully disabled characteristic notifications
     */
    DISABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS(StateType.IDLE),
    /*
    * We failed disabling characteristic notifications
    */
    DISABLE_CHARACTERISTIC_NOTIFICATION_FAILURE(StateType.ERROR),
    /*
     * Notify is different than indicate because we can not transition states until we get a
     * response for an indication.  We are notifying on a characteristic.
     */
    NOTIFYING_CHARACTERISTIC(StateType.IN_PROGRESS),
    /*
     * Successfully notified on the characteristic
     */
    NOTIFY_CHARACTERISTIC_SUCCESS(StateType.IDLE),
    /*
     * Failed to notify on the characteristic
     */
    NOTIFY_CHARACTERISTIC_FAILURE(StateType.ERROR),
    /*
     * In the process of indicating on a characteristic
     */
    INDICATING_CHARACTERISTIC(StateType.IN_PROGRESS),
    /*
     * We were successful indicating on the characteristic
     */
    INDICATE_CHARACTERISTIC_SUCCESS(StateType.IDLE),
    /*
     * We failed indicating on the characteristic
     */
    INDICATE_CHARACTERISTIC_FAILURE(StateType.ERROR),
    /*
     * In the process of requesting a connection interval change
     */
    REQUESTING_CONNECTION_INTERVAL_CHANGE(StateType.IN_PROGRESS),
    /*
     * A successful request has been made, if the OS is going to actually do it? who knows?
     */
    REQUEST_CONNECTION_INTERVAL_SUCCESS(StateType.IDLE),
    /*
     * I'm not sure really what this means, basically that the stack refused to take your request
     */
    REQUEST_CONNECTION_INTERVAL_FAILURE(StateType.ERROR),
    /*
     * Closing the gatt client
     */
    CLOSING_GATT_CLIENT(StateType.IN_PROGRESS),
    /*
     * Close gatt client successful
     */
    CLOSE_GATT_CLIENT_SUCCESS(StateType.IDLE),
    /*
     * Sending server response for a read request
     */
    SENDING_SERVER_RESPONSE(StateType.IN_PROGRESS),
    /*
     * Send server response failure
     */
    SEND_SERVER_RESPONSE_FAILURE(StateType.ERROR),
    /*
     * Send server response success
     */
    SEND_SERVER_RESPONSE_SUCCESS(StateType.IDLE),
    /*
     * Getting server services
     */
    GETTING_SERVER_SERVICES(StateType.IN_PROGRESS),
    /*
     * Get server services success
     */
    GET_SERVER_SERVICES_SUCCESS(StateType.IDLE),
    /*
     * Clear gatt server services
     */
    CLEARING_GATT_SERVER_SERVICES(StateType.IN_PROGRESS),
    /*
     * Clear gatt server services success
     */
    CLEAR_GATT_SERVER_SERVICES_SUCCESS(StateType.IDLE),
    /*
     * In the process of closing the gatt server
     */
    CLOSING_GATT_SERVER(StateType.IN_PROGRESS),
    /*
     * Successfully closed the gatt server
     */
    CLOSE_GATT_SERVER_SUCCESS(StateType.IDLE),
    /*
     * Failed to close the gatt server
     */
    CLOSE_GATT_SERVER_FAILURE(StateType.ERROR),
    /*
     * Removing server service
     */
    REMOVING_SERVER_SERVICE(StateType.IN_PROGRESS),
    /*
     * Remove server service failure
     */
    REMOVE_SERVER_SERVICE_FAILURE(StateType.ERROR),
    /*
     * Remove server service success
     */
    REMOVE_SERVER_SERVICE_SUCCESS(StateType.IDLE),
    /*
     * Requesting a physical layer change
     */
    REQUESTING_PHY_CHANGE(StateType.IN_PROGRESS),
    /*
     * Request physical layer change success
     */
    REQUEST_PHY_CHANGE_SUCCESS(StateType.IDLE),
    /*
     * Request physical layer change failure
     */
    REQUEST_PHY_CHANGE_FAILURE(StateType.ERROR),
    /*
     * Reading the present physical layer
     */
    READING_CURRENT_PHY(StateType.IN_PROGRESS),
    /*
     * Successfully read the current physical layer
     */
    READ_CURRENT_PHY_SUCCESS(StateType.IDLE),
    /*
     * Failed to read the current physical layer
     */
    READ_CURRENT_PHY_FAILURE(StateType.ERROR);

    StateType stateType;

    GattState(StateType type) {
        this.stateType = type;
    }

    public StateType getStateType(){
        return this.stateType;
    }


    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%s - State Type: %s", name(), getStateType().name());
    }
}

