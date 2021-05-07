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

import com.fitbit.bluetooth.fbgatt.commands.client.CloseGattClientCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.FindNearbyDevicesBgGattClientCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.FindNearbyDevicesGattClientCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.GattClientConnectCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.GattClientDisconnectCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.GattClientDiscoverServicesCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.HelpCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ReadGattClientCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ReadGattClientDescriptorCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ReadGattClientPhyCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ReadGattClientRssiCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ReadGattLibVersionCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ReadNumGattActiveConnectionsCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.RefreshGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.RequestGattClientConnectionIntervalCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.RequestGattClientMtuCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.RequestGattClientPhyCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.SetJsonFormatClientCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.ShowRemoteServicesCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.StartGattCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.StopBackgroundScanGattClientCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.SubscribeToGattClientCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.UnsubscribeFromGattClientCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.WriteGattCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.client.WriteGattDescriptorCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.AddLocalGattServerCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.AddLocalGattServerCharacteristicDescriptorCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.AddLocalGattServerServiceCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.ClearLocalGattServerServicesCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.GattServerConnectCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.GattServerDisconnectCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.NotifyGattServerCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.ReadLocalGattServerCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.ReadLocalGattServerCharacteristicDescriptorCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.RemoveGattServerServiceCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.ShowGattServerServiceCharacteristicsCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.ShowGattServerServicesCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.WriteGattServerResponseCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.WriteLocalGattServerCharacteristicCommand;
import com.fitbit.bluetooth.fbgatt.commands.server.WriteLocalGattServerCharacteristicDescriptorCommand;
import com.fitbit.bluetooth.fbgatt.utils.CommonGattServerUtils;
import com.fitbit.bluetooth.fbgatt.utils.OutputFormatStateController;

import java.util.ArrayList;

/**
 * Default generator class for the Stetho commands currently supported.
 */
class CommandGenerator {
    private final PluginConfig config;
    private final OutputFormatStateController jsonFormatController;

    CommandGenerator(PluginConfig config, OutputFormatStateController jsonFormatController) {
        this.config = config;
        this.jsonFormatController = jsonFormatController;
    }

    /**
     * Method that creates the list of supported Stetho commands.
     *
     * @return [ArrayList<GattPluginCommandInterface>] containing all the available Stetho commands.
     */
    ArrayList<GattPluginCommandInterface> generateCommands() {
        ArrayList<GattPluginCommandInterface> commands = new ArrayList<>();
        commands.add(new StartGattCommand(config.getContext(), config.getFitbitGatt(), config.getLogger()));

        commands.add(new AddLocalGattServerServiceCommand(config.getLogger(), new CommonGattServerUtils(config)));
        commands.add(new AddLocalGattServerCharacteristicCommand(config.getLogger()));
        commands.add(new AddLocalGattServerCharacteristicDescriptorCommand(config.getLogger()));

        commands.add(new WriteLocalGattServerCharacteristicCommand(config.getLogger()));
        commands.add(new WriteLocalGattServerCharacteristicDescriptorCommand(config.getLogger()));

        commands.add(new ReadLocalGattServerCharacteristicCommand(config.getLogger()));
        commands.add(new ReadLocalGattServerCharacteristicDescriptorCommand(config.getLogger()));

        commands.add(new ClearLocalGattServerServicesCommand(config.getLogger()));
        commands.add(new GattServerConnectCommand(config.getFitbitGatt(), config.getLogger()));
        commands.add(new GattServerDisconnectCommand(config.getFitbitGatt(), config.getDevicePropertiesListener(), config.getLogger()));
        commands.add(new NotifyGattServerCharacteristicCommand(config.getFitbitGatt(), config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new ShowGattServerServicesCommand(config.getLogger()));
        commands.add(new ShowGattServerServiceCharacteristicsCommand(config.getLogger()));
        commands.add(new RemoveGattServerServiceCommand(config.getLogger()));
        commands.add(new WriteGattServerResponseCommand(config.getFitbitGatt(), config.getLogger()));

        commands.add(new SetJsonFormatClientCommand(jsonFormatController, config.getLogger()));
        commands.add(new FindNearbyDevicesGattClientCommand(config.getFitbitGatt(), config.getLogger()));
        commands.add(new FindNearbyDevicesBgGattClientCommand(config.getFitbitGatt(), config.getLogger()));
        commands.add(new StopBackgroundScanGattClientCommand(config.getFitbitGatt(), config.getLogger()));
        commands.add(new CloseGattClientCommand(config.getDevicePropertiesListener(), config.getLogger()));
        commands.add(new GattClientDiscoverServicesCommand(config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new GattClientConnectCommand(config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new GattClientDisconnectCommand(config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new RequestGattClientPhyCommand(config.getLogger()));
        commands.add(new ReadGattClientCharacteristicCommand(config.getDevicePropertiesListener(), config.getLogger()));
        commands.add(new ReadGattClientDescriptorCommand(config.getDevicePropertiesListener(), config.getLogger()));
        commands.add(new ReadGattClientPhyCommand(config.getLogger()));
        commands.add(new ReadGattClientRssiCommand(config.getDevicePropertiesListener(), config.getLogger()));
        commands.add(new ReadNumGattActiveConnectionsCommand(config.getContext(), config.getLogger()));
        commands.add(new ReadGattLibVersionCommand(config.getLogger()));
        commands.add(new RequestGattClientConnectionIntervalCommand(config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new RequestGattClientMtuCommand(config.getLogger()));
        commands.add(new SubscribeToGattClientCharacteristicCommand(config.getFitbitGatt(), config.getLogger(), config.getDevicePropertiesListener(), config.getConnectionEventListener()));
        commands.add(new UnsubscribeFromGattClientCharacteristicCommand(config.getFitbitGatt(), config.getLogger(), config.getDevicePropertiesListener(), config.getConnectionEventListener()));
        commands.add(new WriteGattCharacteristicCommand(config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new WriteGattDescriptorCommand(config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new RefreshGattCommand(config.getFitbitGatt(), config.getLogger(), config.getDevicePropertiesListener()));
        commands.add(new ShowRemoteServicesCommand(config.getFitbitGatt(), config.getLogger(), config.getDevicePropertiesListener()));

        commands.add(new HelpCommand(commands, config.getLogger()));

        return commands;
    }
}
