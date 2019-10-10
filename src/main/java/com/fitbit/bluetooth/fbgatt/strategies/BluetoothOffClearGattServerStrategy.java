package com.fitbit.bluetooth.fbgatt.strategies;

import com.fitbit.bluetooth.fbgatt.AndroidDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;

import android.bluetooth.BluetoothGattServer;
import android.os.Handler;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 *
 * We want to do this as a strategy as we have seen that disabling bluetooth / enabling bluetooth
 * has different behaviors on different handsets and Android versions.
 *
 * Making this even more complicated, the implementation inferred by looking at source and behavior
 * of Android is that on at least some Android versions, <= 8.0, there is a queue of some sort
 * below or even being the JNI, and that queue is cleared per client connection when BT is disabled
 * however for the gatt server connection, the command queue does not seem to be cleared when BT is
 * disabled, but resumes when BT is re-enabled, so the clear command can occur after BT is turned on
 * leading a developer to add services but for them to disappear.  Another behavior that we have
 * observed is that by the time BT off is called the bluetooth service has been stopped, leading to
 * clear causing a NullPointerException and therefore services not being removed.
 *
 * Some OnePlus phones appear to re-assign the same instance when BT is enabled again giving back
 * the original gatt server_if when BluetoothManager#openGattServer(callback) is called, where
 * adding services again duplicate the services.
 *
 * Another odd failure is where the old queue is still around, but you obtain a new instance, when
 * you get gatt services the new instance will return that there are no services, however the old
 * services are still on the old instance ... a remote device will show duplicate services then you
 * can have a race when the old queue is re-enabled due to BT on and when you add service where the
 * old inaccessible queue / instance can still clear services so you have duplicate at first, then
 * a phantom clear that occurs ... for that reason before adding services we will add a delay also
 * in a strategy.
 *
 * This strategy should only be applied in bluetooth turning off not bluetooth off to give it the
 * best chance for success.  BluetoothGattServer#close() should never be used on Samsung devices as
 * it makes it impossible to add services again until the phone restart, the instance you get back
 * when BT is re-enabled will still be closed, so addService will return false.
 *
 * Since this behavior is multi-dimensional based on phone / OS / OEM it should be strategically applied.
 *
 * Created by Irvin Owens Jr on 9/10/2019
 */

public class BluetoothOffClearGattServerStrategy extends Strategy {

    public BluetoothOffClearGattServerStrategy(@Nullable GattConnection connection, AndroidDevice currentAndroidDevice) {
        super(connection, currentAndroidDevice);
    }

    @Override
    public void applyStrategy() {
        // we want to clear the services on the server, however we must make sure that we do not
        // crash in a stack NPE if the bluetooth service has crashed or is shut down, and we want
        // to make sure that this does not occur on the main thread since this probably transits
        // the JNI and could lead to a ANR
        Handler strategyHandler = new Handler(FitbitGatt.getInstance().getFitbitGattAsyncOperationThread().getLooper());
        strategyHandler.post(() -> {
            GattServerConnection serverConn = FitbitGatt.getInstance().getServer();
            if(serverConn == null) {
                Timber.i("Server connection was null when trying to execute strategy");
                return;
            }
            // make it disconnected so that no one can use it
            serverConn.setState(GattState.DISCONNECTED);
            BluetoothGattServer gattServer = serverConn.getServer();
            if(gattServer == null) {
                Timber.i("Android BluetoothGattServer instance was null when trying to execute strategy");
                return;
            }
            try {
                /* this may not execute instantly, so considering some sort of delay between
                 * clear, close and release ... in the downside of a delay is that the BT service
                 * may go away while we are waiting, so opting for now to just go for it and hope
                 * that the queue system does the right thing.
                 */
                gattServer.clearServices();
            } catch (NullPointerException e) {
                Timber.w(e, "There was an internal stack NPE, the Android BluetoothService probably crashed or already shut down");
            }
            serverConn.setState(GattState.IDLE);
        });
    }
}
