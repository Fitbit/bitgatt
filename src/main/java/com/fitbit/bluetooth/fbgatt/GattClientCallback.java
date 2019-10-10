/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import androidx.annotation.Nullable;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestGattClientPhyChangeTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Common callback for the gatt clients
 *
 * Created by iowens on 10/18/17.
 */

public class GattClientCallback extends BluetoothGattCallback {

    private static final long HOT_QUEUE_EMPTYING_TIME = 1000; // we want to allow just enough for the android system queue to flush
    private final Handler handler;
    private final List<GattClientListener> listeners;
    private final GattUtils gattUtils = new GattUtils();

    GattClientCallback() {
        super();
        this.listeners = Collections.synchronizedList(new ArrayList<>(4));
        Looper looper = FitbitGatt.getInstance().getFitbitGattAsyncOperationThread().getLooper();
        this.handler = new Handler(looper);
    }

    Handler getClientCallbackHandler(){
        return this.handler;
    }

    void addListener(GattClientListener gattListener) {
        synchronized (listeners) {
            if(!listeners.contains(gattListener)) {
                listeners.listIterator().add(gattListener);
            }
        }
    }

    void removeListener(GattClientListener gattListener) {
        synchronized (listeners) {
            Iterator<GattClientListener> listenerIterator = listeners.listIterator();
            while (listenerIterator.hasNext()) {
                GattClientListener listener = listenerIterator.next();
                if (listener.equals(gattListener)) {
                    listenerIterator.remove();
                    return;
                }
            }
        }
    }

    List<GattClientListener> getGattClientListeners(){
        ArrayList<GattClientListener> gattListeners = new ArrayList<>(listeners.size());
        gattListeners.addAll(listeners);
        return gattListeners;
    }

