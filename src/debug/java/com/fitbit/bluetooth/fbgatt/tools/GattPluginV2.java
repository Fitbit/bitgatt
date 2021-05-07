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

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;
import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.utils.GattConnectionUtils;
import com.fitbit.bluetooth.fbgatt.utils.OutputFormatStateController;
import android.content.Context;
import com.facebook.stetho.dumpapp.ArgsHelper;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;
import java.util.ArrayList;
import java.util.Iterator;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

/**
 * Implementation of the Stetho Gatt plugin.
 */
public class GattPluginV2 implements DumperPlugin {
    protected final PluginConfig config;
    private final ArrayList<GattPluginCommandInterface> supportedCommands;
    protected final GattConnectionUtils connectionUtils = new GattConnectionUtils();
    protected boolean isJsonFormat = false;
    protected OutputFormatStateController jsonFormatController = new OutputFormatStateController() {
        @Override
        public void setJsonFormat(boolean isJsonFormat) {
            GattPluginV2.this.isJsonFormat = isJsonFormat;
        }

        @Override
        public boolean isJsonFormat() {
            return GattPluginV2.this.isJsonFormat;
        }
    };

    public GattPluginV2(PluginConfig config) {
        this.config = config;
        this.supportedCommands = new CommandGenerator(config, jsonFormatController).generateCommands();

        config.getFitbitGatt().registerGattEventListener(new FitbitGatt.FitbitGattCallback() {
            @Override
            public void onBluetoothPeripheralDiscovered(GattConnection connection) {
                connectionUtils.onClientDiscovered(connection);
            }

            @Override
            public void onBluetoothPeripheralDisconnected(GattConnection connection) {
                connectionUtils.onClientDisconnected(connection);
            }

            @Override
            public void onScanStarted() {

            }

            @Override
            public void onScanStopped() {

            }

            @Override
            public void onScannerInitError(BitGattStartException error) {

            }

            @Override
            public void onPendingIntentScanStopped() {

            }

            @Override
            public void onPendingIntentScanStarted() {

            }

            @Override
            public void onBluetoothOff() {

            }

            @Override
            public void onBluetoothOn() {

            }

            @Override
            public void onBluetoothTurningOn() {

            }

            @Override
            public void onBluetoothTurningOff() {

            }

            @Override
            public void onGattServerStarted(GattServerConnection serverConnection) {

            }

            @Override
            public void onGattServerStartError(BitGattStartException error) {

            }

            @Override
            public void onGattClientStarted() {

            }

            @Override
            public void onGattClientStartError(BitGattStartException error) {

            }
        });
    }

    @VisibleForTesting
    public GattPluginV2(PluginConfig config, ArrayList<GattPluginCommandInterface> supportedCommands) {
        this.config = config;
        this.supportedCommands = supportedCommands;
    }

    @Override
    public String getName() {
        return "gatt";
    }

    @Override
    public void dump(DumperContext dumpContext) {
        Iterator<String> args = dumpContext.getArgsAsList().iterator();
        String commandName = ArgsHelper.nextOptionalArg(args, null);

        GattPluginCommandInterface command = null;
        for (GattPluginCommandInterface c : supportedCommands) {
            if (c.getFullName().equals(commandName) || c.getShortName().equals(commandName)) {
                command = c;
                break;
            }
        }

        PluginLoggerInterface logger = config.getLogger();
        logger.logMsg(String.format("Received command: %s", commandName));

        ConsumerInterface consumer = getDefaultConsumer(dumpContext);

        if (command == null) {
            Exception exception = new UnsupportedOperationException("Command not implemented.");
            consumer.consumeError(exception);
        } else {
            FitbitGatt fitbitGatt = config.getFitbitGatt();
            if (fitbitGatt.getServer() == null) {
                Exception exception = new IllegalStateException("GattServerConnection cannot be null");
                consumer.consumeError(exception);
                return;
            }

            command.run(new PluginCommandConfig() {
                @NonNull
                @Override
                public GattServerConnection getServerConnection() {
                    return fitbitGatt.getServer();
                }

                @NonNull
                @Override
                public GattConnectionUtils getConnectionUtils() {
                    return connectionUtils;
                }

                @Override
                public Iterator<String> getArgs() {
                    return args;
                }

                @NonNull
                @Override
                public ConsumerInterface getConsumer() {
                    return consumer;
                }

                @Override
                public boolean isJsonFormat() {
                    return jsonFormatController.isJsonFormat();
                }
            });
        }
    }

    protected ConsumerInterface getDefaultConsumer(DumperContext dumperContext) {
        return new DumperConsumer(dumperContext, getLogger());
    }

    protected Context getContext() {
        return config.getContext();
    }

    protected PluginLoggerInterface getLogger() {
        return config.getLogger();
    }
}
