/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.util.Bytes;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A class to encapsulate the range of possible data to be returned from the transactions and the
 * {@link ServerConnectionEventListener}.  Items in this class are final except where they can
 * change after initialization.
 *
 * Created by iowens on 10/19/17.
 */

public class TransactionResult {
    /**
     * The transaction result status
     */
    public enum TransactionResultStatus {
        /**
         * The transaction finished successfully
         */
        SUCCESS,
        /**
         * The transaction failed to complete successfully
         */
        FAILURE,
        /**
         * The entry state was invalid when this transaction was attempted
         */
        INVALID_STATE,
        /**
         * The transaction timed out
         */
        TIMEOUT
    }

    /**
     * The gatt operation response status
     */
    private final int responseStatus;
    /**
     * If handling a gatt server read or write request, the requestId for ensuring proper response
     */
    private final int requestId;
    /**
     * The offset for the data being passed back as the result of a server read or write operation
     */
    private final int offset;
    /**
     * The present tx physical layer, can be {@link BluetoothDevice#PHY_LE_2M}, {@link BluetoothDevice#PHY_LE_1M}
     * or {@link BluetoothDevice#PHY_LE_CODED}
     */
    private final int txPhy;
    /**
     * The present rx physical layer, can be {@link BluetoothDevice#PHY_LE_2M}, {@link BluetoothDevice#PHY_LE_1M}
     * or {@link BluetoothDevice#PHY_LE_CODED}
     */
    private final int rxPhy;
    /**
     * The characteristic UUID involving the transaction that generated this result
     */
    private final UUID characteristicUuid;
    /**
     * The service UUID involving the transaction that generated this result
     */
    private final UUID serviceUuid;
    /**
     * The descriptor UUID involving the transaction that generated this result
     */
    private final UUID descriptorUuid;
    /**
     * The gatt server services found as the result of a discover transaction
     */
    private final List<BluetoothGattService> gattServerServices;
    /**
     * The resulting {@link GattState} for a transaction
     */
    final GattState resultState;
    /**
     * The {@link TransactionResultStatus} for the transaction that generated this result
     */
    final TransactionResultStatus resultStatus;
    /**
     * The data payload copied out of the {@link android.bluetooth.BluetoothGattDescriptor} or the {@link android.bluetooth.BluetoothGattCharacteristic}
     * this needs to be copied because the value attribute of these objects is a dereferenced pointer
     * to the underlying data structure which can change as something else writes to that gatt item
     * this is not final because we don't ever want for this to be null, but we don't know what the
     * size of the value attribute for the result of this transaction will be when the builder is
     * constructing the result.  In theory we could leave it to the builder, but then we would need
     * to fight lint, so it's easier to ensure that it could never be null.
     */
    // it can be an empty array, but should never be null
    private byte[] data = new byte[1];
    /**
     * The RSSI value of the {@link com.fitbit.bluetooth.fbgatt.tx.ReadRssiTransaction} transaction
     *
     * Since this is a primitive it can not be null, and it needs to be set by the builder so this
     * needs to be non-final.
     */
    private int rssi;
    /**
     * The MTU value of the {@link com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction} transaction
     *
     * Since this is a primitive it can not be null, and it needs to be set by the builder so this
     * needs to be non-final.
     */
    private int mtu;
    /**
     * Whether this was a preparedWrite for a received server write request
     */
    private boolean preparedWrite;
    /**
     * Whether a response is required for a given write request
     */
    private boolean responseRequired;
    /**
     * The transaction name to get around obfuscation in logging
     */
    private String transactionName;

    private ArrayList<TransactionResult> transactionResults;

    /**
     * Will construct a transaction result from an existing transaction result for a different
     * transaction, this is a convenience constructor
     * @param result The provided transaction result to copy
     * @param newTransactionName The new transaction name
     */
    TransactionResult(TransactionResult result, String newTransactionName) {
        this(result.resultState, result.resultStatus, result.responseStatus, result.rssi,
                result.mtu, result.requestId, result.characteristicUuid, result.serviceUuid,
                result.descriptorUuid, result.data, result.offset, result.gattServerServices,
                result.preparedWrite, result.responseRequired, result.transactionName, result.txPhy,
                result.rxPhy, result.transactionResults);
        this.transactionName = newTransactionName;
        this.transactionResults = new ArrayList<>();
    }

