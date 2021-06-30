/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;
import static com.fitbit.bluetooth.fbgatt.FitbitGatt.atLeastSDK;

/**
 * The GattConnection object is the API interface for the FitbitGatt module, the code that would
 * like to interact with a remote device must first obtain an instance of gatt connection
 * from the FitbitGatt instance, after that they may begin to execute gatt transactions.
 * <p>
 * Created by iowens on 10/17/17.
 */

public class GattConnection implements Closeable {
    private AtomicLong disconnectedTTL;
    private FitbitBluetoothDevice device;
    private @Nullable volatile BluetoothGatt gatt;
    private GattState state;
    private GattStateTransitionValidator<GattClientTransaction> guard;
    private final ConcurrentHashMap<ConnectionEventListener, Boolean> asynchronousEventListeners = new ConcurrentHashMap<>();
    private boolean mockMode;
    private List<BluetoothGattService> mockServices;
    private TransactionQueueController clientQueue;
    private AtomicLong intraTransactionDelay = new AtomicLong(0);
    private @NonNull Handler mainHandler;

    public GattConnection(FitbitBluetoothDevice device, Looper mainLooper) {
        this.device = device;
        this.guard = new GattStateTransitionValidator<GattClientTransaction>();
        this.mockServices = new ArrayList<>(1);
        this.clientQueue = new TransactionQueueController(this);
        this.state = GattState.DISCONNECTED;
        this.disconnectedTTL = new AtomicLong(FitbitGatt.MAX_TTL);
        this.mainHandler = new Handler(mainLooper);
    }

    long getDisconnectedTTL() {
        return this.disconnectedTTL.get();
    }

    /**
     * Will reset the TTL for a device
     */
    @SuppressWarnings("WeakerAccess")
    // API Method
    void resetDisconnectedTTL() {
        this.disconnectedTTL.set(FitbitGatt.MAX_TTL);
    }

    void setDisconnectedTTL(long value) {
        this.disconnectedTTL.set(value);
    }

    @VisibleForTesting
    int numberOfEventListeners() {
        return this.asynchronousEventListeners.size();
    }

    /**
     * To set or change an intra-transaction delay... this value is initialized to zero, setting
     * it to any non-zero value will cause it to be posted to the main handler
     *
     * @param txDelay The delay in milliseconds to wait before queueing the next transaction
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public void setIntraTransactionDelay(long txDelay) {
        long oldValue = intraTransactionDelay.getAndSet(txDelay);
        Timber.v("[%s] Changing intra-transaction delay from %dms, to %dms", getDevice(), oldValue, intraTransactionDelay.get());
    }

    /**
     * Will return the intratransaction delay
     *
     * @return The delay in milliseconds to wait before queueing the next transaction
     */
    @SuppressWarnings("unused") // API Method
    public long getIntraTransactionDelay() {
        return intraTransactionDelay.get();
    }

    /**
     * To register for connection related events only, this is primarily for the main
     * {@link FitbitGatt} singleton, but if something wants to listen to the global connection
     * events also, they should be able to register here as well
     *
     * @param eventListener The {@link ConnectionEventListener} instance for connection events
     */

    public void registerConnectionEventListener(@NonNull ConnectionEventListener eventListener) {
        if (this.asynchronousEventListeners.putIfAbsent(eventListener, true) != null) {
            Timber.v("[%s] This listener is already registered", getDevice());
        }
    }

    /**
     * To un-register for connection related events only, this is primarily for the main
     * {@link FitbitGatt} singleton, but if something wants to listen to the global connection
     * events also, they should be able to un-register here as well
     *
     * @param eventListener The {@link ConnectionEventListener} instance for connection events
     */

    public void unregisterConnectionEventListener(@NonNull ConnectionEventListener eventListener) {
        Boolean previousValue = asynchronousEventListeners.remove(eventListener);
        if (previousValue == null) { // null when returned from ConcurrentHashMap.remove() means the key was not present.
            Timber.v("[%s] There are no event listeners to remove", Build.MODEL);
        }
    }

