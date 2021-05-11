/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands;

import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.commands.server.BluetoothArgsInterface;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tools.GattPluginCommandInterface;
import com.fitbit.bluetooth.fbgatt.utils.JsonBuilder;

/**
 * Base implementation of a GattCommand.
 */
public abstract class AbstractGattCommand implements GattPluginCommandInterface, BluetoothArgsInterface {
    private final String shortName;
    private final String fullName;
    private final String description;

    protected final JsonBuilder jsonBuilder;
    protected final ResponseHandler responseHandler;

    protected AbstractGattCommand(String shortName, String fullName, String description, PluginLoggerInterface logger) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.description = description;
        this.responseHandler = new ResponseHandler(shortName, logger);

        this.jsonBuilder = new JsonBuilder(logger);
    }

    @Override
    public final String getFullName() {
        return this.shortName;
    }

    @Override
    public final String getShortName() {
        return this.fullName;
    }

    @Override
    public final String getDescription() {
        return this.description;
    }

    protected final void onMessage(PluginCommandConfig config, String message) {
        responseHandler.onMessage(config, message);
    }

    protected final void onSuccess(PluginCommandConfig config, String message) {
        responseHandler.onMessage(config, "SUCCESS :" + message);
    }

    protected final void onFailure(PluginCommandConfig config, String message) {
        responseHandler.onMessage(config, "FAILURE :" + message);
    }

    protected void onResult(PluginCommandConfig config, TransactionResult result) {
        responseHandler.onResponse(config, result);
    }

    protected final void onError(PluginCommandConfig config, Exception e) {
        responseHandler.onError(config, e);
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the mac address
     */
    @Override
    public int getMacArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the service UUID
     */
    @Override
    public int getServiceArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the characteristic UUID
     */
    @Override
    public int getCharacteristicArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the descriptor UUID
     */
    @Override
    public int getDescriptorArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the permissions
     */
    @Override
    public int getPermissionsArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the properties
     */
    @Override
    public int getPropertiesArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the data
     */
    @Override
    public int getDataArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the tx phy
     */
    @Override
    public int getTxPhyArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the rx phy
     */
    @Override
    public int getRxPhyArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the phy options
     */
    @Override
    public int getPhyOptionsArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the request id
     */
    @Override
    public int getRequestIdArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the status
     */
    @Override
    public int getStatusArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the offset
     */
    @Override
    public int getOffsetArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the connection interval
     */
    @Override
    public int getConnectionIntervalArgIndex() {
        return Integer.MIN_VALUE;
    }

    /**
     * To be implemented if applicable.
     *
     * @return the index of the mtu
     */
    @Override
    public int getMtuArgIndex() {
        return Integer.MIN_VALUE;
    }
}