    /**
     * Transaction result constructor
     * @param state The resulting {@link GattState} for a transaction
     * @param status The transaction result status
     * @param responseStatus The gatt operation response status
     * @param rssi The RSSI value of the {@link com.fitbit.bluetooth.fbgatt.tx.ReadRssiTransaction} transaction
     * @param mtu The MTU value of the {@link com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction} transaction
     * @param requestId If handling a gatt server read or write request, the requestId for ensuring proper response
     * @param characteristicUuid The characteristic UUID involving the transaction that generated this result
     * @param serviceUuid The service UUID involving the transaction that generated this result
     * @param descriptorUuid The descriptor UUID involving the transaction that generated this result
     * @param data The data payload copied out of the {@link android.bluetooth.BluetoothGattDescriptor} or the {@link android.bluetooth.BluetoothGattCharacteristic}
     *       this needs to be copied because the value attribute of these objects is a dereferenced pointer
     *       to the underlying data structure which can change as something else writes to that gatt item
     *       this is not final because we don't ever want for this to be null, but we don't know what the
     *       size of the value attribute for the result of this transaction will be when the builder is
     *       constructing the result.
     * @param offset The offset for the data being passed back as the result of a server read or write operation
     * @param services The gatt server services found as the result of a discover transaction
     * @param preparedWrite Whether this was a preparedWrite for a received server write request
     * @param responseRequired Whether a response is required for a given write request
     * @param transactionName The transaction name to get around obfuscation in logging
     */
    TransactionResult(GattState state, TransactionResultStatus status, int responseStatus, int rssi,
                      int mtu, int requestId, UUID characteristicUuid, UUID serviceUuid,
                      UUID descriptorUuid, byte[] data, int offset, List<BluetoothGattService> services, boolean preparedWrite, boolean responseRequired, String transactionName, int txPhy, int rxPhy, List<TransactionResult> transactionResults) {
        this.resultState = state;
        this.resultStatus = status;
        this.responseStatus = responseStatus;
        this.requestId = requestId;
        this.characteristicUuid = characteristicUuid;
        this.serviceUuid = serviceUuid;
        this.descriptorUuid = descriptorUuid;
        this.data = data;
        this.rssi = rssi;
        this.mtu = mtu;
        this.offset = offset;
        this.gattServerServices = (services == null) ? new ArrayList<>() : services;
        this.preparedWrite = preparedWrite;
        this.responseRequired = responseRequired;
        this.transactionName = transactionName;
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
        this.transactionResults = new ArrayList<>(transactionResults);
    }

    /**
     * Set the data property
     * @param data The data that is copied from the value attribute of the descriptor or characteristic
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * The data that is copied from the value attribute of the descriptor or characteristic, can be
     * null if this {@link TransactionResult} is the result of a transaction that produces no data
     * such as {@link com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction}, or if the
     * connection vanishes before the data can be populated, in that scenario it is possible that
     * this could be set to null.
     * @return The data or null
     */
    public @Nullable byte[] getData() {
        return this.data;
    }

    /**
     * The RSSI value provided by the {@link com.fitbit.bluetooth.fbgatt.tx.ReadRssiTransaction}
     * can be undefined if read from a transaction that is not involved with reading the RSSI
     * @return The RSSI value or undefined
     */
    public int getRssi(){
        return this.rssi;
    }

    /**
     * The MTU value resulting from the {@link com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction}
     * can be undefined if read from a transaction that is not involved with an MTU operation
     * @return The mtu value or undefined
     */
    public int getMtu(){
        return this.mtu;
    }

    /**
     * The request ID associated with a received server read or write request
     * @return The request ID
     */
    public int getRequestId() { return this.requestId; }
    /**
     * The present tx physical layer, can be {@link BluetoothDevice#PHY_LE_2M}, {@link BluetoothDevice#PHY_LE_1M}
     * or {@link BluetoothDevice#PHY_LE_CODED}
     * @return The current tx phy, or -1 if uninitialized
     */
    public int getTxPhy() { return this.txPhy; }
    /**
     * The present tx physical layer, can be {@link BluetoothDevice#PHY_LE_2M}, {@link BluetoothDevice#PHY_LE_1M}
     * or {@link BluetoothDevice#PHY_LE_CODED}
     * @return The current rx phy, or -1 if uninitialized
     */
    public int getRxPhy() { return this.rxPhy; }

    /**
     * The transaction result status, this can not be null or the entire transaction system will
     * break, every transaction must provide a defined result.
     * @return The transaction result status
     */
    public @NonNull TransactionResultStatus getResultStatus(){
        return this.resultStatus;
    }

    /**
     * This will provide a list of services delivered in {@link com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction}
     * this will never be null because the builder will default to an empty list, even if someone creates this class without
     * using the builder the constructor will force this to be populated with an empty list
     * @return Either the list of services or an empty list
     */
    public @NonNull List<BluetoothGattService> getServices() {
        return this.gattServerServices;
    }