    @NonNull
    ArrayList<ConnectionEventListener> getConnectionEventListeners() {
        //We want a copy of the listeners set, so that clients can't modify it.
        return new ArrayList<>(asynchronousEventListeners.keySet());
    }

    /**
     * Will register a listener for {@link GattClientCallback} events
     *
     * @param clientListener The {@link GattClientListener} instance
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public void registerGattClientListener(GattClientListener clientListener) {
        if (FitbitGatt.getInstance().getClientCallback() != null) {
            FitbitGatt.getInstance().getClientCallback().addListener(clientListener);
        } else {
            Timber.w("[%s] The client callback was null, something is quite wrong", getDevice());
        }
    }

    /**
     * Will unregister a listener for {@link GattClientListener} events
     *
     * @param clientListener The {@link GattClientListener} instance
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public void unregisterGattClientListener(GattClientListener clientListener) {
        if (FitbitGatt.getInstance().getClientCallback() != null) {
            FitbitGatt.getInstance().getClientCallback().removeListener(clientListener);
        } else {
            Timber.w("[%s] The client callback was null, something is quite wrong", getDevice());
        }
    }

    /**
     * Will return the raw gatt instance
     *
     * @return The raw gatt instance
     */
    public @Nullable BluetoothGatt getGatt() {
        return gatt;
    }

    /**
     * Will manually update the state of this connection, this should only be used with care
     *
     * @param state The state to set the connection to
     */

