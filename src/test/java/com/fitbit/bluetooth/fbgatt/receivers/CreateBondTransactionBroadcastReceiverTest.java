package com.fitbit.bluetooth.fbgatt.receivers;

import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.tx.CreateBondTransactionInterface;
import com.fitbit.bluetooth.fbgatt.util.BluetoothDeviceProviderInterface;
import com.fitbit.bluetooth.fbgatt.util.DeviceMatcher;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateBondTransactionBroadcastReceiverTest {
    private final CreateBondTransactionInterface mockBondTransactionInterface = mock(CreateBondTransactionInterface.class);
    private final GattUtils mockGattUtils = mock(GattUtils.class);
    private final BluetoothDeviceProviderInterface mockDeviceProvider = mock(BluetoothDeviceProviderInterface.class);
    private final DeviceMatcher mockDeviceMatcher = mock(DeviceMatcher.class);

    private final Context mockContext = mock(Context.class);
    private final FitbitBluetoothDevice mockDevice = mock(FitbitBluetoothDevice.class);
    private final BluetoothDevice mockBtDevice = mock(BluetoothDevice.class);

    private final Intent mockIntent = mock(Intent.class);

    private final CreateBondTransactionBroadcastReceiver sut = new CreateBondTransactionBroadcastReceiver(mockBondTransactionInterface, mockGattUtils, mockDeviceProvider, mockDeviceMatcher);

    @Before
    public void before() {
        doReturn(mockDevice).when(mockBondTransactionInterface).getDevice();
        doReturn(mockBtDevice).when(mockDeviceProvider).getFromIntent(any(Intent.class));
        doReturn(true).when(mockDeviceMatcher).matchDevices(mockDevice, mockBtDevice);
        when(mockIntent.getAction()).thenReturn(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_NONE);
    }

    @Test
    public void shouldNotifyCallerOfSuccess() {
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_BONDED);

        sut.onReceive(mockContext, mockIntent);

        verify(mockBondTransactionInterface).bondSuccess();
        verify(mockBondTransactionInterface, never()).bondFailure();
    }

    @Test
    public void shouldNotifyCallerOfFailures() {
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_BONDING);
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_NONE);

        sut.onReceive(mockContext, mockIntent);

        verify(mockBondTransactionInterface, never()).bondSuccess();
        verify(mockBondTransactionInterface).bondFailure();
    }

    @Test
    public void shouldNotNotifyCallerOfBondingState() {
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_BONDING);

        sut.onReceive(mockContext, mockIntent);

        verify(mockBondTransactionInterface, never()).bondSuccess();
        verify(mockBondTransactionInterface, never()).bondFailure();
    }

    /**
     * Do not notify changes for [BluetoothDevice.BOND_NONE] when
     * last state was also [BluetoothDevice.BOND_NONE].
     */
    @Test
    public void shouldNotNotifyFailureForEdgeCase() {
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_NONE);
        when(mockIntent.getIntExtra(eq(BluetoothDevice.EXTRA_BOND_STATE), anyInt())).thenReturn(BluetoothDevice.BOND_NONE);

        sut.onReceive(mockContext, mockIntent);

        verify(mockBondTransactionInterface, never()).bondSuccess();
        verify(mockBondTransactionInterface, never()).bondFailure();
    }
}