    private String getDeviceMacFromGatt(BluetoothGatt gatt) {
        return gattUtils.safeGetBtDeviceName(gatt);
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        Timber.v("[%s] onPhyUpdate: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if (listener.getDevice() != null && gatt != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onPhyUpdate(gatt, txPhy, rxPhy, status));
            }
        }
        final GattConnection conn;
        if (gatt != null) {
            conn = FitbitGatt.getInstance().getConnection(gatt.getDevice());
        } else {
            conn = null;
        }
        if(conn != null) {
            // since this is one of the events that could happen asynchronously, we will
            // need to iterate through our connection listeners
            handler.post(() -> {
                for (ConnectionEventListener asyncConnListener : conn.getConnectionEventListeners()) {
                    TransactionResult.Builder builder = new TransactionResult.Builder();
                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                    } else {
                        builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                    }
                    asyncConnListener.onPhyChanged(builder
                        .transactionName(RequestGattClientPhyChangeTransaction.NAME)
                        .txPhy(txPhy)
                        .rxPhy(rxPhy)
                        .gattState(conn.getGattState())
                        .responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal()).build(), conn);
                }
            });
        }
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyRead(gatt, txPhy, rxPhy, status);
        Timber.v("[%s] onPhyRead: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && gatt != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onPhyRead(gatt, txPhy, rxPhy, status));
            }
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Timber.v("[%s] onConnectionStateChange: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        Timber.d("[%s]Connection state: %s", getDeviceMacFromGatt(gatt), newState == BluetoothProfile.STATE_CONNECTED ? "Connected" : "Not-Connected");
        if(status != BluetoothGatt.GATT_SUCCESS) {
            Timber.i("[%s] The connection state may have changed in error", getDeviceMacFromGatt(gatt));
        }
        // gatt could be null if this is a mock
        GattConnection conn;
        if(gatt != null) {
            conn = FitbitGatt.getInstance().getConnection(gatt.getDevice());
            if(conn == null) {
                Timber.i("[%s] The instance that is receiving this callback for device %s is not in the map, ignoring",getDeviceMacFromGatt(gatt), gatt.getDevice());
                return;
            }
        } else {
            Timber.v("[%s] If GATT is null here, this must be a mock, otherwise there is a quite serious error", getDeviceMacFromGatt(gatt));
            FitbitBluetoothDevice device = new FitbitBluetoothDevice("02:00:00:00:00:00", "fooDevice");
            conn = new GattConnection(device, handler.getLooper());
            FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        }
        switch(newState) {
            case BluetoothProfile.STATE_DISCONNECTING: // never called by android
            case BluetoothProfile.STATE_DISCONNECTED:
                Timber.d("[%s] Disconnection reason: %s", getDeviceMacFromGatt(gatt), GattDisconnectReason.getReasonForCode(newState));
                /*
                 * this is tricky, once we get here, the tracker has disconnected, but we still must
                 * wait for supervision timeout length until we can connect again, so we will force
                 * the state into disconnecting here for the connection object that requires it and
                 * require a wait of 2 seconds ( the average supervision timeout ask ) before moving
                 * it to "disconnected."  This will reduce the number of 133 and bad state errors
                 * but if someone chooses the Android default 20s, or something greater than 2s
                 * it might cause difficulty, as such I will log what is going on so someone debugging
                 * can see what is occurring.  This will be posted before the callback on the main looper
                 * so that the state transition should appear normal in the callback.  We can change the
                 * hardcoded value when we can get information from the peripheral what the actual
                 * connection supervision timeout is.
                 *
                 * The other reason to wait here is because we want to block any other operations
                 * from trying to use this peripheral until the system has had time to work off the
                 * backed up bluetooth operations in the queue if there were any, this number is somewhat
                 * arbitrary, however we want for it to be less than the 60s timeout on a transaction
                 */
                if(gatt != null) {
                    Timber.w("[%s] disconnected, waiting %dms for full disconnection", getDeviceMacFromGatt(gatt), HOT_QUEUE_EMPTYING_TIME);
                    conn.setState(GattState.DISCONNECTING);
                    /*
                     * This is required so that we can cancel the connection attempt if one was pending
                     * and not wedge the queue since we always use direct connections.  If this is removed
                     * you must ensure that there is no way to have a gattConnect call that does not ever
                     * disconnect.
                     *
                     * In the event of a 133 ( cannot start new connection at conn_st: X error, or a series of them ) this will
                     * eventually unwedge the stack along with the gatt.close() that will occur in a second.
                     *
                     * Please see : https://wiki.fitbit.com/pages/viewpage.action?pageId=123374229 for a lot more detail
                     */
                    gatt.disconnect();
                    handler.postDelayed(() -> {
                        conn.gattRelease();
                        Timber.i("[%s] Full disconnection", getDeviceMacFromGatt(gatt));
                        conn.setState(GattState.DISCONNECTED);
                        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
                        copy.addAll(listeners);
                        for (GattClientListener listener : copy) {
                            if (listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                                // we'll want to use the fake state here so that we can wait for disconnecting and call it back
                                // normally once we are actually disconnected after the assumed supervision timeout.
                                handler.post(() -> listener.onConnectionStateChange(gatt, status, BluetoothProfile.STATE_DISCONNECTED));
                            }
                        }
                        // since this is one of the events that could happen asynchronously, we will
                        // need to iterate through our connection listeners, since this is a disconnection
                        // we will want to report failure so that upstream consumers don't get confused on a connection
                        // attempt
                        for (ConnectionEventListener asyncConnListener : conn.getConnectionEventListeners()) {
                            asyncConnListener.onClientConnectionStateChanged(new TransactionResult.Builder()
                                    .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                                    .gattState(conn.getGattState())
                                    .responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal()).build(), conn);
                        }
                    }, HOT_QUEUE_EMPTYING_TIME);
                } else {
                    Timber.v("[%s] Gatt was null, returning disconnected state immediately", getDeviceMacFromGatt(gatt));
                    ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
                    copy.addAll(listeners);
                    for (GattClientListener listener : copy) {
                        handler.post(() -> listener.onConnectionStateChange(null, status, BluetoothProfile.STATE_DISCONNECTED));
                    }
                }
                break;
            case BluetoothProfile.STATE_CONNECTED:
                ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
                copy.addAll(listeners);
                for (GattClientListener listener : copy) {
                    if(gatt == null) {
                        handler.post(() -> listener.onConnectionStateChange(null, status, BluetoothProfile.STATE_CONNECTED));
                    } else if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                        handler.post(() -> listener.onConnectionStateChange(gatt, status, BluetoothProfile.STATE_CONNECTED));
                    } else {
                        Timber.v("[%s] We should never get here, but if we do it is not an exception", getDeviceMacFromGatt(gatt));
                    }
                }
                // since this is one of the events that could happen asynchronously, we will
                // need to iterate through our connection listeners, since this is a disconnection
                // we will want to report success so that upstream consumers don't get confused,
                // or mixed signals on a connection attempt
                handler.post(() -> {
                    for(ConnectionEventListener asyncConnListener : conn.getConnectionEventListeners()) {
                        handler.post(() -> asyncConnListener.onClientConnectionStateChanged(new TransactionResult.Builder()
                            .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS)
                            .gattState(conn.getGattState())
                            .responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal()).build(), conn));
                    }
                });
                break;
            default:
                throw new IllegalStateException(String.format(Locale.ENGLISH,
                        "[%s] The state returned was something unexpected",
                        getDeviceMacFromGatt(gatt)));
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Timber.v("[%s] onServicesDiscovered: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onServicesDiscovered(gatt, status));
            }
        }
        GattConnection conn = FitbitGatt.getInstance().getConnection(gatt.getDevice());
        if(conn != null) {
            List<BluetoothGattService> discoveredServices = gatt.getServices();
            // since this is one of the events that could happen asynchronously, we will
            // need to iterate through our connection listeners
            handler.post(() -> {
                for (ConnectionEventListener asyncConnListener : conn.getConnectionEventListeners()) {
                    TransactionResult.Builder builder = new TransactionResult.Builder();
                    builder.transactionName(GattClientDiscoverServicesTransaction.NAME);
                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                    } else {
                        builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                    }
                    asyncConnListener.onServicesDiscovered(builder
                        .transactionName(GattClientDiscoverServicesTransaction.NAME)
                        .serverServices(discoveredServices)
                        .gattState(conn.getGattState())
                        .responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal()).build(), conn);
                }
            });
        }
    }

    // for Characteristics and Descriptors, they are backed by c level objects and the references
    // are passed up to Java via JNI.  This means that the values can change, so we must copy through
    // here
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Timber.v("[%s] onCharacteristicRead: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onCharacteristicRead(gatt, new GattUtils().copyCharacteristic(characteristic), status));
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Timber.v("[%s] onCharacteristicWrite: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onCharacteristicWrite(gatt, new GattUtils().copyCharacteristic(characteristic), status));
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Timber.d("[%s] onCharacteristicChanged: [Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        BluetoothGattCharacteristicCopy copyOfCharacteristic = new GattUtils().copyCharacteristic(characteristic);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onCharacteristicChanged(gatt, copyOfCharacteristic));
            }
        }
        GattConnection conn = FitbitGatt.getInstance().getConnection(gatt.getDevice());
        if(conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromGatt(gatt));
        } else {
            handler.post(() -> {
                for(ConnectionEventListener asyncListener: conn.getConnectionEventListeners()) {
                    // since this is async, the result status is irrelevant so it will always be
                    // success because we received this data, as this is a snapshot of a live object
                    // we will need to copy the values into the tx result
                    TransactionResult result = new TransactionResult.Builder()
                        .gattState(conn.getGattState())
                        .characteristicUuid(copyOfCharacteristic.getUuid())
                        .data(copyOfCharacteristic.getValue())
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                    asyncListener.onClientCharacteristicChanged(result, conn);
                }
            });
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        Timber.v("[%s] onDescriptorRead: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onDescriptorRead(gatt, new GattUtils().copyDescriptor(descriptor), status));
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Timber.v("[%s] onDescriptorWrite: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onDescriptorWrite(gatt, new GattUtils().copyDescriptor(descriptor), status));
            }
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        Timber.v("[%s] onReliableWriteCompleted: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onReliableWriteCompleted(gatt, status));
            }
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        Timber.v("[%s] onReadRemoteRssi: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onReadRemoteRssi(gatt, rssi, status));
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        Timber.v("[%s] onMtuChanged: Gatt Response Status %s", getDeviceMacFromGatt(gatt), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromGatt(gatt), Thread.currentThread().getName());
        ArrayList<GattClientListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattClientListener listener : copy) {
            if(listener.getDevice() != null && listener.getDevice().equals(gatt.getDevice())) {
                handler.post(() -> listener.onMtuChanged(gatt, mtu, status));
            }
        }
        GattConnection conn = FitbitGatt.getInstance().getConnection(gatt.getDevice());
        if(conn != null) {
            // since this is one of the events that could happen asynchronously, we will
            // need to iterate through our connection listeners
            handler.post(() -> {
                for (ConnectionEventListener asyncConnListener : conn.getConnectionEventListeners()) {
                    TransactionResult.Builder builder = new TransactionResult.Builder();
                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        builder.resultStatus(TransactionResult.TransactionResultStatus.SUCCESS);
                    } else {
                        builder.resultStatus(TransactionResult.TransactionResultStatus.FAILURE);
                    }
                    asyncConnListener.onMtuChanged(builder
                        .transactionName(RequestMtuGattTransaction.NAME)
                        .mtu(mtu)
                        .gattState(conn.getGattState())
                        .responseStatus(GattDisconnectReason.getReasonForCode(status).ordinal()).build(), conn);
                }
            });
        }
    }
}
