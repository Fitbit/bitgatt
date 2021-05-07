/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands.server;

import com.fitbit.bluetooth.fbgatt.commands.PluginCommandConfig;

import java.util.Iterator;

/**
 * Default parser that generates the config for GattServerTransactions.
 */
class GattServerTransactionConfigParser {
    public GattServerTransactionConfigInterface getTransactionConfig(PluginCommandConfig config, BluetoothArgsInterface btArgs) {
        Iterator<String> args = config.getArgs();

        int index = 0;
        String mac = null;
        String serviceUuid = null;
        String characteristicUuid = null;
        String descriptorUuid = null;
        int permissions = Integer.MIN_VALUE;
        int properties = Integer.MIN_VALUE;
        String dataString = null;
        String requestId = null;
        String status = null;
        String offset = null;

        while (args.hasNext()) {
            if (index == btArgs.getMacArgIndex()) {
                mac = args.next();
            } else if (index == btArgs.getServiceArgIndex()) {
                serviceUuid = args.next();
            } else if (index == btArgs.getCharacteristicArgIndex()) {
                characteristicUuid = args.next();
            } else if (index == btArgs.getDescriptorArgIndex()) {
                descriptorUuid = args.next();
            } else if (index == btArgs.getPermissionsArgIndex()) {
                permissions = Integer.parseInt(args.next());
            } else if (index == btArgs.getPropertiesArgIndex()) {
                properties = Integer.parseInt(args.next());
            } else if (index == btArgs.getDataArgIndex()) {
                dataString = args.next();
            } else if(index == btArgs.getRequestIdArgIndex()) {
                requestId = args.next();
            } else if(index == btArgs.getStatusArgIndex()) {
                status = args.next();
            } else if(index == btArgs.getOffsetArgIndex()) {
                offset = args.next();
            }

            index++;
        }

        byte[] data = (dataString != null) ? dataString.getBytes() : null;

        return new GattServerTransactionConfig(config.getServerConnection(), mac, serviceUuid, characteristicUuid, descriptorUuid, permissions, properties, data, requestId, status, offset);
    }
}
