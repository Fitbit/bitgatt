/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattCharacteristicCopy;
import com.fitbit.bluetooth.fbgatt.btcopies.BluetoothGattDescriptorCopy;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;

/**
 * Manage gatt server callbacks
 * <p>
 * Created by iowens on 10/17/17.
 */

class GattServerCallback extends BluetoothGattServerCallback {

    private final @NonNull Handler handler;

    private final List<GattServerListener> listeners;
    private GattUtils gattUtils = new GattUtils();

    GattServerCallback() {
        super();
        this.listeners = Collections.synchronizedList(new ArrayList<>(4));
        Looper looper = FitbitGatt.getInstance().getFitbitGattAsyncOperationThread().getLooper();
        this.handler = new Handler(looper);
    }

    void addListener(GattServerListener gattListener) {
        synchronized (listeners) {
            if (!listeners.contains(gattListener)) {
                listeners.add(gattListener);
            }
        }
    }

    void removeListener(GattServerListener gattListener) {
        synchronized (listeners) {
            Iterator<GattServerListener> listenerIterator = listeners.listIterator();
            while (listenerIterator.hasNext()) {
                GattServerListener listener = listenerIterator.next();
                if (listener.equals(gattListener)) {
                    listenerIterator.remove();
                    return;
                }
            }
        }
    }

    void unregisterAll() {
        this.listeners.clear();
    }

    private String getDeviceMacFromDevice(BluetoothDevice device) {
        return gattUtils.debugSafeGetBtDeviceName(device);
    }