    /**
     * Will provide the descriptor UUID for transactions that deal with descriptors such as
     * {@link com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction}
     * This can be null for transactions that do not populate a descriptor UUID, or in the case
     *      * of an underlying gatt error where something else clears the GATT db
     * @return The descriptor UUID or null if unknown
     */
    public @Nullable UUID getDescriptorUuid() { return this.descriptorUuid; }
    /**
     * Will provide the characteristic UUID for transactions that deal with descriptors such as
     * {@link com.fitbit.bluetooth.fbgatt.tx.WriteGattCharacteristicTransaction}
     * This can be null for transactions that do not populate a characteristic UUID, or in the case
     *      * of an underlying gatt error where something else clears the GATT db
     * @return The characteristic UUID or null if unknown
     */
    public @Nullable UUID getCharacteristicUuid(){
        return this.characteristicUuid;
    }

    /**
     * Will provide the service UUID for transactions that deal with services such as
     * {@link com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction}
     * This can be null for transactions that do not populate a service UUID, or in the case
     * of an underlying gatt error where something else clears the GATT db
     * @return
     */
    public @Nullable UUID getServiceUuid() { return this.serviceUuid; }

    /**
     * Will return the offset for asynchronous server read or server write requests, can be
     * undefined for transactions that are not triggered by server read or write requests
     * {@link android.bluetooth.BluetoothGattServerCallback#onCharacteristicWriteRequest(BluetoothDevice, int, BluetoothGattCharacteristic, boolean, boolean, int, byte[])}
     * @return The offset value for the data
     */

    public int getOffset() { return this.offset; }

    /**
     * Whether this is a prepared write or not in the write request received against the local gatt
     * server {@link android.bluetooth.BluetoothGattServerCallback#onCharacteristicWriteRequest(BluetoothDevice, int, BluetoothGattCharacteristic, boolean, boolean, int, byte[])}
     * @return True if this is a prepared write, false if not
     */
    public boolean isPreparedWrite() {
        return preparedWrite;
    }
    /**
     * Whether this is a response is required or not in the write request received against the local gatt
     * server {@link android.bluetooth.BluetoothGattServerCallback#onCharacteristicWriteRequest(BluetoothDevice, int, BluetoothGattCharacteristic, boolean, boolean, int, byte[])}
     * @return True if a response is required, false if not
     */
    public boolean isResponseRequired() {
        return responseRequired;
    }

    /**
     * Will return the response code string from a {@link GattStatus} if contained in this
     * transaction response
     * @return The gatt status enum for the ordinal
     */
    public String getResponseCodeString(){
        return GattStatus.values()[this.responseStatus].name();
    }

    /**
     * The gatt transaction name
     * @return The string representing the gatt transaction
     */
    public String getTransactionName(){
        return this.transactionName;
    }

    /**
     * The resulting state at the end of the transaction.  This is to provide assurance in the case
     * that you are validating the system state at the end of a given transaction, but the system
     * has marched on, I.E. you are gathering transaction results for post processing and {@link GattConnection#getGattState()}
     * is now the present state of the system.
     * @return The result state as of the end of the transaction
     */

    public GattState getResultState() { return this.resultState; }

    /**
     * Gets the transaction results
     * @return The list of transaction results
     */
    public List<TransactionResult> getTransactionResults(){
        return this.transactionResults;
    }

    /**
     * The builder for the transaction result
     */
    public static class Builder {

        private int responseStatus;
        private int requestId;
        private UUID characteristicUuid;
        private UUID serviceUuid;
        private UUID descriptorUuid;
        private GattState resultState;
        private TransactionResultStatus resultStatus;
        private byte[] data;
        private int rssi;
        private int mtu;
        private int offset;
        private int txPhy = 1; /** or {@link BluetoothDevice.PHY_LE_1M} because it's default for BLE */
        private int rxPhy = 1; /** or {@link BluetoothDevice.PHY_LE_1M} because it's default for BLE */
        private List<BluetoothGattService> services = new ArrayList<>();
        private boolean preparedWrite;
        private boolean responseRequired;
        private String transactionName = "Unknown";
        private ArrayList<TransactionResult> results = new ArrayList<>();

        public Builder() {

        }

        /**
         * Add a batch of transaction results
         * @param results The results to add as a batch
         * @return This builder
         */
        public Builder addTransactionResults(List<TransactionResult> results) {
            this.results.addAll(results);
            return this;
        }

        /**
         * Add transaction result
         * @param transactionResult the transaction result to be added
         * @return This builder
         */
        public Builder addTransactionResult(TransactionResult transactionResult) {
            this.results.add(transactionResult);
            return this;
        }

