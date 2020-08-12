/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import timber.log.Timber;

/**
 * Gatt server connection wrapper, there will be only one and it will be maintained on the
 * {@link FitbitGatt}
 *
 * Created by iowens on 11/17/17.
 */

public class GattServerConnection implements Closeable {
    private BluetoothGattServer server;
    private TransactionQueueController serverQueue;
    private GattState state;
    private AtomicLong intraTransactionDelay = new AtomicLong(0);
    private GattStateTransitionValidator<GattServerTransaction> guard;
    private final ConcurrentHashMap<ServerConnectionEventListener, Boolean> asynchronousEventListeners = new ConcurrentHashMap<>();
    private HashSet<FitbitBluetoothDevice> connectedDevices = new HashSet<>();
    private Handler mainHandler;
    private boolean mockMode;

    protected GattServerConnection(@Nullable BluetoothGattServer server, Looper looper) {
        this.server = server;
        this.serverQueue = new TransactionQueueController();
        this.guard = new GattStateTransitionValidator<>();
        this.state = GattState.IDLE;
        this.mainHandler = new Handler(looper);
    }

    public synchronized GattState getGattState(){
        return state;
    }

    public BluetoothGattServer getServer(){
        return server;
    }

    synchronized GattStateTransitionValidator.GuardState checkTransaction(GattServerTransaction tx) {
        return guard.checkTransaction(getGattState(), tx);
    }

    public void registerConnectionEventListener(@NonNull ServerConnectionEventListener eventListener) {
        if(this.asynchronousEventListeners.putIfAbsent(eventListener, true) != null) {
            Timber.v("[%s] This listener is already registered", Build.MODEL);
        }
    }

    @SuppressWarnings("WeakerAccess") // API Method
    public void unregisterConnectionEventListener(@NonNull ServerConnectionEventListener eventListener) {
        Boolean previousValue = asynchronousEventListeners.remove(eventListener);
        if(previousValue == null) { // null when returned from ConcurrentHashMap.remove() means the key was not present.
            Timber.v("[%s] There are no event listeners to remove", Build.MODEL);
        }
    }

    @NonNull
    ArrayList<ServerConnectionEventListener> getConnectionEventListeners(){
        //We want a copy of the listeners set, so that clients can't modify it.
        return new ArrayList<>(asynchronousEventListeners.keySet());
    }

    public synchronized void setState(GattState state) {
        Timber.v("[%s] Transitioning from state %s to state %s", Build.MODEL, this.state.name(), state.name());
        this.state = state;
    }

    @SuppressWarnings("unused") // API Method
    void resetStates(){
        this.setState(GattState.DISCONNECTED);
    }

