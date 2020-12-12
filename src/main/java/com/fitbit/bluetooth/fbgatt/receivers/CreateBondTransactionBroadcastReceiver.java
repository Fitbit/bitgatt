package com.fitbit.bluetooth.fbgatt.receivers;

import com.fitbit.bluetooth.fbgatt.tx.CreateBondTransactionInterface;
import com.fitbit.bluetooth.fbgatt.util.BluetoothDeviceProvider;
import com.fitbit.bluetooth.fbgatt.util.BluetoothDeviceProviderInterface;
import com.fitbit.bluetooth.fbgatt.util.DeviceMatcher;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * BroadcastReceiver responsible for handling the updates of the BondState of a device.
 */
public class CreateBondTransactionBroadcastReceiver extends BroadcastReceiver {
    private final CreateBondTransactionInterface createBondTransactionInterface;
    private final GattUtils gattUtils;
    private final BluetoothDeviceProviderInterface deviceProvider;
    private final DeviceMatcher deviceMatcher;

    public CreateBondTransactionBroadcastReceiver(CreateBondTransactionInterface createBondTransactionInterface) {
        this(createBondTransactionInterface, new GattUtils(), new BluetoothDeviceProvider(), new DeviceMatcher());
    }

    @VisibleForTesting
    public CreateBondTransactionBroadcastReceiver(CreateBondTransactionInterface createBondTransactionInterface, GattUtils gattUtils, BluetoothDeviceProviderInterface deviceProvider, DeviceMatcher deviceMatcher) {
        this.createBondTransactionInterface = createBondTransactionInterface;
        this.gattUtils = gattUtils;
        this.deviceProvider = deviceProvider;
        this.deviceMatcher = deviceMatcher;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
            BluetoothDevice extraDevice = deviceProvider.getFromIntent(intent);
            /*
             * equals handled inside of {@link FitbitBluetoothDevice} for comparison to an
             * {@link BluetoothDevice}
             */
            //noinspection EqualsBetweenInconvertibleTypes
            if (deviceMatcher.matchDevices(createBondTransactionInterface.getDevice(), extraDevice)) {
                int oldState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                int newState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                Timber.d("[%s] Bond state changed from %s to %s",
                    createBondTransactionInterface.getDevice(),
                    gattUtils.getBondStateDescription(oldState),
                    gattUtils.getBondStateDescription(newState));
                switch (newState) {
                    case BluetoothDevice.BOND_BONDED:
                        Timber.d("[%s] Bond state changed to BONDED", createBondTransactionInterface.getDevice());
                        // success
                        createBondTransactionInterface.bondSuccess();
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Timber.w("[%s] Bond state changed to NONE", createBondTransactionInterface.getDevice());
                        // if we are here, we should go ahead and release the lock
                        // failure
                        if (newState != oldState) {
                            //  Only notify failures for different states.
                            createBondTransactionInterface.bondFailure();
                        }
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Timber.d("[%s] Bond state changed to BONDING", createBondTransactionInterface.getDevice());
                        // in progress
                        break;
                    default:
                        Timber.w("[%s] Bond state changed to UNKNOWN", createBondTransactionInterface.getDevice());
                        // could be error, but perhaps not, we don't know
                        break;
                }
            } else {
                Timber.i("[%s] Received Bond result, but for %s",
                    createBondTransactionInterface.getDevice(),
                    extraDevice);
            }
        }
    }
}
