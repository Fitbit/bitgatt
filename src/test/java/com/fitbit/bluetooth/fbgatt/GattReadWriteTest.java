/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import com.fitbit.bluetooth.fbgatt.tx.CreateBondTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestGattConnectionIntervalTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadGattDescriptorMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.ReadRssiMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.RequestGattConnectionIntervalMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.RequestMtuGattMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattCharacteristicMockTransaction;
import com.fitbit.bluetooth.fbgatt.tx.mocks.WriteGattDescriptorMockTransaction;
import com.fitbit.bluetooth.fbgatt.util.GattStatus;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import com.fitbit.bluetooth.fbgatt.util.LooperWatchdog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.stubbing.Answer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class GattReadWriteTest {

    private static final String MOCK_ADDRESS = "02:00:00:00:00:00";
    private static final UUID characteristicUuid = UUID.fromString("BE378669-3D69-43D8-BCC6-39E8769FAF13");
    private GattConnection conn;

    @Before
    public void before() {
        Context ctx = mock(Context.class);
        when(ctx.getApplicationContext()).thenReturn(ctx);
        Handler mockHandler = mock(Handler.class);
        Looper mockLooper = mock(Looper.class);
        Thread mockThread = mock(Thread.class);
        when(mockThread.getName()).thenReturn("Irvin's mock thread");
        when(mockLooper.getThread()).thenReturn(mockThread);
        when(mockHandler.getLooper()).thenReturn(mockLooper);
        when(mockHandler.postDelayed(any(Runnable.class), anyLong())).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        when(mockHandler.post(any(Runnable.class))).thenAnswer((Answer) invocation -> {
            Runnable msg = invocation.getArgument(0);
            msg.run();
            return null;
        });
        FitbitBluetoothDevice device = new FitbitBluetoothDevice(MOCK_ADDRESS, "fooDevice", mock(BluetoothDevice.class));
        conn = spy(new GattConnection(device, mockLooper));
        conn.setMockMode(true);
        when(conn.getMainHandler()).thenReturn(mockHandler);
        FitbitGatt.getInstance().setAsyncOperationThreadWatchdog(mock(LooperWatchdog.class));
        FitbitGatt.getInstance().startGattClient(ctx);
        FitbitGatt.getInstance().putConnectionIntoDevices(device, conn);
        conn.setState(GattState.IDLE);
    }

    @After
    public void after() {
        FitbitGatt.getInstance().shutdown();
        FitbitGatt.setInstance(null);
    }

    @Test
    public void readCharacteristicTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        ReadGattCharacteristicMockTransaction readChar = new ReadGattCharacteristicMockTransaction(conn, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        conn.runTx(readChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.resultState.equals(readChar.getSuccessState()));
        });
    }

    @Test
    public void writeCharacteristicTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        WriteGattCharacteristicMockTransaction writeChar = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        conn.runTx(writeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.resultState.equals(writeChar.getSuccessState()));
        });
    }
    @Test
    public void writeCharacteristicWithBtOffTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.randomUUID(), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        WriteGattCharacteristicMockTransaction writeChar = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        conn.setState(GattState.BT_OFF);
        conn.runTx(writeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.FAILURE) && result.resultState.equals(writeChar.getSuccessState()));
        });
    }


    @Test
    public void readCharacteristicChangeDataTest() throws InterruptedException{
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(characteristic.getValue()).thenReturn(fakeData);
        when(characteristic.getUuid()).thenReturn(characteristicUuid);
        when(characteristic.getPermissions()).thenReturn(BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        when(characteristic.getProperties()).thenReturn(BluetoothGattCharacteristic.PROPERTY_READ);
        ReadGattCharacteristicTransaction readChar = new ReadGattCharacteristicTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic);
        CountDownLatch cdl = new CountDownLatch(1);
        readChar.callback = result -> {
            fakeData[3] = 'x';
            Assert.assertNotEquals(Arrays.hashCode(fakeData), Arrays.hashCode(result.getData()));
            cdl.countDown();
        };
        // the copy would happen before the callback were delivered in the tx, so this is still
        // valid
        readChar.onCharacteristicRead(null, new GattUtils().copyCharacteristic(characteristic), GattStatus.GATT_SUCCESS.getCode());
        cdl.await();
    }

    @Test
    public void readDescriptorTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(characteristicUuid);
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        ReadGattDescriptorMockTransaction writeChar = new ReadGattDescriptorMockTransaction(conn, GattState.READ_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        conn.runTx(writeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.resultState.equals(writeChar.getSuccessState()));
        });
    }

    @Test
    public void writeDescriptorTest() {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(characteristicUuid);
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        WriteGattDescriptorMockTransaction writeChar = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        conn.runTx(writeChar, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.resultState.equals(writeChar.getSuccessState()));
        });
    }

    @Test
    public void readDescriptorChangeDataTest() throws InterruptedException {
        final byte[] fakeData = new byte[]{'a','b','c','d','e','f','g','h','i', 'j'};
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        // this should be the same as the real thing, the data inside could change as it's pointing
        // to something else.
        when(descriptor.getValue()).thenReturn(fakeData);
        when(descriptor.getUuid()).thenReturn(characteristicUuid);
        when(descriptor.getPermissions()).thenReturn(BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        ReadGattDescriptorTransaction readChar = new ReadGattDescriptorTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, descriptor);
        CountDownLatch cdl = new CountDownLatch(1);
        readChar.callback = result -> {
            fakeData[3] = 'x';
            Assert.assertNotEquals(Arrays.hashCode(fakeData), Arrays.hashCode(result.getData()));
            cdl.countDown();
        };
        // the copy would happen before the callback were delivered in the tx, so this is still
        // valid
        readChar.onDescriptorRead(null, new GattUtils().copyDescriptor(descriptor), GattStatus.GATT_SUCCESS.getCode());
        cdl.await();
    }

    @Test
    public void readRssiTest() {
        ReadRssiMockTransaction mockTransaction = new ReadRssiMockTransaction(conn, GattState.READ_RSSI_SUCCESS, false);
        conn.runTx(mockTransaction, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS));
        });
    }

    @Test
    public void requestMtuTest() {
        RequestMtuGattMockTransaction mockTransaction = new RequestMtuGattMockTransaction(conn, GattState.REQUEST_MTU_SUCCESS, 185, false);
        conn.runTx(mockTransaction, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS) && result.getMtu() == 185);
        });
    }

    @Test
    public void requestCiChange() {
        RequestGattConnectionIntervalMockTransaction ciMock = new RequestGattConnectionIntervalMockTransaction(conn,
                GattState.REQUEST_CONNECTION_INTERVAL_SUCCESS,
                RequestGattConnectionIntervalTransaction.Speed.HIGH,
                false);
        conn.runTx(ciMock, result -> {
            assert(result.resultStatus.equals(TransactionResult.TransactionResultStatus.SUCCESS));
        });
    }

    @Test
    public void nullTransactionDeviceTest() {
        // this could be any transaction, it's just convenient to use the bond transaction
        CreateBondTransaction bondTx = spy(new CreateBondTransaction(conn, GattState.CREATE_BOND_SUCCESS));
        when(bondTx.getDevice()).thenReturn(null);
        conn.runTx(bondTx, result -> {
            // no device, so it should fail
            Assert.assertEquals("Bond failure!", TransactionResult.TransactionResultStatus.FAILURE, result.getResultStatus());
        });
        // shouldn't crash on run tx
    }

    @Test
    public void provideBluetoothStatusToWriteGattDescriptorTransaction() {
        BluetoothGatt gatt = mock(BluetoothGatt.class);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        when(descriptor.getUuid()).thenReturn(characteristicUuid);
        WriteGattDescriptorTransaction writeTx = new WriteGattDescriptorTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor);
        writeTx.callback = result -> {
            try {
                // should force the index out of bounds if code doesn't work
                Assert.assertTrue("We can just skate here, all good", true);
            } catch(ArrayIndexOutOfBoundsException ex) {
                Assert.fail("This must be converted to a normal ordinal");
            }
        };
        writeTx.onDescriptorWrite(gatt, new GattUtils().copyDescriptor(descriptor), 133);
    }

    @Test
    public void provideBluetoothStatusToWriteGattCharacteristicTransaction() {
        BluetoothGatt gatt = mock(BluetoothGatt.class);
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(characteristicUuid);
        WriteGattCharacteristicTransaction writeTx = new WriteGattCharacteristicTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic);
        writeTx.callback = result -> {
            try {
                // should force the index out of bounds if code doesn't work
                Assert.assertTrue("We can just skate here, all good", true);
            } catch(ArrayIndexOutOfBoundsException ex) {
                Assert.fail("This must be converted to a normal ordinal");
            }
        };
        writeTx.onCharacteristicWrite(gatt, new GattUtils().copyCharacteristic(characteristic), 133);
    }

    @Test
    public void provideBluetoothStatusToReadGattCharacteristicTransaction() {
        BluetoothGatt gatt = mock(BluetoothGatt.class);
        BluetoothGattCharacteristic characteristic = mock(BluetoothGattCharacteristic.class);
        when(characteristic.getUuid()).thenReturn(characteristicUuid);
        ReadGattCharacteristicTransaction readTx = new ReadGattCharacteristicTransaction(conn, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic);
        readTx.callback = result -> {
            try {
                // should force the index out of bounds if code doesn't work
                Assert.assertTrue("We can just skate here, all good", true);
            } catch(ArrayIndexOutOfBoundsException ex) {
                Assert.fail("This must be converted to a normal ordinal");
            }
        };
        readTx.onCharacteristicRead(gatt, new GattUtils().copyCharacteristic(characteristic), 133);
    }

    @Test
    public void provideBluetoothStatusToReadGattDescriptorTransaction() {
        BluetoothGatt gatt = mock(BluetoothGatt.class);
        BluetoothGattDescriptor descriptor = mock(BluetoothGattDescriptor.class);
        when(descriptor.getUuid()).thenReturn(characteristicUuid);
        ReadGattDescriptorTransaction readTx = new ReadGattDescriptorTransaction(conn, GattState.READ_DESCRIPTOR_SUCCESS, descriptor);
        readTx.callback = result -> {
            try {
                // should force the index out of bounds if code doesn't work
                Assert.assertTrue("We can just skate here, all good", true);
            } catch(ArrayIndexOutOfBoundsException ex) {
                Assert.fail("This must be converted to a normal ordinal");
            }
        };
        readTx.onDescriptorRead(gatt, new GattUtils().copyDescriptor(descriptor), 133);
    }
}