    public synchronized void setState(GattState state) {
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.v("[%s] Transitioning from state %s to state %s", getDevice(), this.state.name(), state.name());
        }
        this.state = state;
    }

    /**
     * Will get the current gatt state
     *
     * @return The current gatt state
     */

    public synchronized GattState getGattState() {
        return state;
    }

    /**
     * Will get the {@link FitbitBluetoothDevice} that this connection is for
     *
     * @return The {@link FitbitBluetoothDevice} for this connection
     */

    public FitbitBluetoothDevice getDevice() {
        return device;
    }

    synchronized GattStateTransitionValidator.GuardState checkTransaction(GattClientTransaction tx) {
        return guard.checkTransaction(getGattState(), tx);
    }

    void resetStates() {
        this.setState(GattState.DISCONNECTED);
    }

    @VisibleForTesting
    void addService(BluetoothGattService service) {
        this.mockServices.add(service);
    }

    boolean connectedDeviceHostsService(UUID serviceUuid) {
        if (mockMode) {
            for (BluetoothGattService service : mockServices) {
                if (service.getUuid() != null && service.getUuid().equals(serviceUuid)) {
                    return true;
                }
            }
            return false;
        } else {
            // if the device has not had discovery performed, we will not know that the connection
            // is hosting the service
            if (isConnected()) {
                return null != getGatt() && null != getGatt().getService(serviceUuid);
            } else {
                return false;
            }
        }
    }

    /**
     * Convenience method for fetching a remote gatt service characteristic from a connected peripheral
     *
     * @param serviceUuid        The service UUID hosting the remote service
     * @param characteristicUuid The characteristic UUID hosted by the remote service
     * @return The {@link BluetoothGattCharacteristic} if available or null if the service or characteristic is not present.
     */

    public @Nullable
    BluetoothGattCharacteristic getRemoteGattServiceCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        BluetoothGattService service = getRemoteGattService(serviceUuid);
        if (service == null) {
            return null;
        } else {
            return service.getCharacteristic(characteristicUuid);
        }
    }

    /**
     * Convenience method for fetching a remote gatt service from a connected peripheral
     *
     * @param uuid The UUID of the service for which we are searching
     * @return The {@link BluetoothGattService} or null if we do not find it or are not connected
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public @Nullable
    BluetoothGattService getRemoteGattService(UUID uuid) {
        if (mockMode) {
            for (BluetoothGattService service : mockServices) {
                if (uuid.equals(service.getUuid())) {
                    return service;
                }
            }
            return null;
        } else {
            BluetoothGatt localGatt = gatt;
            if (isConnected() && localGatt != null) {
                return localGatt.getService(uuid);
            } else {
                return null;
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void simulateDisconnect() {
        if (this.mockMode) {
            setState(GattState.DISCONNECTED);
            for (ConnectionEventListener asyncConnListener : getConnectionEventListeners()) {
                asyncConnListener.onClientConnectionStateChanged(new TransactionResult.Builder()
                        .resultStatus(TransactionResult.TransactionResultStatus.FAILURE)
                        .gattState(getGattState())
                        .responseStatus(GattStatus.GATT_UNKNOWN.ordinal()).build(), this);
            }
        } else {
            throw new IllegalStateException(String.format(Locale.ENGLISH, "[%s] You can't simulate a disconnection if you are not in mock mode", getDevice()));
        }
    }


    @VisibleForTesting
    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    /* ------------------------------- dispatch ----------------------------------------------- */

    /**
     * Will perform a gatt connect, on the device, if the device isn't in the connection pool, we
     * will only continue to connect to the device if there is not
     * already a connection for this device, in this way we will only ever have one device connection
     * at a time, please use {@link FitbitGatt#getConnection(FitbitBluetoothDevice)} or
     * {@link FitbitGatt#getMatchingConnectionsForDeviceNames(List)}, or
     * {@link FitbitGatt#getMatchingConnectionsForServices(List)} to find your device
     *
     * @return True if the connection attempt was made, false if it was not
     */
    public synchronized boolean connect() {
        if (connectionInstanceAlreadyExists(getDevice())) {
            Timber.d("[%s] GattConnection already exists for the device", getDevice());
            return false;
        }
        if (gatt == null) {
            Timber.v("[%s] Android BluetoothGatt was null, start a new Android BluetoothGatt instance connect to device", device);
            return ifGattHasNeverBeenInstantiatedConnect(device);
        } else {
            Timber.v("[%s] Android BluetoothGatt has been used before, using an existing Android BluetoothGatt instance to connect to device", device);
            return ifGattHasBeenInstantiatedConnect(device);
        }
    }

    /**
     * This method is called only for devices that are already connected on Gatt Profile
     * {@link android.bluetooth.BluetoothManager#getConnectedDevices(int)}
     * <p>
     * Will perform a gatt connect on the device, to initialise [BluetoothGatt]
     * for this connection and set the state to [GattState.CONNECTED]
     * because no connection change callback will happen
     */
    void initGattForConnectedDevice() {
        Timber.v("[%s] already connected, init BluetoothGatt ", device);
        if (isConnected()) {
            return;
        }
        if (ifGattHasNeverBeenInstantiatedConnect(device)) {
            setState(GattState.CONNECTED);
        }
    }

    private boolean connectionInstanceAlreadyExists(FitbitBluetoothDevice device) {
        GattConnection cachedConnection = FitbitGatt.getInstance().getConnectionMap().get(device);
        if (cachedConnection != null && !cachedConnection.equals(this)) {
            Timber.w("[%s] You are trying to connect but there is already a connection available for it.  We are saving you from creating too many client_ifs #developerlove.", device);
            return true;
        } else if (cachedConnection != null && cachedConnection.getDevice().equals(device)) {
            Timber.w("[%s] While this instance isn't in the cache, there is already a connection in the queue, please be careful not to create too many client_ifs #developerlove.", device);
            return false;
        } else {
            return false;
        }
    }

    private boolean ifGattHasBeenInstantiatedConnect(FitbitBluetoothDevice device) {
        if (isConnected()) {
            Timber.w("[%s] You can't connect while connected, or connecting, please know what you are doing here.", device);
            return true;
        }
        setState(GattState.CONNECTING);
        // keep in mind that this could go on for 90 seconds on some devices
        if (mockMode) {
            mockConnect();
            return true;
        }
        /* we can re-use the listener here because all of our gatt instances are using the
         * same listener which calls back everyone, but will only notify the transaction instances
         * on the connection that are relevant using the device as a discriminator.
         */
        BluetoothGatt localGatt = gatt;
        boolean success = false;
        if (localGatt != null) {
            try {
                success = localGatt.connect();
            } catch (NullPointerException e) {
                Timber.e(e);
                setState(GattState.FAILURE_CONNECTING_WITH_SYSTEM_CRASH);
                return false;
            }
        }
        if (!success) {
            setState(GattState.FAILURE_CONNECTING);
        }
        return success;
    }

    private boolean ifGattHasNeverBeenInstantiatedConnect(FitbitBluetoothDevice device) {
        if (mockMode) {
            // mock connect
            mockConnect();
            return true;
        }
        if (atLeastSDK(Build.VERSION_CODES.M)) {
            try {
                gatt = device.getBtDevice().connectGatt(FitbitGatt.getInstance().getAppContext(), false, FitbitGatt.getInstance().getClientCallback(), BluetoothDevice.TRANSPORT_LE);
            } catch (NullPointerException e) {
                Timber.e(e);
                //This crash can be seen mainly on some low end devices such as P20 Lite or A5
                setState(GattState.FAILURE_CONNECTING_WITH_SYSTEM_CRASH);
                return false;
            }
        } else {
            gatt = device.getBtDevice().connectGatt(FitbitGatt.getInstance().getAppContext(), false, FitbitGatt.getInstance().getClientCallback());
        }
        if (gatt == null) {
            setState(GattState.FAILURE_CONNECTING);
            return false;
        } else {
            setState(GattState.CONNECTING);
            return true;
        }
    }

    private void mockConnect() {
        Timber.i("[%s] Mock connecting!!!!", getDevice());
        setState(GattState.CONNECTING);
        mainHandler.postDelayed(() -> FitbitGatt.getInstance().getClientCallback().
                onConnectionStateChange(null, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED), 1499);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, this);
    }

    private void mockDisconnect() {
        Timber.i("[%s] Mock disconnecting!!!", getDevice());
        setState(GattState.DISCONNECTING);
        mainHandler.postDelayed(() -> FitbitGatt.getInstance().getClientCallback().onConnectionStateChange(null, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED), 150);
    }

    /**
     * Will unregister this process from the system so no further callback will be delivered, note
     * this does not actually disconnect the remote peripheral
     */

    public void disconnect() {
        if (mockMode) {
            mockDisconnect();
            return;
        }
        BluetoothGatt localGatt = gatt;
        if (localGatt == null) {
            setState(GattState.DISCONNECTED);
        } else {
            /*
             * It is important to note that after disconnect is processed, there can be a long
             * supervision timeout if the device disconnects itself, the state will remain
             * {@link GattState.DISCONNECTING} until that is complete ...
             */
            try {
                localGatt.disconnect();
            } catch (NullPointerException e) {
                // this means that the hardware's underlying connection went away while
                // we were trying to cancel a connection attempt
                // there are a number of phones that have this flaw, we don't need to do
                // anything though, if this NPEs it means that the connection is already
                // cancelled ... there is nothing there to cancel
                Timber.e(e, "[%s] OS Stack Failure while disconnecting", getDevice());
            }
            setState(GattState.DISCONNECTING);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    synchronized void finish() {
        BluetoothGatt localGatt = this.gatt;
        if (localGatt != null) {
            closeClientIf(localGatt);
            gatt = null;
            setState(GattState.DISCONNECTED);
        }
        clientQueue.stop();
        asynchronousEventListeners.clear();
    }

    /**
     * This is only for use with the refresh transaction
     */
    public void justClearGatt() {
        BluetoothGatt localGatt = this.gatt;
        if (localGatt != null) {
            closeClientIf(localGatt);
            gatt = null;
            setState(GattState.DISCONNECTED);
        }
    }

    /**
     * This is different from finish in that we might use this connection instance again
     */

    void cleanUpConnection() {
        Timber.v("[%s] Cleaning up connection, flushing pending gatt operations", getDevice());
        clientQueue.clearQueue();
    }

    /**
     * Will return the present state of this connection, it will return false if bluetooth is turned off
     * or if this connection is in the process of disconnecting or is disconnected.  Note, disconnected
     * DOES NOT mean that the peripheral is actually disconnected from the phone, it just means that we have
     * deregistered the client_if.  If you don't know what a client_if is, have a read
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/BluetoothGatt.java
     *
     * @return true if the client if for the peripheral is registered and the peripheral is connected, false if the peripheral is disconnecting, connecting, disconnected, or bt is off.
     */

    public boolean isConnected() {
        return !getGattState().equals(GattState.DISCONNECTED)
                && !getGattState().equals(GattState.DISCONNECTING)
                && !getGattState().equals(GattState.BT_OFF)
                && !getGattState().equals(GattState.CONNECTING)
                && !getGattState().equals(GattState.FAILURE_CONNECTING)
                && !getGattState().equals(GattState.FAILURE_CONNECTING_WITH_SYSTEM_CRASH);
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
     * <p>
     * Every time a transaction is attempted, we will reset the disconnected TTL to prevent cleanup
     *
     * @param transaction The transaction to run
     * @param callback    The gatt transaction callback
     */

    public void runTx(GattClientTransaction transaction, GattTransactionCallback callback) {
        Timber.v("[%s] Received transaction: %s", getDevice(), transaction.getName());
        resetDisconnectedTTL();
        if (intraTransactionDelay.get() == 0) {
            queueTransaction(transaction, callback);
        } else {
            // uses the main handler
            final long currentTimeMillis = System.currentTimeMillis();
            Timber.v("[%s] Posting tx to queue in %dms", getDevice(), intraTransactionDelay.get());
            getMainHandler().postDelayed(() -> {
                final long queueTimeMillis = System.currentTimeMillis();
                long timeToQueue = queueTimeMillis - currentTimeMillis;
                Timber.v("[%s] Queueing tx %dms after posting", getDevice(), timeToQueue);
                queueTransaction(transaction, callback);
            }, intraTransactionDelay.get());
        }
    }

    private void queueTransaction(GattClientTransaction transaction, GattTransactionCallback callback) {
        clientQueue.queueTransaction(() -> transaction.commit(callback));
    }

    /**
     * Will retrieve the connection handler for this remote device connection, blocking this will
     * slow down operations
     *
     * @return The current connection handler
     */
    @SuppressWarnings("WeakerAccess") // API Method
    public TransactionQueueController getClientTransactionQueueController() {
        return clientQueue;
    }

    boolean getMockMode() {
        return mockMode;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Object.toString(): ");
        builder.append(super.toString());
        builder.append(" device: ");
        builder.append(getDevice());
        builder.append(" isConnected: ");
        builder.append(isConnected());
        builder.append(" state: ");
        GattState gattState = getGattState();
        builder.append(gattState);
        if (gattState != null) {
            builder.append(" state type: ");
            builder.append(getGattState().stateType);
        }
        builder.append(" numConnEvtListeners ");
        builder.append(getConnectionEventListeners().size());
        return builder.toString();
    }

    /**
     * Used to release the held gatt instance
     */
    synchronized void gattRelease() {
        BluetoothGatt localGatt = gatt;
        if (localGatt != null) {
            closeClientIf(localGatt);
            gatt = null;
            setState(GattState.DISCONNECTED);
        } else {
            Timber.w("[%s] The gatt was null when trying to release, the logic is busted or you are suffering from an Android bug, look into a strategy.", getDevice());
        }
    }

    public @NonNull Handler getMainHandler() {
        return mainHandler;
    }

    /**
     * Will stop the connection handler, effectively rendering this connection useless.  Only should
     * be called by the cleanup process, this is not intended to be the same as disconnect, which
     * indicates that you might want to use this connection again.
     */

    @Override
    public void close() {
        finish();
    }

    private void closeClientIf(BluetoothGatt localGatt) {
        try {
            localGatt.close();
        } catch (NullPointerException e) {
            // if we end up with an NPE here, it means that the underlying driver
            // didn't find the connection, this can happen with an unexpected
            // disconnection from the peripheral side which hasn't yet been realized
            // by the Android OS
            Timber.e(e, "[%s] Ran into OS Stack NPE", getDevice());
        }
    }
}