    @NonNull Handler getServerCallbackHandler(){
        return this.handler;
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);
        Timber.v("[%s] onConnectionStateChange: Gatt Response Status %s", getDeviceMacFromDevice(device), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerConnectionStateChange(device, status, newState));
        }
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        GattConnection gattConnection = FitbitGatt.getInstance().getConnection(device);

        if (conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromDevice(device));
        } else {
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTING: // never called by android
                case BluetoothProfile.STATE_DISCONNECTED:
                    for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                        // since this is async, the result status is irrelevant so it will always be
                        // success because we received this data
                        TransactionResult result = new TransactionResult.Builder()
                                .gattState(conn.getGattState())
                                .responseStatus(status)
                                .resultStatus(TransactionResult.TransactionResultStatus.FAILURE).build();
                        handler.post(() -> asyncListener.onServerConnectionStateChanged(device, result, conn));
                    }

                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    if (gattConnection == null) {
                        FitbitGatt.getInstance().addConnectedDevice(device);
                    }
                    for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                        // since this is async, the result status is irrelevant so it will always be
                        // success because we received this data
                        TransactionResult result = new TransactionResult.Builder()
                                .gattState(conn.getGattState())
                                .responseStatus(status)
                                .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                        handler.post(() -> asyncListener.onServerConnectionStateChanged(device, result, conn));
                    }

                    break;
                default:
                    Timber.w("[%s] Unknown state %d", getDeviceMacFromDevice(device), newState);
            }
        }
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Timber.v("[%s] onServiceAdded: Gatt Response Status %s", Build.MODEL , GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", Build.MODEL, Thread.currentThread().getName());
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerServiceAdded(status, service));
        }
    }
    // for Characteristics and Descriptors, they are backed by c level objects and the references
    // are passed up to Java via JNI.  This means that the values can change, so we must copy through
    // here

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        Timber.d("[%s] onCharacteristicReadRequest: [Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        if (conn != null) {
            if (ifNotHostingCharacteristicRespondError(conn, characteristic, device, requestId, offset)) {
                // we returned an error to the requester
                return;
            }
        }
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        BluetoothGattCharacteristicCopy copyOfCharacteristic = new GattUtils().copyCharacteristic(characteristic);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerCharacteristicReadRequest(device, requestId, offset, copyOfCharacteristic));
        }
        if (conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromDevice(device));
        } else {
            // if there are no listeners for this event, then we will need to send a response
            // on the handler to prevent the peripheral from disconnecting
            if (conn.getConnectionEventListeners().isEmpty() && copy.isEmpty()) {
                Timber.e("[%s] You must listen and respond to server read and write requests, responding with error.", getDeviceMacFromDevice(device));
                handler.post(() -> {
                    try {
                        conn.getServer().sendResponse(device, requestId, GattStatus.GATT_ERROR.getCode(), offset, new byte[0]);
                    } catch (NullPointerException e) {
                        Timber.w(e, "[%s] Looks like BluetoothGattServer#sendResponse(...) can run into the unboxing bug also.  No response sent.", getDeviceMacFromDevice(device));
                    }
                });
            } else {
                for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                    // since this is async, the result status is irrelevant so it will always be
                    // success because we received this data
                    UUID serviceUuid = null;
                    if (characteristic.getService() != null) {
                        serviceUuid = characteristic.getService().getUuid();
                    }
                    TransactionResult result = new TransactionResult.Builder()
                            .gattState(conn.getGattState())
                            .requestId(requestId)
                            .offset(offset)
                            .serviceUuid(serviceUuid)
                            .characteristicUuid(copyOfCharacteristic == null ? null : copyOfCharacteristic.getUuid())
                            .data(copyOfCharacteristic == null ? null : copyOfCharacteristic.getValue())
                            .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                    handler.post(() -> asyncListener.onServerCharacteristicReadRequest(device, result, conn));
                }
            }
        }
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        Timber.d("[%s] onCharacteristicWriteRequest: [Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        if (conn != null && ifNotHostingCharacteristicRespondError(conn, characteristic, device, requestId, offset)) {
            // we returned an error to the requester, or if the gatt server is null we returned nothing and must be mocking which we will handle below
            return;
        }
        Timber.v("[%s] You must respond to this request", getDeviceMacFromDevice(device));
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        BluetoothGattCharacteristicCopy copyOfCharacteristic = new GattUtils().copyCharacteristic(characteristic);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerCharacteristicWriteRequest(device, requestId, copyOfCharacteristic, preparedWrite, responseNeeded, offset, value));
        }
        if (conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromDevice(device));
        } else {
            if (conn.getConnectionEventListeners().isEmpty() && copy.isEmpty()) {
                Timber.e("You must listen and respond to server read and write requests, responding with error.");
                handler.post(() -> {
                    try {
                        conn.getServer().sendResponse(device, requestId, GattStatus.GATT_ERROR.getCode(), offset, new byte[0]);
                    } catch (NullPointerException e) {
                        Timber.w(e, "[%s] Looks like BluetoothGattServer#sendResponse(...) can run into the unboxing bug also.  No response sent, peripheral may disconnect.", getDeviceMacFromDevice(device));
                    }
                });
            } else {
                for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                    // since this is async, the result status is irrelevant so it will always be
                    // success because we received this data
                    UUID serviceUuid = null;
                    if (characteristic.getService() != null) {
                        serviceUuid = characteristic.getService().getUuid();
                    }
                    TransactionResult result = new TransactionResult.Builder()
                            .gattState(conn.getGattState())
                            .characteristicUuid(copyOfCharacteristic == null ? null : copyOfCharacteristic.getUuid())
                            .serviceUuid(serviceUuid)
                            .data(value)
                            .requestId(requestId)
                            .offset(offset)
                            .preparedWrite(preparedWrite)
                            .responseRequired(responseNeeded)
                            .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                    handler.post(() -> asyncListener.onServerCharacteristicWriteRequest(device, result, conn));
                }
            }
        }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        Timber.d("[%s] onDescriptorReadRequest: [Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        if (conn != null) {
            if (ifNotHostingDescriptorRespondError(conn, descriptor, device, requestId, offset)) {
                // we returned an error to the requester, or if the gatt server is null we returned nothing and must be mocking which we will handle below
                return;
            }
        }
        Timber.v("[%s] You must respond to this request", getDeviceMacFromDevice(device));
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        BluetoothGattDescriptorCopy copyOfDescriptor = new GattUtils().copyDescriptor(descriptor);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerDescriptorReadRequest(device, requestId, offset, copyOfDescriptor));
        }
        if (conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromDevice(device));
        } else {
            if (conn.getConnectionEventListeners().isEmpty() && copy.isEmpty()) {
                Timber.e("[%s] You must listen and respond to server read and write requests, responding with error.", getDeviceMacFromDevice(device));
                handler.post(() -> {
                    try {
                        conn.getServer().sendResponse(device, requestId, GattStatus.GATT_ERROR.getCode(), offset, new byte[0]);
                    } catch (NullPointerException e) {
                        Timber.w(e, "[%s] Looks like BluetoothGattServer#sendResponse(...) can run into the unboxing bug also.  No response sent, peripheral may disconnect.", getDeviceMacFromDevice(device));
                    }
                });
            } else {
                for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                    // since this is async, the result status is irrelevant so it will always be
                    // success because we received this data
                    UUID characteristicUuid = null;
                    UUID serviceUuid = null;
                    if (descriptor.getCharacteristic() != null) {
                        characteristicUuid = descriptor.getCharacteristic().getUuid();
                        if (descriptor.getCharacteristic().getService() != null) {
                            serviceUuid = descriptor.getCharacteristic().getService().getUuid();
                        }
                    }
                    TransactionResult result = new TransactionResult.Builder()
                            .gattState(conn.getGattState())
                            .serviceUuid(serviceUuid)
                            .characteristicUuid(characteristicUuid)
                            .descriptorUuid(UUID.fromString(descriptor.getUuid().toString()))
                            .data(copyOfDescriptor == null ? new byte[]{} : copyOfDescriptor.getValue())
                            .requestId(requestId)
                            .offset(offset)
                            .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                    handler.post(() -> asyncListener.onServerDescriptorReadRequest(device, result, conn));
                }
            }
        }
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        Timber.d("[%s] onDescriptorWriteRequest: [Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        if (conn != null) {
            if (ifNotHostingDescriptorRespondError(conn, descriptor, device, requestId, offset)) {
                // we returned an error to the requester
                return;
            }
        }
        Timber.v("[%s] You must respond to this request", getDeviceMacFromDevice(device));
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        BluetoothGattDescriptorCopy copyOfDescriptor = new GattUtils().copyDescriptor(descriptor);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerDescriptorWriteRequest(device, requestId, copyOfDescriptor, preparedWrite, responseNeeded, offset, value));
        }
        if (conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromDevice(device));
        } else {
            if (conn.getConnectionEventListeners().isEmpty() && copy.isEmpty()) {
                Timber.e("[%s] You must listen and respond to server read and write requests, responding with error.", getDeviceMacFromDevice(device));
                handler.post(() -> {
                    try {
                        conn.getServer().sendResponse(device, requestId, GattStatus.GATT_ERROR.getCode(), offset, new byte[0]);
                    } catch (NullPointerException e) {
                        Timber.w(e, "[%s] Looks like BluetoothGattServer#sendResponse(...) can run into the unboxing bug also.  No response sent, peripheral may disconnect.", getDeviceMacFromDevice(device));
                    }
                });
            } else {
                for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                    // since this is async, the result status is irrelevant so it will always be
                    // success because we received this data
                    UUID characteristicUuid = null;
                    UUID serviceUuid = null;
                    if (descriptor.getCharacteristic() != null) {
                        characteristicUuid = descriptor.getCharacteristic().getUuid();
                        if (descriptor.getCharacteristic().getService() != null) {
                            serviceUuid = descriptor.getCharacteristic().getService().getUuid();
                        }
                    }
                    TransactionResult result = new TransactionResult.Builder()
                            .gattState(conn.getGattState())
                            .serviceUuid(serviceUuid)
                            .characteristicUuid(characteristicUuid)
                            .descriptorUuid(copyOfDescriptor == null ? null : copyOfDescriptor.getUuid())
                            .data(value)
                            .requestId(requestId)
                            .offset(offset)
                            .preparedWrite(preparedWrite)
                            .responseRequired(responseNeeded)
                            .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                    handler.post(() -> asyncListener.onServerDescriptorWriteRequest(device, result, conn));
                }
            }
        }
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        super.onExecuteWrite(device, requestId, execute);
        Timber.d("[%s] onExecuteWrite: [Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        Timber.v("[%s] You must respond to this request", getDeviceMacFromDevice(device));
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerExecuteWrite(device, requestId, execute));
        }
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
        Timber.v("[%s] onNotificationSent: Gatt Response Status %s", getDeviceMacFromDevice(device), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerNotificationSent(device, GattStatus.getStatusForCode(status).ordinal()));
        }
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
        Timber.d("[%s] onMtuChanged: [Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerMtuChanged(device, mtu));
        }
        GattServerConnection conn = FitbitGatt.getInstance().getServer();
        if (conn == null) {
            Timber.v("[%s] Gatt was null, we could be mocking, if so we can't notify async", getDeviceMacFromDevice(device));
        } else {
            for (ServerConnectionEventListener asyncListener : conn.getConnectionEventListeners()) {
                // since this is async, the result status is irrelevant so it will always be
                // success because we received this data
                TransactionResult result = new TransactionResult.Builder()
                        .gattState(conn.getGattState())
                        .mtu(mtu)
                        .resultStatus(TransactionResult.TransactionResultStatus.SUCCESS).build();
                handler.post(() -> asyncListener.onServerMtuChanged(device, result, conn));
            }
        }
    }

    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(device, txPhy, rxPhy, status);
        Timber.v("[%s] onPhyUpdate: Gatt Response Status %s", getDeviceMacFromDevice(device), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerPhyUpdate(device, txPhy, rxPhy, status));
        }
    }

    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyRead(device, txPhy, rxPhy, status);
        Timber.v("[%s] onPhyRead: Gatt Response Status %s", getDeviceMacFromDevice(device), GattStatus.getStatusForCode(status));
        Timber.d("[%s][Threading] Originally called on thread : %s", getDeviceMacFromDevice(device), Thread.currentThread().getName());
        ArrayList<GattServerListener> copy = new ArrayList<>(listeners.size());
        copy.addAll(listeners);
        for (GattServerListener listener : copy) {
            handler.post(() -> listener.onServerPhyRead(device, txPhy, rxPhy, status));
        }
    }

    /**
     * If the Gatt server is not null, this will return an error to the caller and return true if the
     * remote characteristic's service is null, or if the remote characteristic's service's UUID is null,
     * or if the remote characteristic's service's UUID is null,
     * or if the hosted characteristic is not found, or if the hosted characteristic's service is not found,
     * or if the hosted characteristic's hosted service is not found.
     * <p>
     * If the gatt server is null, this will return true even though we did not actually send the response to allow the null
     * gatt server mocking code to take over if we are in mock mode
     *
     * @param conn           The gatt server connection
     * @param characteristic The remote characteristic provided in the characteristic write or read request
     * @param device         The bluetooth device
     * @param requestId      The request id
     * @param offset         The offset
     * @return true if the gatt server is null, or if one of the chained remote characteristic,
     * service, or hosted chained characteristic, services is null
     */

    private boolean ifNotHostingCharacteristicRespondError(GattServerConnection conn, BluetoothGattCharacteristic characteristic, BluetoothDevice device, int requestId, int offset) {
        return checkCharacteristicAndCharacteristicService(conn, device, characteristic, requestId, offset);
    }

    /**
     * If the Gatt server is not null, this will return an error to the caller and return true if the
     * remote descriptor's characteristic is null, or if the remote descriptor's characteristic's service is null,
     * or if the remote descriptor's characteristic's UUID is null, or if the remote descriptor's UUID is null,
     * or if the remote descriptor's characteristic's UUID is null, or the remote descriptor's characteristic's service's UUID is null,
     * or if the hosted descriptor is not found, or if the hosted descriptor's hosted characteristic is not found,
     * or if the hosted descriptor's hosted characteristic's hosted service is not found.
     * <p>
     * If the gatt server is null, this will return true even though we did not actually send the response to allow the null
     * gatt server mocking code to take over if we are in mock mode
     *
     * @param conn       The gatt server connection
     * @param descriptor The remote descriptor provided in the descriptor write or read request
     * @param device     The bluetooth device
     * @param requestId  The request id
     * @param offset     The offset
     * @return true if the gatt server is null, or if one of the chained remote descriptor,
     * characteristic, service, or hosted chained descriptor, characteristic, services is null
     */

    private boolean ifNotHostingDescriptorRespondError(GattServerConnection conn, BluetoothGattDescriptor descriptor, BluetoothDevice device, int requestId, int offset) {
        UUID descUuid = descriptor.getUuid();
        if (conn.getServer() == null) {
            Timber.w("[%s] The server instance was null!", getDeviceMacFromDevice(device));
            return false;
        }

        if (checkCharacteristicAndCharacteristicService(conn, device, descriptor.getCharacteristic(), requestId, offset)) {
            return true;
        }
        // if we can get here, we know that the characteristic and service are both non-null
        BluetoothGattService hostedService = descriptor.getCharacteristic().getService();
        BluetoothGattCharacteristic hostedCharacteristic = hostedService.getCharacteristic(descriptor.getCharacteristic().getUuid());
        if (hostedCharacteristic.getDescriptor(descUuid) == null) {
            Timber.w("[%s] The expected descriptor wasn't hosted! Returning error", device);
            returnErrorToRemoteClient(conn, device, requestId, offset);
            return true;
        }
        return false;
    }

    private boolean checkCharacteristicAndCharacteristicService(GattServerConnection conn, BluetoothDevice device, @Nullable BluetoothGattCharacteristic characteristic, int requestId, int offset) {
        BluetoothGattServer server = conn.getServer();
        if (characteristic == null) {
            Timber.w("[%s] The characteristic wasn't attached to the descriptor! Returning error", device);
            returnErrorToRemoteClient(conn, device, requestId, offset);
            return true;
        }
        UUID charUuid = characteristic.getUuid();
        BluetoothGattService referencedService = characteristic.getService();
        if (referencedService == null) {
            Timber.w("[%s] The expected service wasn't attached to the characteristic! Returning error", device);
            returnErrorToRemoteClient(conn, device, requestId, offset);
            return true;
        }
        if (server.getService(referencedService.getUuid()) == null) {
            Timber.w("[%s] The expected service wasn't hosted! Returning error", device);
            returnErrorToRemoteClient(conn, device, requestId, offset);
            return true;
        }
        BluetoothGattService hostedService = server.getService(referencedService.getUuid());
        if (hostedService.getCharacteristic(charUuid) == null) {
            Timber.w("[%s] The expected characteristic wasn't hosted! Returning error", device);
            returnErrorToRemoteClient(conn, device, requestId, offset);
            return true;
        }
        return false;
    }

    /**
     * Will return an error to the remote client with the error code (24)GATT_ERROR with the provided offset
     * and a zero data array
     *
     * @param conn      The gatt server connection
     * @param device    The bluetooth device
     * @param requestId The request id
     * @param offset    The offset
     */

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void returnErrorToRemoteClient(GattServerConnection conn, BluetoothDevice device, int requestId, int offset) {
        handler.post(() -> {
            try {
                conn.getServer().sendResponse(device, requestId, GattStatus.GATT_ERROR.getCode(), offset, new byte[0]);
            } catch (NullPointerException e) {
                Timber.w(e, "[%s] Looks like BluetoothGattServer#sendResponse(...) can run into the unboxing bug also.  No response sent, peripheral may disconnect.", getDeviceMacFromDevice(device));
            }
        });
    }
}
