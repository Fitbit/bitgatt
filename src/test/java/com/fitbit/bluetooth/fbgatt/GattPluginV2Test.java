/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tools.GattPluginCommandInterface;
import com.fitbit.bluetooth.fbgatt.tools.GattPluginV2;
import com.fitbit.bluetooth.fbgatt.tools.PluginConfig;
import android.content.Context;
import com.facebook.stetho.dumpapp.DumperContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class GattPluginV2Test {
    private final DumperContext mockDumperContext = mock(DumperContext.class);
    private final List<String> mockArgs = new ArrayList<>();
    private final PrintStream mockPrintStream = new PrintStream("success.out");
    private final PrintStream mockErrStream = new PrintStream("error.our");

    private final Context mockContext = mock(Context.class);
    private final FitbitGatt mockFitbitGatt = mock(FitbitGatt.class);
    private final PluginLoggerInterface mockLogger = mock(PluginLoggerInterface.class);
    private final FitbitBluetoothDevice.DevicePropertiesChangedCallback mockDeviceListener = mock(FitbitBluetoothDevice.DevicePropertiesChangedCallback.class);
    private final ConnectionEventListener mockConnectionListener = mock(ConnectionEventListener.class);


    private final GattPluginCommandInterface mockCommand = mock(GattPluginCommandInterface.class);
    private final String supportedCommandName = "h";

    private GattPluginV2 sut;

    public GattPluginV2Test() throws FileNotFoundException {
    }

    @Before
    public void setup() {
        doReturn(supportedCommandName).when(mockCommand).getShortName();
        doReturn(supportedCommandName).when(mockCommand).getFullName();

        doReturn(mockArgs).when(mockDumperContext).getArgsAsList();

        doReturn(mockPrintStream).when(mockDumperContext).getStdout();
        doReturn(mockErrStream).when(mockDumperContext).getStderr();

        doReturn(mock(GattServerConnection.class)).when(mockFitbitGatt).getServer();

        PluginConfig mockConfig = mock(PluginConfig.class);

        doReturn(mockContext).when(mockConfig).getContext();
        doReturn(mockFitbitGatt).when(mockConfig).getFitbitGatt();
        doReturn(mockLogger).when(mockConfig).getLogger();
        doReturn(mockDeviceListener).when(mockConfig).getDevicePropertiesListener();
        doReturn(mockConnectionListener).when(mockConfig).getConnectionEventListener();

        sut = new GattPluginV2(mockConfig, new ArrayList<GattPluginCommandInterface>() {{
            add(mockCommand);
        }});
    }

    @Test
    public void shouldRunSupportedCommands() {
        mockArgs.add("h");

        sut.dump(mockDumperContext);

        verify(mockCommand).run(any());
    }

    @Test
    @Ignore
    public void shouldNotRunUnsupportedCommands() {
        mockArgs.add("unsupported");

        sut.dump(mockDumperContext);

        verify(mockCommand, never()).run(any());
        verify(mockLogger).logError(any());
        verify(mockErrStream).println(any(String.class));
        verify(mockErrStream).flush();
    }

    @Test
    @Ignore
    public void shouldNotRunAnyCommandWhenCommandNotSpecified() {

        sut.dump(mockDumperContext);

        verify(mockCommand, never()).run(any());
        verify(mockLogger).logError(any());
        verify(mockErrStream).println(any(String.class));
        verify(mockErrStream).flush();
    }

}