        /**
         * Adds a list of {@link BluetoothGattService} to this builder
         * @param services The list of services
         * @return This builder
         */
        public Builder serverServices(List<BluetoothGattService> services){
            this.services = services;
            return this;
        }

        /**
         * Adds data to this builder
         * @param data The data
         * @return This builder
         */
        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        /**
         * Adds the response status to this builder
         * @param code The repsonse status ordinal
         * @return This builder
         */
        public Builder responseStatus(int code) {
            this.responseStatus = code;
            return this;
        }

        /**
         * Adds an rssi value to this builder
         * @param rssi The rssi
         * @return This builder
         */
        public Builder rssi(int rssi) {
            this.rssi = rssi;
            return this;
        }
        /**
         * Adds an mtu value to this builder
         * @param mtu The mtu
         * @return This builder
         */
        public Builder mtu(int mtu) {
            this.mtu = mtu;
            return this;
        }

        /**
         * Adds a requestId to this builder
         * @param requestId The request id
         * @return This builder
         */
        public Builder requestId(int requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Adds a characteristic UUID to this builder
         * @param characteristicUuid The characteristic UUID
         * @return This builder
         */
        public Builder characteristicUuid(UUID characteristicUuid) {
            this.characteristicUuid = characteristicUuid;
            return this;
        }

        /**
         * Adds a service UUID to this builder
         * @param serviceUuid The service UUID
         * @return This builder
         */
        public Builder serviceUuid(UUID serviceUuid) {
            this.serviceUuid = serviceUuid;
            return this;
        }

        /**
         * Adds a descriptor UUID to this builder
         * @param descriptorUuid The descriptor UUID
         * @return This builder
         */
        public Builder descriptorUuid(UUID descriptorUuid) {
            this.descriptorUuid = descriptorUuid;
            return this;
        }

        /**
         * Adds a {@link GattState} to this builder
         * @param state The gatt state
         * @return This builder
         */
        public Builder gattState(GattState state) {
            this.resultState = state;
            return this;
        }

        /**
         * Adds a result status for the transaction to this builder
         * @param resultStatus The transaction result status
         * @return This builder
         */
        public Builder resultStatus(TransactionResultStatus resultStatus) {
            this.resultStatus = resultStatus;
            return this;
        }

        /**
         * Adds an offset to this builder
         * @param offset The offset for the data transmission
         * @return This builder
         */
        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Adds a preparedWrite boolean to this builder
         * @param preparedWrite Whether this is a prepared write or not
         * @return This builder
         */

        public Builder preparedWrite(boolean preparedWrite) {
            this.preparedWrite = preparedWrite;
            return this;
        }
        /**
         * Adds a responseRequired boolean to this builder
         * @param responseRequired Whether this requires a response or not
         * @return This builder
         */
        public Builder responseRequired(boolean responseRequired) {
            this.responseRequired = responseRequired;
            return this;
        }

        /**
         * Will add a transaction name or "unknown" if null is provided
         * @param transactionName The transaction name
         * @return This builder
         */
        public Builder transactionName(@Nullable String transactionName) {
            if(transactionName == null) {
                this.transactionName = "Unknown";
            } else {
                this.transactionName = transactionName;
            }
            return this;
        }

        public Builder txPhy(int txPhy) {
            this.txPhy = txPhy;
            return this;
        }

        public Builder rxPhy(int rxPhy) {
            this.rxPhy = rxPhy;
            return this;
        }

        /**
         * Will construct an instance of a {@link TransactionResult}
         * @return the {@link TransactionResult} with the properties described in the builder
         */
        public TransactionResult build() {
            return new TransactionResult(resultState, resultStatus, responseStatus, rssi,
                    mtu, requestId, characteristicUuid, serviceUuid,
                    descriptorUuid, data, offset, services, preparedWrite, responseRequired, transactionName, txPhy, rxPhy, results);
        }

    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Transaction Name: %s, Gatt State: %s, Transaction Result Status: %s, Response Status: %s, rssi: %d, mtu: %d, Characteristic UUID: %s, Service UUID: %s, Descriptor UUID: %s, Data: %s, Offset: %d, txPhy: %d, rxPhy: %d, transaction results: %s", this.transactionName, this.resultState, this.resultStatus, GattStatus.getStatusForCode(this.responseStatus), this.rssi, this.mtu, this.characteristicUuid, this.serviceUuid, this.descriptorUuid, Bytes.byteArrayToHexString(this.data), this.offset, this.txPhy, this.rxPhy, this.transactionResults);
    }
}