    @VisibleForTesting
    void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }
    /**
     * To set or change an intra-transaction delay... this value is initialized to zero, setting
     * it to any non-zero value will cause it to be posted to the connection handler
     * @param txDelay The delay in milliseconds to wait before queueing the next transaction
     */
    void setIntraTransactionDelay(long txDelay) {
        long oldValue = intraTransactionDelay.getAndSet(txDelay);
        Timber.v("[%s] Changing intra-transaction delay from %dms, to %dms", Build.MODEL, oldValue, intraTransactionDelay.get());
    }

    /**
     * Will return the intra-transaction delay
     * @return The delay in milliseconds to wait before queueing the next transaction
     */
    @SuppressWarnings("unused") // API Method
    public long getIntraTransactionDelay(){
        return intraTransactionDelay.get();
    }
    /**
     * Will run the provided transaction once the execution thread is ready, internally will queue the
     * transaction on the calling thread.  If these come in too quickly from arbitrary threads
     * the second thread, who should wait, will get invalid state.  We should post the commits
     * to the connection thread.  If a transaction is running on the thread, this runnable will
     * be queued, and executed later, once the other tx has completed.  The tx can be delayed by
     * setting {@link GattConnection#setIntraTransactionDelay(long)}.  The recommended delay is 3ms
     * this seems to prevent gatt_if queue wedging for most phones, although more or less delay
     * maybe usable for the library user depending on the performance of the phone, it's BT stack,
     * and the peripheral
     * @param transaction The transaction to run
     * @param callback The gatt transaction callback
     */

    public void runTx(GattServerTransaction transaction, GattTransactionCallback callback) {
        if(intraTransactionDelay.get() == 0) {
            queueTransaction(transaction, callback);
        } else {
            // uses the main handler
            final long currentTimeMillis = System.currentTimeMillis();
            Timber.v("[%s] Posting tx to queue in %dms", Build.MODEL, intraTransactionDelay.get());
            getMainHandler().postDelayed(() -> {
                final long queueTimeMillis = System.currentTimeMillis();
                long timeToQueue = queueTimeMillis - currentTimeMillis;
                Timber.v("[%s] Queueing tx %dms after posting", Build.MODEL, timeToQueue);
                queueTransaction(transaction, callback);
            }, intraTransactionDelay.get());
        }
    }

    private void queueTransaction(GattServerTransaction transaction, GattTransactionCallback callback) {
        serverQueue.queueTransaction(() -> transaction.commit(callback));
    }

    /**
     * Will return the present state of this connection, it will return false if bluetooth is turned off
     * or if this connection is in the process of disconnecting or is disconnected.  Note, disconnected
     * DOES NOT mean that the peripheral is actually disconnected from the phone, it just means that we have
     * deregistered the client_if.  If you don't know what a client_if is, have a read
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/BluetoothGatt.java
     * @return true if the client if for the peripheral is registered and the peripheral is connected, false if the peripheral is disconnecting, or disconnected, or bt is off.
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public boolean isConnected(){
        return !getGattState().equals(GattState.DISCONNECTED) && !getGattState().equals(GattState.DISCONNECTING) && !getGattState().equals(GattState.BT_OFF);
    }

    TransactionQueueController getServerTransactionQueueController() {
        return serverQueue;
    }

    public void connect(FitbitBluetoothDevice device) {
        if(mockMode) {
            connectedDevices.add(device);
            mockConnect();
            return;
        }
        if(connectedDevices.contains(device)) {
            return;
        }
        boolean success = server.connect(device.getBtDevice(), true);
        if(success) {
            connectedDevices.add(device);
            setState(GattState.CONNECTING);
        } else {
            setState(GattState.FAILURE_CONNECTING);
        }
    }

    private void mockConnect() {
        Timber.i("[%s] Mock connecting!!!!", Build.MODEL);
        setState(GattState.CONNECTING);
        mainHandler.postDelayed(() -> FitbitGatt.getInstance().getServerCallback().
                onConnectionStateChange(null, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED), 1499);
    }

    private void mockDisconnect() {
        Timber.i("[%s] Mock disconnecting!!!", Build.MODEL);
        setState(GattState.DISCONNECTING);
        mainHandler.postDelayed(() -> FitbitGatt.getInstance().getServerCallback().onConnectionStateChange(null, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED), 150);
    }

    @SuppressWarnings("unused") // API Method
    public boolean isDeviceConnected(FitbitBluetoothDevice device) {
        return connectedDevices.contains(device);
    }

    public void disconnect(FitbitBluetoothDevice device) {
        if(mockMode) {
            connectedDevices.remove(device);
            mockDisconnect();
            return;
        }
        if(!connectedDevices.contains(device)) {
            return;
        }
        /*
         * It is important to note that after disconnect is processed, there can be a long
         * supervision timeout if the device disconnects itself, the state will remain
         * {@link GattState.DISCONNECTING} until that is complete ...
         */
        server.cancelConnection(device.getBtDevice());
        connectedDevices.remove(device);
        setState(GattState.DISCONNECTING);
    }

    @SuppressWarnings("unused") // API method and warning
    protected void closeGattServer(){
        Timber.v("Unregistering gatt server listeners");
        asynchronousEventListeners.clear();
        BluetoothGattServer server = getServer();
        if(server != null) {
            setState(GattState.CLOSING_GATT_SERVER);
            Timber.v("Clearing gatt server services");
            server.clearServices();
            Timber.v("Closing gatt server");
            // it seems to me that close should not be used unless the process is dying
            // it always prevents adding services on the GS9+ ( Exynos ) and on the Pixel 2 ( Q )
            server.close();
            setState(GattState.CLOSE_GATT_SERVER_SUCCESS);
            if(serverQueue != null) {
                serverQueue.stop();
            }
        }
    }

    /**
     * Will log off the state of this connection
     * @return The state of this connection as a string
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Object.toString(): ");
        builder.append(super.toString());
        builder.append(" isConnected: ");
        builder.append(isConnected());
        builder.append(" state: ");
        GattState gattState = getGattState();
        builder.append(gattState);
        if (gattState != null) {
            builder.append(" state type: ");
            builder.append(getGattState().stateType);
        }
        builder.append(" numConnEvtListeners: ");
        builder.append(getConnectionEventListeners().size());
        return builder.toString();
    }

    /**
     * Will return a handler on the main thread for use in a transaction
     * @return The main looper based handler
     */

    public Handler getMainHandler() {
        return mainHandler;
    }

    @Override
    public void close() {
        finish();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    synchronized void finish() {
        if(serverQueue != null) {
            serverQueue.stop();
        }
    }
}
