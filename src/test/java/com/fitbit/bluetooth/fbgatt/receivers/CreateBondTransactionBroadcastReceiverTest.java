package com.fitbit.bluetooth.fbgatt.receivers;

import androidx.test.core.app.ApplicationProvider;
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
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowBluetoothDevice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class CreateBondTransactionBroadcastReceiverTest {
    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private final CreateBondTransactionInterface mockBondTransactionInterface = mock(CreateBondTransactionInterface.class);
    private final GattUtils mockGattUtils = mock(GattUtils.class);
    private final BluetoothDeviceProviderInterface mockDeviceProvider = mock(BluetoothDeviceProviderInterface.class);
    private final DeviceMatcher mockDeviceMatcher = mock(DeviceMatcher.class);

    private final Context context = ApplicationProvider.getApplicationContext();
    private final FitbitBluetoothDevice mockFitbitBluetoothDevice = mock(FitbitBluetoothDevice.class);
    private final BluetoothDevice bluetoothDevice = ShadowBluetoothDevice.newInstance(MOCK_ADDRESS);

    private final Intent mockIntent = new Intent();

    private final CreateBondTransactionBroadcastReceiver sut = new CreateBondTransactionBroadcastReceiver(mockBondTransactionInterface, mockGattUtils, mockDeviceProvider, mockDeviceMatcher);

    @Before
    public void before() {
        doReturn(mockFitbitBluetoothDevice).when(mockBondTransactionInterface).getDevice();
        doReturn(bluetoothDevice).when(mockDeviceProvider).getFromIntent(any(Intent.class));
        doReturn(true).when(mockDeviceMatcher).matchDevices(mockFitbitBluetoothDevice, bluetoothDevice);
        mockIntent.setAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mockIntent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
    }

    @Test
    public void shouldNotifyCallerOfSuccess() {
        mockIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDED);
        sut.onReceive(context, mockIntent);

        verify(mockBondTransactionInterface).bondSuccess();
        verify(mockBondTransactionInterface, never()).bondFailure();
    }

    @Test
    public void shouldNotifyCallerOfFailures() {
        mockIntent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_BONDING);
        mockIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);

        sut.onReceive(context, mockIntent);

        verify(mockBondTransactionInterface, never()).bondSuccess();
        verify(mockBondTransactionInterface).bondFailure();
    }

    @Test
    public void shouldNotNotifyCallerOfBondingState() {
        mockIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_BONDING);

        sut.onReceive(context, mockIntent);

        verify(mockBondTransactionInterface, never()).bondSuccess();
        verify(mockBondTransactionInterface, never()).bondFailure();
    }

    /**
     * Do not notify changes for [BluetoothDevice.BOND_NONE] when
     * last state was also [BluetoothDevice.BOND_NONE].
     */
    @Test
    public void shouldNotNotifyFailureForEdgeCase() {
        mockIntent.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
        mockIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);

        sut.onReceive(context, mockIntent);

        verify(mockBondTransactionInterface, never()).bondSuccess();
        verify(mockBondTransactionInterface, never()).bondFailure();
    }
}
