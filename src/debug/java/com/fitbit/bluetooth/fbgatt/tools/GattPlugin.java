/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.tools;

import com.fitbit.bluetooth.fbgatt.BuildConfig;
import com.fitbit.bluetooth.fbgatt.ConnectionEventListener;
import com.fitbit.bluetooth.fbgatt.FitbitBluetoothDevice;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.ServerConnectionEventListener;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceCharacteristicDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.AddGattServerServiceTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ClearServerServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.CloseGattTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattClientRefreshGattTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattDisconnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattServerConnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattServerDisconnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.NotifyGattServerCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattClientPhyTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicDescriptorValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattServerCharacteristicValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadRssiTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RemoveGattServerServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestGattClientPhyChangeTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestGattConnectionIntervalTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestMtuGattTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SendGattServerResponseTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SubscribeToCharacteristicNotificationsTransaction;
import com.fitbit.bluetooth.fbgatt.tx.UnSubscribeToGattCharacteristicNotificationsTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattServerCharacteristicDescriptorValueTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattServerCharacteristicValueTransaction;
import com.fitbit.bluetooth.fbgatt.util.Bytes;
import com.fitbit.bluetooth.fbgatt.util.GattUtils;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.ParcelUuid;
import com.facebook.stetho.dumpapp.ArgsHelper;
import com.facebook.stetho.dumpapp.DumpException;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL;
import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;
import static java.util.Locale.ENGLISH;

/**
 * An implementation of a stetho gatt plugin
 * <p>
 * Created by iowens on 7/12/18.
 */
public class GattPlugin implements DumperPlugin, FitbitGatt.FitbitGattCallback, ConnectionEventListener, FitbitBluetoothDevice.DevicePropertiesChangedCallback {
    private final FitbitGatt fitbitGatt;
    private final Map<String, GattConnection> clientConnections = new HashMap<>();
    protected final Context context;
    protected boolean isJsonFormat = false;
    private ServerConnectionListener serverConnectionListener;

    // Constants
    // Indicates a command passed
    protected static final String PASS_STATUS = "pass";

    // Indicates a command failed
    protected static final String FAIL_STATUS = "fail";

    // JSON Keys
    protected static final String COMMAND_KEY = "command";
    protected static final String STATUS_KEY = "status";
    protected static final String RESULT_KEY = "result";
    protected static final String ERROR_KEY = "error";

    // JSON Result object keys
    private static final String RESULT_RSSI_KEY = "rssi";
    private static final String RESULT_SERVICE_UUID_KEY = "service_uuid";
    private static final String RESULT_SERVICE_TYPE_KEY = "type";
    private static final String RESULT_CHARACTERISTIC_UUID_KEY = "characteristic_UUID";
    private static final String RESULT_DESCRIPTOR_UUID_KEY = "descriptor_uuid";
    private static final String RESULT_PERMISSIONS_KEY = "permissions";
    private static final String RESULT_PROPERTIES_KEY = "properties";
    private static final String RESULT_VALUE_KEY = "value";
    private static final String RESULT_CHARACTERISTIC_VALUE = "characteristic_value";
    private static final String RESULT_DESCRIPTOR_VALUE = "descriptor_value";
    private static final String GATT_SERVER_CHARACTERISTIC_READ_REQUEST_VALUE = "server_characteristic_read_request";
    private static final String GATT_SERVER_CHARACTERISTIC_WRITE_REQUEST_VALUE = "server_characteristic_write_request";
    private static final String GATT_SERVER_DESCRIPTOR_READ_REQUEST_VALUE = "server_descriptor_read_request";
    private static final String GATT_SERVER_DESCRIPTOR_WRITE_REQUEST_VALUE = "server_descriptor_write_request";
    private static final String GATT_SERVER_CONNECTION_STATE_CHANGE_VALUE = "server_connection_state_change";
    private static final String GATT_SERVER_MTU_CHANGE_VALUE = "server_mtu_change_value";
    private static final String GATT_CLIENT_CONNECTION_STATE_CHANGED = "gatt_client_connection_state_changed";
    private static final String GATT_CLIENT_CHARACTERISTIC_CHANGED = "gatt_client_characteristic_changed";
    private static final String GATT_CLIENT_PERIPHERAL_DEVICE_PROPERTIES_CHANGED = "gatt_client_peripheral_properties_changed";
    private static final String GATT_CLIENT_DISCOVERED_SERVICES = "gatt_client_discovered_services";
    private static final String GATT_CLIENT_CHANGED_PHY = "gatt_client_changed_phy";
    private static final String GATT_CLIENT_CHANGED_MTU = "gatt_client_changed_mtu";
    private DumperContext dumperContext;

    public enum GattCommand {
        HELP("help", "h", "Description: Will print this help"),
        ADD_LOCAL_GATT_SERVER_SERVICE("add-local-gatt-server-service", "algss", "<uuid>\n\nDescription: Will add a local gatt server service to the mobile device"),
        ADD_LOCAL_GATT_SERVER_CHARACTERISTIC("add-local-gatt-server-characteristic", "algsc",
            "<service uuid> <characteristic uuid> <properties int>\n" +
                "Properties Values: PROPERTY_BROADCAST=1, PROPERTY_EXTENDED_PROPS=2, " +
                "PROPERTY_INDICATE=32, PROPERTY_NOTIFY=16, PROPERTY_READ=2, " +
                "PROPERTY_SIGNED_WRITE=64, PROPERTY_WRITE=8, PROPERTY_WRITE_NO_RESPONSE=4\n" +
                " <permissions int>\nPermission Values: PERMISSION_READ=1, " +
                "PERMISSION_READ_ENCRYPTED=2, PERMISSION_READ_ENCRYPTED_MITM=4, " +
                "PERMISSION_WRITE=16, PERMISSION_WRITE_ENCRYPTED=32, " +
                "PERMISSION_WRITE_ENCRYPTED_MITM=64, PERMISSION_WRITE_SIGNED=128, " +
                "PERMISSION_WRITE_SIGNED_MITM=256\n\nDescription: Will add a local gatt server characteristic to a gatt service on the mobile device"),
        ADD_LOCAL_GATT_SERVER_CHARACTERISTIC_DESCRIPTOR("add-local-gatt-server-characteristic-descriptor", "algscd", "<service uuid> <characteristic uuid> <descriptor uuid> <int permission>\nPermission Values: PERMISSION_READ=1, \" +\n" +
            "PERMISSION_READ_ENCRYPTED=2, PERMISSION_READ_ENCRYPTED_MITM=4, " +
            "PERMISSION_WRITE=16, PERMISSION_WRITE_ENCRYPTED=32, " +
            "PERMISSION_WRITE_ENCRYPTED_MITM=64, PERMISSION_WRITE_SIGNED=128, " +
            "PERMISSION_WRITE_SIGNED_MITM=256\n\nDescription: Will add a local gatt server characteristic descriptor to a gatt service on the mobile device"),
        WRITE_LOCAL_GATT_SERVER_CHARACTERISTIC("write-local-gatt-server-characteristic", "wlgsc", "<service uuid> <characteristic uuid> <data>\n\nDescription: Will write to a local gatt server service characteristic on a service on the mobile device"),
        WRITE_LOCAL_GATT_SERVER_CHARACTERISTIC_DESCRIPTOR("write-local-gatt-server-characteristic-descriptor", "wlgscd", "<service uuid> <characteristic uuid> <descriptor uuid> <data>\n\nDescription: Will write to a local gatt server descriptor on a characteristic on a service on the gatt server of the mobile device"),
        READ_LOCAL_GATT_SERVER_CHARACTERISTIC("read-local-gatt-server-characteristic", "rlgsc", "<service uuid> <characteristic uuid>\n\nDescription: Will read out the data value of a local gatt server service characteristic on the mobile device"),
        READ_LOCAL_GATT_SERVER_CHARACTERISTIC_DESCRIPTOR("read-local-gatt-server-characteristic-descriptor", "rlgscd", "<service uuid> <characteristic uuid> <descriptor uuid>\n\nDescription: Will read off the value of a descriptor on a gatt server service characteristic descriptor on the mobile device"),
        CLEAR_LOCAL_GATT_SERVER_SERVICES("clear-local-gatt-server-services", "clgss", "Description: Will remove all hosted service from the local gatt server on the mobile device"),
        CLOSE_GATT_CLIENT("close-gatt-client", "cgc", "<mac>\n\nDescription: Will close the gatt client and release the android client_if handle"),
        INIT_GATT("init", "init", "Description: Will initialize the gatt server and start passively scanning for devices"),
        FIND_NEARBY_DEVICES("find-nearby-devices", "fnd", "Description: Will find nearby, connected, and bonded devices"),
        FIND_NEARBY_DEVICES_WITH_BACKGROUND_SCAN("find-nearby-devices-background", "fndbkgnd", "Description: Will find nearby devices using the pending intent background scan"),
        STOP_BACKGROUND_SCAN("stop-background-scan", "sbs", "Description: Will stop the background scanner"),
        GATT_CLIENT_DISCOVER_SERVICES("gatt-client-discover-services", "gcds", "<mac>\n\nDescription: Will discover services on connected peripheral with the given mac address"),
        GATT_CLIENT_CONNECT("gatt-client-connect", "gcc", "<mac>\n\nDescription: Will connect to the peripheral with the provided mac address"),
        GATT_CLIENT_DISCONNECT("gatt-client-disconnect", "gcd", "<mac>\n\nDescription: Will unregister the android application from the peripheral with the given mac address.  Note, this does not mean that the peripheral is disconnected from the mobile device"),
        GATT_SERVER_CONNECT("gatt-server-connect", "gsc", "<mac>\n\nDescription: Will connect to the peripheral with the given mac address from the local gatt server"),
        GATT_SERVER_DISCONNECT("gatt-server-disconnect", "gsd", "<mac>\n\nDescription: Will unregister the android application's gatt server instance from the peripheral with the given mac address.  Note, this does not mean that the peripheral is disconnected from the mobile device"),
        SHOW_GATT_SERVER_SERVICES("show-gatt-server-services", "sgss", "Description: Will list off hosted gatt server services on the mobile device"),
        SHOW_GATT_SERVER_SERVICE_CHARACTERISTICS("show-gatt-server-service-characteristics", "sgssc", "<service uuid>\n\nDescription: Will list off characteristics hosted by the provided local gatt server service"),
        NOTIFY_GATT_SERVER_CHARACTERISTIC("notify-gatt-server-characteristic", "ngsc", "<mac> <service uuid> <characteristic uuid>\n\nDescription: Will notify on the given server service characteristic that something has changed, this will tell the peripheral that the service has had something done to it if the peripheral has subscribed to notifications on the characteristic that is being notified."),
        READ_GATT_CLIENT_CHARACTERISTIC("read-gatt-client-characteristic", "rgcc", "<mac> <service uuid> <characteristic uuid>\n\nDescription: Will read a value from a characteristic hosted on the peripheral's gatt server for a given service"),
        READ_GATT_CLIENT_DESCRIPTOR("read-gatt-client-descriptor", "rgcd", "<mac> <service uuid> <characteristic uuid> <descriptor uuid>\n\nDescription: Will read a value from a descriptor hosted on the peripheral's gatt server for a given service and characteristic"),
        READ_GATT_CLIENT_RSSI("read-gatt-client-rssi", "rgcr", "<mac>\n\nDescription: Will read the RSSI value from the peripheral with the given mac"),
        READ_GATT_CLIENT_PHY("read-gatt-client-phy", "rgcp", "<mac>\n\nDescription: Will read the gatt client phy"),
        REQUEST_GATT_CLIENT_PHY("request-gatt-client-phy", "rqgcp", "<mac> <txPhy> <rxPhy> <phyOptions>\n\nDescription: Will request a different PHY from the mobile, can be 1, 2, or 3, please see BluetoothDevice#PHY*"),
        REMOVE_GATT_SERVER_SERVICE("remove-gatt-server-service", "rgss", "<service uuid>\n\nDescription: Will remove a service from the local gatt server on the mobile device"),
        REQUEST_GATT_CLIENT_CONNECTION_INTERVAL("request-gatt-client-connection-interval", "rgcci", "<mac> <low|medium|high>\n\nDescription: Will request a new connection interval mapping to one of the values hard-coded into Android from the mobile device"),
        REQUEST_GATT_CLIENT_MTU("request-gatt-client-mtu", "rgcm", "<mac> <mtu> ( must be between 23 and 512 )\n\nDescription: Will request a different MTU size from a peripheral with the given mac address"),
        WRITE_GATT_SERVER_RESPONSE("write-gatt-server-response", "wgsr", "<mac> <request id> <status-int> <offset-int> <data char[]>\n\nDescription: Will send the gatt server response to the peripheral for a read or write request"),
        SUBSCRIBE_TO_GATT_CLIENT_CHARACTERISTIC("subscribe-to-gatt-client-characteristic", "stgcc", "<mac> <service uuid> <characteristic uuid>\n\nDescription: Will subscribe to a particular gatt client characteristic, please remember that you must write to the notification descriptor on the given characteristic to truly have notifications enabled, this command will just route the notifications to this android process"),
        UNSUBSCRIBE_FROM_GATT_CLIENT_CHARACTERISTIC("unsubscribe-from-gatt-client-characteristic", "ufgcc", "<mac> <characteristic uuid>\n\nDescription: Will unsubscribe from a particular gatt client characteristic.  Please remember that you must write the unsubscribe value to the subscription descriptor on the given characteristic to truly unsubscribe from notifications"),
        WRITE_GATT_CHARACTERISTIC("write-gatt-characteristic", "wgc", "<mac>  <service uuid> <characteristic uuid> <data>\n\nDescription: Will write to a remote gatt characteristic hosted on the peripheral with the given service"),
        WRITE_GATT_DESCRIPTOR("write-gatt-descriptor", "wgd", "<mac>  <service uuid> <characteristic uuid> <descriptor uuid> <data>\n\nDescription: Will write to a remote gatt descriptor hosted on the peripheral's gatt server with the given service and characteristic"),
        SET_JSON_OUTPUT_FORMAT("set-json-output", "sjo", "on/off\n\nDescription: Will enable json command line output or disable it"),
        REFRESH_GATT("refresh-gatt", "rgt", "<mac>\n\nDescription: Refresh the gatt on the phone"),
        SHOW_REMOTE_SERVICES("show-remote-services", "srs", "<mac>\n\nDescription: Will show remote services, characteristics, and descriptors available post discovery"),
        READ_GATT_LIB_VERSION("read-gatt-lib-version", "rglv", "Description: Print version of the GATT library in use"),
        READ_NUM_GATT_ACTIVE_CONNECTIONS("read-num-gatt-active-connections", "rngac", "Description: Read number of active connetions on GATT");

        private String fullName;
        private String shortName;
        private String description;

        GattCommand(String fullName, String shortName, String description) {
            this.fullName = fullName;
            this.shortName = shortName;
            this.description = description;
        }

        public String getFullName() {
            return fullName;
        }

        public String getShortName() {
            return shortName;
        }

        public String getDescription() {
            return description;
        }

        public static @Nullable
        GattCommand getEnum(@Nullable String value) {
            if (value == null) {
                return null;
            } else {
                GattCommand[] commands = GattCommand.values();
                for (GattCommand command : commands) {
                    if (command.getFullName().equals(value) || command.getShortName().equals(value)) {
                        return command;
                    }
                }
            }
            return null;
        }
    }

    public GattPlugin(Context context) {
        this.context = context;
        this.fitbitGatt = FitbitGatt.getInstance();
        this.fitbitGatt.registerGattEventListener(this);
        serverConnectionListener = new ServerConnectionListener();
    }

    @Override
    public String getName() {
        return "gatt";
    }

    @Override
    public void dump(DumperContext dumpContext) throws DumpException {
        this.dumperContext = dumpContext;
        if(!BuildConfig.DEBUG) {
            logError(dumpContext, new IllegalStateException("Nope."));
            return;
        }
        List<String> argsList = dumpContext.getArgsAsList();
        Iterator<String> args = argsList.iterator();
        String command = ArgsHelper.nextOptionalArg(args, null);

        GattCommand commandEnum = GattCommand.getEnum(command);
        if (commandEnum == null) {
            logError(dumpContext, new IllegalArgumentException("Provided command does not match"));
            return;
        }
        try {
            switch (commandEnum) {
                case HELP:
                    printAvailableCommands(dumpContext);
                    break;
                case INIT_GATT:
                    startGatt(dumpContext);
                    break;
                case ADD_LOCAL_GATT_SERVER_SERVICE:
                    addLocalGattServerService(dumpContext, args);
                    break;
                case ADD_LOCAL_GATT_SERVER_CHARACTERISTIC:
                    addLocalGattServerCharacteristic(dumpContext, args);
                    break;
                case ADD_LOCAL_GATT_SERVER_CHARACTERISTIC_DESCRIPTOR:
                    addLocalGattServerCharacteristicDescriptor(dumpContext, args);
                    break;
                case WRITE_LOCAL_GATT_SERVER_CHARACTERISTIC:
                    writeLocalGattServerCharacteristic(dumpContext, args);
                    break;
                case READ_LOCAL_GATT_SERVER_CHARACTERISTIC:
                    readLocalGattServerCharacteristic(dumpContext, args);
                    break;
                case WRITE_LOCAL_GATT_SERVER_CHARACTERISTIC_DESCRIPTOR:
                    writeLocalGattServerCharacteristicDescriptor(dumpContext, args);
                    break;
                case READ_LOCAL_GATT_SERVER_CHARACTERISTIC_DESCRIPTOR:
                    readLocalGattServerCharacteristicDescriptor(dumpContext, args);
                    break;
                case CLEAR_LOCAL_GATT_SERVER_SERVICES:
                    clearLocalGattServerServices(dumpContext);
                    break;
                case CLOSE_GATT_CLIENT:
                    closeGattClient(dumpContext, args);
                    break;
                case FIND_NEARBY_DEVICES:
                    findNearbyDevices(dumpContext);
                    break;
                case FIND_NEARBY_DEVICES_WITH_BACKGROUND_SCAN:
                    findNearbyDevicesBackgroundScan(dumpContext);
                    break;
                case STOP_BACKGROUND_SCAN:
                    stopBackgroundScan(dumpContext);
                    break;
                case GATT_CLIENT_DISCOVER_SERVICES:
                    gattClientDiscoverServices(dumpContext, args);
                    break;
                case GATT_CLIENT_CONNECT:
                    gattClientConnect(dumpContext, args);
                    break;
                case GATT_CLIENT_DISCONNECT:
                    gattClientDisconnect(dumpContext, args);
                    break;
                case GATT_SERVER_CONNECT:
                    gattServerConnect(dumpContext, args);
                    break;
                case GATT_SERVER_DISCONNECT:
                    gattServerDisconnect(dumpContext, args);
                    break;
                case SHOW_GATT_SERVER_SERVICES:
                    showGattServerServices(dumpContext);
                    break;
                case SHOW_GATT_SERVER_SERVICE_CHARACTERISTICS:
                    showGattServerServiceCharacteristics(dumpContext, args);
                    break;
                case NOTIFY_GATT_SERVER_CHARACTERISTIC:
                    notifyGattServerCharacteristic(dumpContext, args);
                    break;
                case READ_GATT_CLIENT_CHARACTERISTIC:
                    readGattClientCharacteristic(dumpContext, args);
                    break;
                case READ_GATT_CLIENT_DESCRIPTOR:
                    readGattClientDescriptor(dumpContext, args);
                    break;
                case READ_GATT_CLIENT_RSSI:
                    readGattClientRssi(dumpContext, args);
                    break;
                case REMOVE_GATT_SERVER_SERVICE:
                    removeGattServerService(dumpContext, args);
                    break;
                case REQUEST_GATT_CLIENT_CONNECTION_INTERVAL:
                    requestGattClientConnectionInterval(dumpContext, args);
                    break;
                case REQUEST_GATT_CLIENT_MTU:
                    requestGattClientMtu(dumpContext, args);
                    break;
                case WRITE_GATT_SERVER_RESPONSE:
                    writeGattServerResponse(dumpContext, args);
                    break;
                case SUBSCRIBE_TO_GATT_CLIENT_CHARACTERISTIC:
                    subscribeToGattClientCharacteristic(dumpContext, args);
                    break;
                case UNSUBSCRIBE_FROM_GATT_CLIENT_CHARACTERISTIC:
                    unsubscribeFromGattClientCharacteristic(dumpContext, args);
                    break;
                case WRITE_GATT_CHARACTERISTIC:
                    writeGattCharacteristic(dumpContext, args);
                    break;
                case WRITE_GATT_DESCRIPTOR:
                    writeGattDescriptor(dumpContext, args);
                    break;
                case SET_JSON_OUTPUT_FORMAT:
                    setJsonFormat(dumpContext, args);
                    break;
                case SHOW_REMOTE_SERVICES:
                    showRemoteServices(dumpContext, args);
                    break;
                case READ_GATT_LIB_VERSION:
                    readGattLibVersion(dumpContext);
                    break;
                case REFRESH_GATT:
                    refreshGatt(dumpContext, args);
                    break;
                case REQUEST_GATT_CLIENT_PHY:
                    requestGattClientPhy(dumpContext, args);
                    break;
                case READ_GATT_CLIENT_PHY:
                    readGattClientPhy(dumpContext, args);
                    break;
                case READ_NUM_GATT_ACTIVE_CONNECTIONS:
                    readNumGattActiveConnections(dumpContext);
                    break;
                default:
                    log(dumpContext, "Illegal command provided");
            }
        } catch (Throwable t) {
            logError(dumpContext, t);
        }
    }

    private void refreshGatt(DumperContext dumperContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String mac = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumperContext, new IllegalArgumentException("No remote mac address provided"));
            return;
        }
        GattConnection conn = FitbitGatt.getInstance().getConnectionForBluetoothAddress(FitbitGatt.getInstance().getAppContext(), mac);
        if (conn == null) {
            logError(dumperContext, new IllegalArgumentException("No connection available for provided mac"));
            return;
        }
        if (conn.getGatt() == null) {
            logError(dumperContext, new IllegalStateException("No Gatt client established, you have to at least have connected once"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        GattClientRefreshGattTransaction refresh = new GattClientRefreshGattTransaction(conn, GattState.REFRESH_GATT_SUCCESS);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(refresh, result -> {
            logSuccessOrFailure(result, dumperContext, "Successfully refreshed on " + conn.getDevice(), "Failed refreshing on " + conn.getDevice());
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void showRemoteServices(DumperContext dumpContext, Iterator<String> args) {
        int index = 0;
        String mac = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No remote mac address provided"));
            return;
        }
        int n = 220;
        StringBuilder builder = new StringBuilder();
        String status = PASS_STATUS;
        String error = "";
        if (!isJsonFormat) {
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
            format(dumpContext, "| %1$36s | %2$36s | %3$36s | %4$32s | %5$32s | %6$32s\n",
                "Service UUID", "Characteristic UUID", "Descriptor UUID",
                "Permissions",
                "Properties",
                "Value");
        }
        GattConnection conn = FitbitGatt.getInstance().getConnectionForBluetoothAddress(FitbitGatt.getInstance().getAppContext(), mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No connection available for provided mac"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        JSONArray jsonArray = new JSONArray();
        List<BluetoothGattService> remoteServices = conn.getGatt().getServices();
        for (BluetoothGattService remoteService : remoteServices) {
            List<BluetoothGattCharacteristic> characteristics = remoteService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                String permission = getStringRepresentationOfPermissionsForCharacteristic(characteristic);
                String properties = getStringRepresentationOfPropertiesForCharacteristic(characteristic);
                if (!isJsonFormat) {
                    format(dumpContext, "| %1$36s | %2$36s | %3$36s | %4$32s | %5$32s | %6$32s\n", remoteService.getUuid().toString(), characteristic.getUuid().toString(), "N/A", permission, properties, Bytes.byteArrayToHexString(characteristic.getValue()));
                } else {
                    Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                        put(RESULT_SERVICE_UUID_KEY, remoteService.getUuid().toString());
                        put(RESULT_CHARACTERISTIC_UUID_KEY, characteristic.getUuid().toString());
                        put(RESULT_DESCRIPTOR_UUID_KEY, "N/A");
                        put(RESULT_PERMISSIONS_KEY, permission);
                        put(RESULT_PROPERTIES_KEY, properties);
                        put(RESULT_VALUE_KEY, Bytes.byteArrayToHexString(characteristic.getValue()));
                    }};
                    JSONObject jsonObject = makeJsonObject(map);
                    jsonArray.put(jsonObject);
                }
                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    String descriptorPermission = getStringRepresentationOfPermissionsForDescriptor(descriptor);
                    if (!isJsonFormat) {
                        format(dumpContext, "| %1$36s | %2$36s | %3$36s | %4$32s | %5$32s | %6$32s\n", remoteService.getUuid(),
                            characteristic.getUuid().toString(), descriptor.getUuid().toString(),
                            descriptorPermission, "N/A", Bytes.byteArrayToHexString(descriptor.getValue()));
                    } else {
                        Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                            put(RESULT_SERVICE_UUID_KEY, remoteService.getUuid().toString());
                            put(RESULT_CHARACTERISTIC_UUID_KEY, characteristic.getUuid().toString());
                            put(RESULT_DESCRIPTOR_UUID_KEY, descriptor.getUuid().toString());
                            put(RESULT_PROPERTIES_KEY, "N/A");
                            put(RESULT_PERMISSIONS_KEY, descriptorPermission);
                            put(RESULT_VALUE_KEY, Bytes.byteArrayToHexString(descriptor.getValue()));
                        }};
                        JSONObject jsonObject = makeJsonObject(map);
                        jsonArray.put(jsonObject);
                    }
                }
            }
        }

        if (!isJsonFormat) {
            builder = new StringBuilder();
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
        } else {
            logJsonResult(dumpContext, status, error, jsonArray);
        }
        conn.getDevice().removeDevicePropertiesChangedListener(this);
    }

    private void readGattLibVersion(DumperContext dumpContext) {
        String glversion = "0.0.0"; // For Irvin: Here replace with the correct API

        if (isJsonFormat) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(COMMAND_KEY, "read-gatt-lib-version");
            map.put(STATUS_KEY, PASS_STATUS);
            map.put(RESULT_KEY, glversion);
            JSONObject jsonObject = makeJsonObject(map);
            log(dumpContext, jsonObject.toString());
        } else {
            log(dumpContext, glversion);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    String getStringRepresentationOfPermissionsForDescriptor(BluetoothGattDescriptor descriptor) {
        StringBuilder permissionBuilder = new StringBuilder();
        ArrayList<Integer> permissions = new ArrayList<>(8);
        int descriptorPermissions = descriptor.getPermissions();
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_READ) == BluetoothGattDescriptor.PERMISSION_READ) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_READ);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) == BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) == BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE) == BluetoothGattDescriptor.PERMISSION_WRITE) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) == BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) == BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) == BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED);
        }
        if ((descriptorPermissions & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM) == BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM) {
            permissions.add(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM);
        }
        for (int i = 0; i < permissions.size(); i++) {
            int permission = permissions.get(i);
            switch (permission) {
                case BluetoothGattDescriptor.PERMISSION_READ:
                    permissionBuilder.append("read");
                    break;
                case BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED:
                    permissionBuilder.append("read-encrypted");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE:
                    permissionBuilder.append("write");
                    break;
                case BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM:
                    permissionBuilder.append("read-encrypted-mitm");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED:
                    permissionBuilder.append("write-encrypted");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM:
                    permissionBuilder.append("write-encrypted-mitm");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED:
                    permissionBuilder.append("write-signed");
                    break;
                case BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM:
                    permissionBuilder.append("write-signed-mitm");
                    break;
                default:
                    permissionBuilder.append("unknown");
            }
            if (i < permissions.size() - 1) {
                permissionBuilder.append(", ");
            }
        }
        return permissionBuilder.toString();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    String getStringRepresentationOfPropertiesForCharacteristic(BluetoothGattCharacteristic characteristic) {
        StringBuilder propertyBuilder = new StringBuilder();
        ArrayList<Integer> properties = new ArrayList<>(8);
        int characteristicProperties = characteristic.getProperties();
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_READ);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_WRITE);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) == BluetoothGattCharacteristic.PROPERTY_BROADCAST) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_BROADCAST);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) == BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_INDICATE);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE);
        }
        if ((characteristicProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
            properties.add(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
        }
        for (int i = 0; i < properties.size(); i++) {
            int property = properties.get(i);
            switch (property) {
                case BluetoothGattCharacteristic.PROPERTY_READ:
                    propertyBuilder.append("read");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_WRITE:
                    propertyBuilder.append("write");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_BROADCAST:
                    propertyBuilder.append("broadcast");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS:
                    propertyBuilder.append("extended-props");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                    propertyBuilder.append("notify");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                    propertyBuilder.append("indicate");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE:
                    propertyBuilder.append("write-signed");
                    break;
                case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                    propertyBuilder.append("write-no-response");
                    break;
                default:
                    propertyBuilder.append("unknown");
            }
            if (i < properties.size() - 1) {
                propertyBuilder.append(", ");
            }
        }
        return propertyBuilder.toString();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    String getStringRepresentationOfPermissionsForCharacteristic(BluetoothGattCharacteristic characteristic) {
        StringBuilder permissionBuilder = new StringBuilder();
        ArrayList<Integer> permissions = new ArrayList<>(8);
        int characteristicPermissions = characteristic.getPermissions();
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_READ) == BluetoothGattCharacteristic.PERMISSION_READ) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) == BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) == BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE) == BluetoothGattCharacteristic.PERMISSION_WRITE) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) == BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) == BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) == BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED);
        }
        if ((characteristicPermissions & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) == BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) {
            permissions.add(BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM);
        }
        for (int i = 0; i < permissions.size(); i++) {
            int permission = permissions.get(i);
            switch (permission) {
                case BluetoothGattCharacteristic.PERMISSION_READ:
                    permissionBuilder.append("read");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
                    permissionBuilder.append("read-encrypted");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM:
                    permissionBuilder.append("read-encrypted-mitm");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE:
                    permissionBuilder.append("write");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
                    permissionBuilder.append("write-encrypted");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM:
                    permissionBuilder.append("write-encrypted-mitm");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED:
                    permissionBuilder.append("write-signed");
                    break;
                case BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM:
                    permissionBuilder.append("write-signed-mitm");
                    break;
                default:
                    permissionBuilder.append("unknown");
            }
            if (i < permissions.size() - 1) {
                permissionBuilder.append(", ");
            }
        }
        return permissionBuilder.toString();
    }

    private void readLocalGattServerCharacteristicDescriptor(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String descriptorString = null;
        while (args.hasNext()) {
            if (index == 0) {
                serviceString = args.next();
            } else if (index == 1) {
                characteristicString = args.next();
            } else if (index == 2) {
                descriptorString = args.next();
            }
            index++;
        }
        if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (descriptorString == null) {
            logError(dumpContext, new IllegalArgumentException("No descriptor uuid provided"));
            return;
        }
        GattServerConnection conn = fitbitGatt.getServer();
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid gatt server available"));
            return;
        }
        BluetoothGattService localService = conn.getServer().getService(UUID.fromString(serviceString));
        if (localService == null) {
            logError(dumpContext, new IllegalStateException("No service for the uuid " + serviceString + " found"));
            return;
        }
        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(characteristicString));
        if (localCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid " + characteristicString + " found"));
            return;
        }
        BluetoothGattDescriptor localDescriptor = localCharacteristic.getDescriptor(UUID.fromString(descriptorString));
        if (localDescriptor == null) {
            logError(dumpContext, new IllegalStateException("No descriptor for the uuid " + descriptorString + " found"));
            return;
        }
        ReadGattServerCharacteristicDescriptorValueTransaction tx = new ReadGattServerCharacteristicDescriptorValueTransaction(conn, GattState.READ_DESCRIPTOR_SUCCESS, localService, localCharacteristic, localDescriptor);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote to " + localDescriptor.getUuid().toString() + " on " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString(), "Failed writing to " + localDescriptor.getUuid().toString() + " on " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString());
            cdl.countDown();
        });
        cdl.await();
    }

    private void readLocalGattServerCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        while (args.hasNext()) {
            if (index == 0) {
                serviceString = args.next();
            } else if (index == 1) {
                characteristicString = args.next();
            }
            index++;
        }
        if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        }
        GattServerConnection conn = fitbitGatt.getServer();
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid gatt server available"));
            return;
        }
        BluetoothGattService localService = conn.getServer().getService(UUID.fromString(serviceString));
        if (localService == null) {
            logError(dumpContext, new IllegalStateException("No service for the uuid " + serviceString + " found"));
            return;
        }
        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(characteristicString));
        if (localCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid " + characteristicString + " found"));
            return;
        }
        ReadGattServerCharacteristicValueTransaction tx = new ReadGattServerCharacteristicValueTransaction(conn, GattState.READ_CHARACTERISTIC_SUCCESS, localService, localCharacteristic);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote to " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString(), "Failed writing to " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString());
            cdl.countDown();
        });
        cdl.await();
    }

    private void writeLocalGattServerCharacteristicDescriptor(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String descriptorString = null;
        String data = null;
        while (args.hasNext()) {
            if (index == 0) {
                serviceString = args.next();
            } else if (index == 1) {
                characteristicString = args.next();
            } else if (index == 2) {
                descriptorString = args.next();
            } else if (index == 3) {
                data = args.next();
            }
            index++;
        }
        if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (descriptorString == null) {
            logError(dumpContext, new IllegalArgumentException("No descriptor uuid provided"));
            return;
        } else if (data == null) {
            logError(dumpContext, new IllegalArgumentException("No data provided"));
            return;
        }
        GattServerConnection conn = fitbitGatt.getServer();
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid gatt server available"));
            return;
        }
        BluetoothGattService localService = conn.getServer().getService(UUID.fromString(serviceString));
        if (localService == null) {
            logError(dumpContext, new IllegalStateException("No service for the uuid " + serviceString + " found"));
            return;
        }
        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(characteristicString));
        if (localCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid " + characteristicString + " found"));
            return;
        }
        BluetoothGattDescriptor localDescriptor = localCharacteristic.getDescriptor(UUID.fromString(descriptorString));
        if (localDescriptor == null) {
            logError(dumpContext, new IllegalStateException("No descriptor for the uuid " + descriptorString + " found"));
            return;
        }
        localDescriptor.setValue(data.getBytes());
        WriteGattServerCharacteristicDescriptorValueTransaction tx = new WriteGattServerCharacteristicDescriptorValueTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, localService, localCharacteristic, localDescriptor, data.getBytes());
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote to " + localDescriptor.getUuid().toString() + " on " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString(), "Failed writing to " + localDescriptor.getUuid().toString() + " on " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString());
            cdl.countDown();
        });
        cdl.await();
    }

    private void writeLocalGattServerCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String data = null;
        while (args.hasNext()) {
            if (index == 0) {
                serviceString = args.next();
            } else if (index == 1) {
                characteristicString = args.next();
            } else if (index == 2) {
                data = args.next();
            }
            index++;
        }
        if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (data == null) {
            logError(dumpContext, new IllegalArgumentException("No data provided"));
            return;
        }
        GattServerConnection conn = fitbitGatt.getServer();
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid gatt server available"));
            return;
        }
        BluetoothGattService localService = conn.getServer().getService(UUID.fromString(serviceString));
        if (localService == null) {
            logError(dumpContext, new IllegalStateException("No service for the uuid " + serviceString + " found"));
            return;
        }
        BluetoothGattCharacteristic localCharacteristic = localService.getCharacteristic(UUID.fromString(characteristicString));
        if (localCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid " + characteristicString + " found"));
            return;
        }
        localCharacteristic.setValue(data.getBytes());
        WriteGattServerCharacteristicValueTransaction tx = new WriteGattServerCharacteristicValueTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, localService, localCharacteristic, data.getBytes());
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote to " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString(), "Failed writing to " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString());
            cdl.countDown();
        });
        cdl.await();
    }

    private void addLocalGattServerCharacteristicDescriptor(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String localServiceUuid = null;
        String characteristicUuid = null;
        String descriptorUuid = null;
        int permissions = -1;
        while (args.hasNext()) {
            if (index == 0) {
                localServiceUuid = args.next();
            } else if (index == 1) {
                characteristicUuid = args.next();
            } else if (index == 2) {
                descriptorUuid = args.next();
            } else if (index == 3) {
                permissions = Integer.parseInt(args.next());
            }
            index++;
        }
        if (localServiceUuid == null) {
            logError(dumpContext, new IllegalArgumentException("No local server service uuid provided"));
            return;
        } else if (characteristicUuid == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (descriptorUuid == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic descriptor uuid provided"));
            return;
        } else if (permissions == -1) {
            logError(dumpContext, new IllegalArgumentException("No characteristic permissions provided"));
            return;
        }
        GattServerConnection conn = fitbitGatt.getServer();
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }
        BluetoothGattService localService = conn.getServer().getService(UUID.fromString(localServiceUuid));
        if (localService == null) {
            logError(dumpContext, new IllegalStateException("No local service for the uuid" + localServiceUuid + "found"));
            return;
        }
        BluetoothGattCharacteristic localCharacteristic =
            localService.getCharacteristic(UUID.fromString(characteristicUuid));
        if (!localService.addCharacteristic(localCharacteristic)) {
            logError(dumpContext, new IllegalStateException("Couldn't get characteristic from service"));
            return;
        }
        BluetoothGattDescriptor localDescriptor = new BluetoothGattDescriptor(UUID.fromString(descriptorUuid), permissions);
        if (!localCharacteristic.addDescriptor(localDescriptor)) {
            logError(dumpContext, new IllegalStateException("Couldn't add descriptor " + descriptorUuid + " to the local characteristic " + characteristicUuid + " on the service " + localServiceUuid));
            return;
        }
        // don't worry, this instance can only be registered once, but we do want for it to have
        // a fresh dumper context
        serverConnectionListener.setContext(dumpContext);
        conn.registerConnectionEventListener(serverConnectionListener);
        CountDownLatch cdl = new CountDownLatch(1);
        AddGattServerServiceCharacteristicDescriptorTransaction tx = new AddGattServerServiceCharacteristicDescriptorTransaction(conn, GattState.ADD_SERVICE_CHARACTERISTIC_DESCRIPTOR_SUCCESS, localService, localCharacteristic, localDescriptor);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext,
                "Successfully added " + localDescriptor.getUuid().toString() + " to " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString(),
                "Failed to add " + localDescriptor.getUuid().toString() + " to " + localCharacteristic.getUuid().toString() + " on " + localService.getUuid().toString());
            cdl.countDown();
        });
        cdl.await();
    }

    private void addLocalGattServerCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String localServiceUuid = null;
        String characteristicUuid = null;
        int permissions = -1;
        int properties = -1;
        while (args.hasNext()) {
            if (index == 0) {
                localServiceUuid = args.next();
            } else if (index == 1) {
                characteristicUuid = args.next();
            } else if (index == 2) {
                properties = Integer.parseInt(args.next());
            } else if (index == 3) {
                permissions = Integer.parseInt(args.next());
            }
            index++;
        }
        if (localServiceUuid == null) {
            logError(dumpContext, new IllegalArgumentException("No local server service uuid provided"));
            return;
        } else if (characteristicUuid == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (permissions == -1) {
            logError(dumpContext, new IllegalArgumentException("No characteristic permissions provided"));
            return;
        } else if (properties == -1) {
            logError(dumpContext, new IllegalArgumentException("No characteristic properties provided"));
            return;
        }
        GattServerConnection conn = fitbitGatt.getServer();
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }
        BluetoothGattService localService = conn.getServer().getService(UUID.fromString(localServiceUuid));
        if (localService == null) {
            logError(dumpContext, new IllegalStateException("No local service for the uuid" + localServiceUuid + "found"));
            return;
        }
        BluetoothGattCharacteristic localCharacteristic =
            new BluetoothGattCharacteristic(UUID.fromString(characteristicUuid), properties, permissions);
        if (!localService.addCharacteristic(localCharacteristic)) {
            logError(dumpContext, new IllegalStateException("Couldn't add characteristic to service"));
            return;
        }
        // don't worry, this instance can only be registered once, but we do want for it to have
        // a fresh dumper context
        serverConnectionListener.setContext(dumpContext);
        conn.registerConnectionEventListener(serverConnectionListener);
        CountDownLatch cdl = new CountDownLatch(1);
        AddGattServerServiceCharacteristicTransaction tx = new AddGattServerServiceCharacteristicTransaction(conn, GattState.ADD_SERVICE_CHARACTERISTIC_SUCCESS, localService, localCharacteristic);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext,
                "Successfully added " + localCharacteristic.getUuid().toString() + " to " + localService.getUuid().toString(),
                "Failed to add " + localCharacteristic.getUuid().toString() + " to " + localService.getUuid().toString());
            cdl.countDown();
        });
        cdl.await();
    }

    private void unsubscribeFromGattClientCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }
        BluetoothGattCharacteristic remoteCharacteristic = conn.getRemoteGattServiceCharacteristic(UUID.fromString(serviceString), UUID.fromString(characteristicString));
        if (remoteCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid" + characteristicString + "found"));
            return;
        }
        conn.unregisterConnectionEventListener(this);
        conn.getDevice().addDevicePropertiesChangedListener(this);
        CountDownLatch cdl = new CountDownLatch(1);
        UnSubscribeToGattCharacteristicNotificationsTransaction tx = new UnSubscribeToGattCharacteristicNotificationsTransaction(conn, GattState.DISABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, remoteCharacteristic);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully unsubscribed from " + remoteCharacteristic.getUuid().toString() + " on " + conn.getDevice(), "Failed unsubscribing to " + remoteCharacteristic.getUuid().toString() + " on " + conn.getDevice());
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void writeGattDescriptor(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String descriptorString = null;
        String mac = null;
        String data = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            } else if (index == 4) {
                data = args.next();
            } else if (index == 3) {
                descriptorString = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (data == null) {
            logError(dumpContext, new IllegalArgumentException("No data provided"));
            return;
        } else if (descriptorString == null) {
            logError(dumpContext, new IllegalArgumentException("No descriptor uuid provided"));
            return;
        } else if (!Bytes.isValidHexString(data)) {
            logError(dumpContext, new IllegalArgumentException("Invalid hex value; e.g. 01, AA00, bb5577 observing complete octets"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }
        BluetoothGattCharacteristic remoteCharacteristic = conn.getRemoteGattServiceCharacteristic(UUID.fromString(serviceString), UUID.fromString(characteristicString));
        if (remoteCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid" + characteristicString + "found"));
            return;
        }
        BluetoothGattDescriptor remoteDescriptor = remoteCharacteristic.getDescriptor(UUID.fromString(descriptorString));
        if (remoteDescriptor == null) {
            logError(dumpContext, new IllegalStateException("No descriptor for the uuid" + descriptorString + "found"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        remoteDescriptor.setValue(Bytes.hexStringToByteArray(data));
        WriteGattDescriptorTransaction tx = new WriteGattDescriptorTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, remoteDescriptor);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote to " + remoteDescriptor.getUuid().toString() + " on " + conn.getDevice(), "Failed writing to " + remoteDescriptor.getUuid().toString() + " on " + conn.getDevice());
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void writeGattCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String mac = null;
        String data = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            } else if (index == 3) {
                data = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        } else if (data == null) {
            logError(dumpContext, new IllegalArgumentException("No data provided"));
            return;
        } else if (!Bytes.isValidHexString(data)) {
            logError(dumpContext, new IllegalArgumentException("Invalid hex value; e.g. 01, AA00, bb5577 observing complete octets"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }
        BluetoothGattCharacteristic remoteCharacteristic = conn.getRemoteGattServiceCharacteristic(UUID.fromString(serviceString), UUID.fromString(characteristicString));
        if (remoteCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid" + characteristicString + "found"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        remoteCharacteristic.setValue(Bytes.hexStringToByteArray(data));
        WriteGattCharacteristicTransaction tx = new WriteGattCharacteristicTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, remoteCharacteristic);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote to " + remoteCharacteristic.getUuid().toString() + " on " + conn.getDevice(), "Failed writing to " + remoteCharacteristic.getUuid().toString() + " on " + conn.getDevice());
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void subscribeToGattClientCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No valid connection for provided mac"));
            return;
        }
        BluetoothGattCharacteristic remoteCharacteristic = conn.getRemoteGattServiceCharacteristic(UUID.fromString(serviceString), UUID.fromString(characteristicString));
        if (remoteCharacteristic == null) {
            logError(dumpContext, new IllegalStateException("No characteristic for the uuid" + characteristicString + "found"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        conn.registerConnectionEventListener(this);
        SubscribeToCharacteristicNotificationsTransaction tx = new SubscribeToCharacteristicNotificationsTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, remoteCharacteristic);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully subscribed to " + remoteCharacteristic.getUuid().toString() + " on " + conn.getDevice(), "Failed subscribing to " + remoteCharacteristic.getUuid().toString() + " on " + conn.getDevice());
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void writeGattServerResponse(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String mac = null;
        String requestId = null;
        String status = null;
        String offset = null;
        String data = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                requestId = args.next();
            } else if (index == 2) {
                status = args.next();
            } else if (index == 3) {
                offset = args.next();
            } else if (index == 4) {
                data = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        }
        if (requestId == null) {
            logError(dumpContext, new IllegalArgumentException("No requestId provided"));
            return;
        }
        if (status == null) {
            logError(dumpContext, new IllegalArgumentException("No status provided"));
            return;
        }
        if (offset == null) {
            logError(dumpContext, new IllegalArgumentException("No offset provided"));
            return;
        }
        if (data == null) {
            logError(dumpContext, new IllegalArgumentException("No data provided"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("Bluetooth connection for mac " + mac + " not found."));
            return;
        }
        FitbitBluetoothDevice fitbitBluetoothDevice = conn.getDevice();
        SendGattServerResponseTransaction tx = new SendGattServerResponseTransaction(fitbitGatt.getServer(),
            GattState.SEND_SERVER_RESPONSE_SUCCESS, fitbitBluetoothDevice,
            Integer.parseInt(requestId), Integer.parseInt(status), Integer.parseInt(offset),
            data.getBytes());
        CountDownLatch cdl = new CountDownLatch(1);
        fitbitGatt.getServer().runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully wrote response to " + conn.getDevice(), "Failed writing response to " + conn.getDevice());
            cdl.countDown();
        });
        cdl.await();
    }

    private void requestGattClientMtu(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String mtu = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                mtu = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        }
        if (mtu == null) {
            logError(dumpContext, new IllegalArgumentException("No mtu"));
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
            return;
        }
        RequestMtuGattTransaction tx = new RequestMtuGattTransaction(conn, GattState.REQUEST_MTU_SUCCESS, Integer.parseInt(mtu));
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully changed mtu to " + result.getMtu() + " on " + conn.getDevice(), "Failed changed mtu to " + result.getMtu() + " on " + conn.getDevice());
            cdl.countDown();
        });
        cdl.await();
    }

    private void readGattClientPhy(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String mac = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
            return;
        }
        ReadGattClientPhyTransaction readGattClientPhyTransaction = new ReadGattClientPhyTransaction(conn, GattState.READ_CURRENT_PHY_SUCCESS);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(readGattClientPhyTransaction, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully read physical layer to tx: " + result.getTxPhy() + ", rx:  " + result.getRxPhy(),
                "Failed in reading physical layer to tx: " + result.getTxPhy() + ", rx: " + result.getRxPhy());
            cdl.countDown();
        });
        cdl.await();
    }

    private void readNumGattActiveConnections(DumperContext dumpContext) {
        String sNum = "";
        BluetoothManager btMan = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if(btMan == null) {
            return;
        }
        List<BluetoothDevice> deviceList = btMan.getConnectedDevices(BluetoothProfile.GATT);
        sNum = Integer.toString(deviceList.size());

        if (isJsonFormat) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(COMMAND_KEY, "read-num-gatt-active-connections");
            map.put(STATUS_KEY, PASS_STATUS);
            map.put(RESULT_KEY, sNum);
            JSONObject jsonObject = makeJsonObject(map);
            log(dumpContext, jsonObject.toString());
        } else {
            log(dumpContext, String.format(ENGLISH, "num-gatt-active-connections=%s",sNum));
        }

        // Log devices info --> Logcat
        Timber.v("num-gatt-active-connections=%s", sNum);
        if (deviceList.size() > 0) {
            Map<String, Object> map = new LinkedHashMap<>();
            GattUtils gattutil = new GattUtils();
            int i = 0;
            for (BluetoothDevice dev : deviceList) {
                if (dev != null) {
                    map.put(COMMAND_KEY, "rngac");
                    map.put("deviceid", i);
                    map.put("addr", dev.getAddress());
                    if (dev.getName() != null) {
                        map.put("name", dev.getName());
                    } else {
                        map.put("name", "Unknown");
                    }
                    map.put("type", gattutil.getDevTypeDescription(dev.getType()));
                    map.put("bond", gattutil.getBondStateDescription(dev.getBondState()));
                    map.put("class", gattutil.getDevClassDescription(dev.getBluetoothClass().getDeviceClass()));
                    // will print a separate JSON for each active connection
                    JSONObject jsonObject = makeJsonObject(map);
                    Timber.v(jsonObject.toString());
                    i++;
                }
            }
        }
    }

    private void requestGattClientPhy(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        int txPhy = -1;
        int rxPhy = -1;
        int phyOptions = -1;
        String mac = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                txPhy = Integer.parseInt(args.next());
            } else if (index == 2) {
                rxPhy = Integer.parseInt(args.next());
            } else if (index == 3) {
                phyOptions = Integer.parseInt(args.next());
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        }
        if (txPhy == -1) {
            logError(dumpContext, new IllegalArgumentException("No tx PHY"));
        }
        if (rxPhy == -1) {
            logError(dumpContext, new IllegalArgumentException("No rx PHY"));
        }
        if (phyOptions == -1) {
            logError(dumpContext, new IllegalArgumentException("No phy options"));
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
            return;
        }
        RequestGattClientPhyChangeTransaction gattClientPhyChangeTransaction = new RequestGattClientPhyChangeTransaction(conn, GattState.REQUEST_PHY_CHANGE_SUCCESS, txPhy, rxPhy, phyOptions);
        CountDownLatch cdl = new CountDownLatch(1);
        int finalTxPhy = txPhy;
        int finalRxPhy = rxPhy;
        conn.runTx(gattClientPhyChangeTransaction, response -> {
            logSuccessOrFailure(response, dumpContext, "Successfully changed physical layer to tx: " + finalTxPhy + ", rx:  " + finalRxPhy,
                "Failed in changing physical layer to tx: " + finalTxPhy + ", rx: " + finalRxPhy);
            cdl.countDown();
        });
        cdl.await();
    }

    private void requestGattClientConnectionInterval(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String connectionInterval = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                connectionInterval = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (connectionInterval == null || (!connectionInterval.equals("low") && !connectionInterval.equals("medium") && !connectionInterval.equals("high"))) {
            logError(dumpContext, new IllegalArgumentException("No valid connection interval provided, must be low|medium|high"));
            return;
        }
        RequestGattConnectionIntervalTransaction.Speed realCI = RequestGattConnectionIntervalTransaction.Speed.MID;
        switch (connectionInterval) {
            case "low":
                realCI = RequestGattConnectionIntervalTransaction.Speed.LOW;
                break;
            case "medium":
                realCI = RequestGattConnectionIntervalTransaction.Speed.MID;
                break;
            case "high":
                realCI = RequestGattConnectionIntervalTransaction.Speed.HIGH;
                break;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        RequestGattConnectionIntervalTransaction tx = new RequestGattConnectionIntervalTransaction(conn, GattState.REQUEST_CONNECTION_INTERVAL_SUCCESS, realCI);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(tx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully changed connection speed on " + conn.getDevice(), "Failed changing connection speed on " + conn.getDevice());
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void removeGattServerService(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String serviceUuid = args.next();
            BluetoothGattService gattService = new BluetoothGattService(UUID.fromString(serviceUuid), BluetoothGattService.SERVICE_TYPE_PRIMARY);
            RemoveGattServerServicesTransaction tx = new RemoveGattServerServicesTransaction(fitbitGatt.getServer(), GattState.ADD_SERVICE_SUCCESS, gattService);
            CountDownLatch cdl = new CountDownLatch(1);
            fitbitGatt.getServer().runTx(tx, result -> {
                logSuccessOrFailure(result, dumpContext, "Successfully Removed Gatt Server Service", "Failed Removing Gatt Server Service");
                cdl.countDown();
            });
            cdl.await();
        } else {
            logError(dumpContext, new IllegalArgumentException("No viable service UUID provided"));
        }
    }

    private void readGattClientRssi(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            FitbitBluetoothDevice device = null;
            GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
            if (conn != null) {
                device = conn.getDevice();
            }
            if (device == null) {
                logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
                return;
            }
            conn.getDevice().addDevicePropertiesChangedListener(this);
            ReadRssiTransaction readRssiTransaction = new ReadRssiTransaction(conn, GattState.READ_RSSI_SUCCESS);
            CountDownLatch cdl = new CountDownLatch(1);
            conn.runTx(readRssiTransaction, result -> {
                if (!isJsonFormat) {
                    log(dumpContext, result.toString());
                    if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        format(dumpContext, "%d db for device with mac : %s", result.getRssi(), mac);
                        logSuccess(dumpContext, "Successfully read rssi from " + mac);
                    } else {
                        logFailure(dumpContext, "Failed reading rssi from " + mac);
                    }
                } else {
                    Map<String, Object> resultMap = new LinkedHashMap<>();
                    resultMap.put(RESULT_RSSI_KEY, result.getRssi());
                    logJsonResult(dumpContext, result, result.toString(), resultMap);
                }
                cdl.countDown();
                conn.getDevice().removeDevicePropertiesChangedListener(this);
            });
            cdl.await();
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void readGattClientDescriptor(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String descriptorString = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            } else if (index == 3) {
                descriptorString = args.next();
            }
            index++;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("Bluetooth connection for mac " + mac + " not found."));
            return;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        }
        BluetoothGattService service = conn.getGatt().getService(UUID.fromString(serviceString));
        if (service == null) {
            logError(dumpContext, new IllegalArgumentException("Remote gatt service not found"));
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicString));
        if (characteristic == null) {
            logError(dumpContext, new IllegalArgumentException("Remote gatt characteristic not found"));
            return;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorString));
        if (descriptor == null) {
            logError(dumpContext, new IllegalArgumentException("Remote gatt descriptor not found"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        ReadGattDescriptorTransaction readTx = new ReadGattDescriptorTransaction(conn, GattState.READ_DESCRIPTOR_SUCCESS, descriptor);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(readTx, result -> {
            if (!isJsonFormat) {
                log(dumpContext, result.toString());
                if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                    logSuccess(dumpContext, "Successfully read descriptor " + descriptor.getUuid());
                    log(dumpContext, Bytes.byteArrayToHexString(result.getData()));
                } else {
                    logFailure(dumpContext, "Failed reading descriptor " + descriptor.getUuid());
                }
            } else {
                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put(RESULT_DESCRIPTOR_VALUE, Bytes.byteArrayToHexString(result.getData()));
                logJsonResult(dumpContext, result, result.toString(), resultMap);
            }
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void readGattClientCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            }
            index++;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("Bluetooth connection for mac " + mac + " not found."));
            return;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        }
        BluetoothGattService service = conn.getGatt().getService(UUID.fromString(serviceString));
        if (service == null) {
            logError(dumpContext, new IllegalArgumentException("Server gatt service not found"));
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicString));
        if (characteristic == null) {
            logError(dumpContext, new IllegalArgumentException("Server gatt characteristic not found"));
            return;
        }
        conn.getDevice().addDevicePropertiesChangedListener(this);
        ReadGattCharacteristicTransaction readTx = new ReadGattCharacteristicTransaction(conn, GattState.READ_CHARACTERISTIC_SUCCESS, characteristic);
        CountDownLatch cdl = new CountDownLatch(1);
        conn.runTx(readTx, result -> {
            if (!isJsonFormat) {
                log(dumpContext, result.toString());
                if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                    logSuccess(dumpContext, "Successfully read characteristic " + characteristic.getUuid());
                    log(dumpContext, Bytes.byteArrayToHexString(result.getData()));
                } else {
                    logFailure(dumpContext, "Failed reading characteristic " + characteristic.getUuid());
                }
            } else {
                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put(RESULT_CHARACTERISTIC_VALUE, Bytes.byteArrayToHexString(result.getData()));
                logJsonResult(dumpContext, result, result.toString(), resultMap);
            }
            cdl.countDown();
            conn.getDevice().removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void notifyGattServerCharacteristic(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        int index = 0;
        String serviceString = null;
        String characteristicString = null;
        String mac = null;
        while (args.hasNext()) {
            if (index == 2) {
                characteristicString = args.next();
            } else if (index == 0) {
                mac = args.next();
            } else if (index == 1) {
                serviceString = args.next();
            }
            index++;
        }
        if (mac == null) {
            logError(dumpContext, new IllegalArgumentException("No bluetooth mac provided"));
            return;
        } else if (serviceString == null) {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
            return;
        } else if (characteristicString == null) {
            logError(dumpContext, new IllegalArgumentException("No characteristic uuid provided"));
            return;
        }
        BluetoothGattService service = fitbitGatt.getServer().getServer().getService(UUID.fromString(serviceString));
        if (service == null) {
            logError(dumpContext, new IllegalArgumentException("Server gatt service not found"));
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicString));
        if (characteristic == null) {
            logError(dumpContext, new IllegalArgumentException("Server gatt characteristic not found"));
            return;
        }
        GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
        if (conn == null) {
            logError(dumpContext, new IllegalArgumentException("Bluetooth connection for mac " + mac + " not found."));
            return;
        }
        FitbitBluetoothDevice fitbitBluetoothDevice = conn.getDevice();
        fitbitBluetoothDevice.addDevicePropertiesChangedListener(this);
        NotifyGattServerCharacteristicTransaction ngsTx = new NotifyGattServerCharacteristicTransaction(fitbitGatt.getServer(), fitbitBluetoothDevice, GattState.NOTIFY_CHARACTERISTIC_SUCCESS, characteristic, false);
        CountDownLatch cdl = new CountDownLatch(1);
        fitbitGatt.getServer().runTx(ngsTx, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully notified gatt server characteristic " + characteristic.getUuid(), "Failed notifying gatt server characteristic " + characteristic.getUuid());
            cdl.countDown();
            fitbitBluetoothDevice.removeDevicePropertiesChangedListener(this);
        });
        cdl.await();
    }

    private void showGattServerServiceCharacteristics(DumperContext dumpContext, Iterator<String> args) {
        int n = 106;
        StringBuilder builder = new StringBuilder();
        String status = PASS_STATUS;
        String error = "";
        if (!isJsonFormat) {
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
            format(dumpContext, "| %1$32s | %2$32s | %3$32s | %4$32s |\n",
                "Characteristic UUID",
                "Permissions",
                "Properties",
                "Value");
        }
        JSONArray jsonArray = new JSONArray();
        if (args.hasNext()) {
            try {
                String serviceName = args.next();
                BluetoothGattService service = fitbitGatt.getServer().getServer().getService(UUID.fromString(serviceName));
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    String permission;
                    String properties;
                    switch (characteristic.getPermissions()) {
                        case BluetoothGattCharacteristic.PERMISSION_READ:
                            permission = "read";
                            break;
                        case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED:
                            permission = "read-encrypted";
                            break;
                        case BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM:
                            permission = "read-encrypted-mitm";
                            break;
                        case BluetoothGattCharacteristic.PERMISSION_WRITE:
                            permission = "write";
                            break;
                        case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED:
                            permission = "write-encrypted";
                            break;
                        case BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM:
                            permission = "write-encrypted-mitm";
                            break;
                        default:
                            permission = "unknown";
                    }
                    switch (characteristic.getProperties()) {
                        case BluetoothGattCharacteristic.PROPERTY_BROADCAST:
                            properties = "broadcast";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS:
                            properties = "extended-props";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                            properties = "indicate";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                            properties = "notify";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_READ:
                            properties = "read";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE:
                            properties = "signed-write";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_WRITE:
                            properties = "write";
                            break;
                        case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                            properties = "write-no-response";
                            break;
                        default:
                            properties = "unknown";
                    }
                    if (!isJsonFormat) {
                        format(dumpContext, "| %1$32s | %2$32s | %3$32s | %4$32s |\n", characteristic.getUuid().toString(), permission, properties, Bytes.byteArrayToHexString(characteristic.getValue()));
                    } else {
                        Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                            put(RESULT_CHARACTERISTIC_UUID_KEY, characteristic.getUuid().toString());
                            put(RESULT_PERMISSIONS_KEY, permission);
                            put(RESULT_PROPERTIES_KEY, properties);
                            put(RESULT_VALUE_KEY, Bytes.byteArrayToHexString(characteristic.getValue()));
                        }};
                        JSONObject jsonObject = makeJsonObject(map);
                        jsonArray.put(jsonObject);
                    }
                }
            } catch (Exception e) {
                status = FAIL_STATUS;
                error = Arrays.toString(e.getStackTrace());
            }
        } else {
            logError(dumpContext, new IllegalArgumentException("No service uuid provided"));
        }

        if (!isJsonFormat) {
            builder = new StringBuilder();
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
        } else {
            logJsonResult(dumpContext, status, error, jsonArray);
        }
    }

    private void showGattServerServices(DumperContext dumpContext) {
        int n = 106;
        StringBuilder builder = new StringBuilder();
        String status = PASS_STATUS;
        String error = "";
        if (!isJsonFormat) {
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
            format(dumpContext, "| %1$32s | %2$32s |\n",
                "Service UUID",
                "Type");
        }
        String serviceUuid;
        String type;
        BluetoothGattServer server = fitbitGatt.getServer().getServer();
        List<BluetoothGattService> services = server.getServices();
        JSONArray jsonArray = new JSONArray();
        try {
            for (BluetoothGattService service : services) {
                serviceUuid = service.getUuid().toString();
                switch (service.getType()) {
                    case BluetoothGattService.SERVICE_TYPE_PRIMARY:
                        type = "primary";
                        break;
                    case BluetoothGattService.SERVICE_TYPE_SECONDARY:
                        type = "secondary";
                        break;
                    default:
                        type = "unknown";
                }
                if (!isJsonFormat) {
                    format(dumpContext, "| %1$32s | %2$32s |\n", serviceUuid, type);
                } else {
                    Map<String, Object> map = new LinkedHashMap<String, Object>();
                    map.put(RESULT_SERVICE_UUID_KEY, serviceUuid);
                    map.put(RESULT_SERVICE_TYPE_KEY, type);
                    JSONObject jsonObject = makeJsonObject(map);
                    jsonArray.put(jsonObject);
                }
            }
        } catch (Exception e) {
            status = FAIL_STATUS;
            error = Arrays.toString(e.getStackTrace());
        }

        builder = new StringBuilder();
        if (!isJsonFormat) {
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
        } else {
            logJsonResult(dumpContext, status, error, jsonArray);
        }
    }

    private void gattServerDisconnect(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            FitbitBluetoothDevice device = null;
            GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
            if (conn != null) {
                device = conn.getDevice();
            }
            if (device == null) {
                logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
                return;
            }
            conn.getDevice().addDevicePropertiesChangedListener(this);
            GattServerDisconnectTransaction disconnectTx = new GattServerDisconnectTransaction(fitbitGatt.getServer(), GattState.DISCONNECTED, device);
            CountDownLatch cdl = new CountDownLatch(1);
            fitbitGatt.getServer().runTx(disconnectTx, result -> {
                logSuccessOrFailure(result, dumpContext, "Successfully Server Disconnected from " + mac, "Failed Server Disconnecting from " + mac);
                cdl.countDown();
                conn.getDevice().removeDevicePropertiesChangedListener(this);
            });
            cdl.await();
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void gattServerConnect(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            FitbitBluetoothDevice device = null;
            GattConnection conn = fitbitGatt.getConnectionForBluetoothAddress(context, mac);
            if (conn != null) {
                device = conn.getDevice();
            }
            if (device == null) {
                logError(dumpContext, new IllegalArgumentException("No device for mac address provided"));
                return;
            }
            GattServerConnectTransaction serverConnectTx = new GattServerConnectTransaction(fitbitGatt.getServer(), GattState.CONNECTED, device);
            CountDownLatch cdl = new CountDownLatch(1);
            fitbitGatt.getServer().runTx(serverConnectTx, result -> {
                logSuccessOrFailure(result, dumpContext, "Successfully Server Connected to " + mac, "Failed Server Connecting to " + mac);
                cdl.countDown();
            });
            cdl.await();
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void gattClientDisconnect(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            GattConnection conn = clientConnections.get(mac);
            if (conn == null) {
                logError(dumpContext, new IllegalStateException("No connected client for mac " + mac));
            } else {
                conn.getDevice().addDevicePropertiesChangedListener(this);
                conn.unregisterConnectionEventListener(this);
                GattDisconnectTransaction disconnectTx = new GattDisconnectTransaction(conn, GattState.DISCONNECTED);
                CountDownLatch cdl = new CountDownLatch(1);
                conn.runTx(disconnectTx, result -> {
                    logSuccessOrFailure(result, dumpContext, "Successfully Disconnected from " + mac, "Failed Disconnecting from " + mac);
                    cdl.countDown();
                    conn.getDevice().removeDevicePropertiesChangedListener(this);
                });
                cdl.await();
            }
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void gattClientConnect(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            GattConnection conn = clientConnections.get(mac);
            if (conn == null) {
                logError(dumpContext, new IllegalStateException("No connected client for mac " + mac));
            } else {
                conn.registerConnectionEventListener(this);
                conn.getDevice().addDevicePropertiesChangedListener(this);
                GattConnectTransaction connectTx = new GattConnectTransaction(conn, GattState.CONNECTED);
                CountDownLatch cdl = new CountDownLatch(1);
                conn.runTx(connectTx, result -> {
                    logSuccessOrFailure(result, dumpContext, "Successfully Connected to " + mac, "Failed Connecting to " + mac);
                    cdl.countDown();
                    conn.getDevice().removeDevicePropertiesChangedListener(this);
                });
                cdl.await();
            }
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void gattClientDiscoverServices(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            GattConnection conn = clientConnections.get(mac);
            if (conn == null) {
                logError(dumpContext, new IllegalStateException("No connected client for mac " + mac));
            } else {
                conn.getDevice().addDevicePropertiesChangedListener(this);
                GattClientDiscoverServicesTransaction discoverTx = new GattClientDiscoverServicesTransaction(conn, GattState.DISCOVERY_SUCCESS);
                CountDownLatch cdl = new CountDownLatch(1);
                conn.runTx(discoverTx, result -> {
                    logSuccessOrFailure(result, dumpContext, "Successfully Discovered Gatt Client Services", "Failed Discovering Gatt Client Services");
                    cdl.countDown();
                    conn.getDevice().removeDevicePropertiesChangedListener(this);
                });
                cdl.await();
            }
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void stopBackgroundScan(DumperContext dumpContext) {
        fitbitGatt.stopSystemManagedPendingIntentScan();
        log(dumpContext, "Successfully stopped the managed system scans");
    }

    private void findNearbyDevicesBackgroundScan(DumperContext dumpContext) {
        int n = 180;
        StringBuilder builder = new StringBuilder();
        String status = PASS_STATUS;
        String error = "";
        if (!isJsonFormat) {
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
            format(dumpContext, "| %1$32s | %2$32s | %3$32s | %4$32s | %5$32s\n",
                "Name",
                "Address",
                "BtName", "Origin", "RSSI");
        }
        JSONArray jsonArray = new JSONArray();
        Iterable<String> iterable = clientConnections.keySet();
        try {
            for (String mac : iterable) {
                BluetoothDevice device = fitbitGatt.getBluetoothDevice(mac);
                if (device == null) {
                    continue;
                }
                String type;
                switch (device.getType()) {
                    case DEVICE_TYPE_CLASSIC:
                        type = "classic";
                        break;
                    case DEVICE_TYPE_DUAL:
                        type = "dual";
                        break;
                    case DEVICE_TYPE_LE:
                        type = "low energy";
                        break;
                    default:
                        type = "unknown";
                }
                String deviceName = new GattUtils().debugSafeGetBtDeviceName(device);
                String deviceAddress = device.getAddress();
                String origin = clientConnections.get(mac).getDevice().getOrigin().name();
                String rssi = String.valueOf(clientConnections.get(mac).getDevice().getRssi());
                Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                    put("name", deviceName);
                    put("address", deviceAddress);
                    put("btname", type);
                    put("origin", origin);
                    put("rssi", rssi);
                }};
                JSONObject jsonObject = makeJsonObject(map);
                jsonArray.put(jsonObject);
                if (!isJsonFormat) {
                    format(dumpContext, "| %1$32s | %2$32s | %3$32s | %4$32s | %5$32s\n",
                        deviceName, deviceAddress, type, origin, rssi);
                }
            }
        } catch (Exception e) {
            status = FAIL_STATUS;
            error = Arrays.toString(e.getStackTrace());
        }

        if (!isJsonFormat) {
            builder = new StringBuilder();
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
        } else {
            logJsonResult(dumpContext, status, error, jsonArray);
        }
        // look for everything just for the sake of this call
        ScanFilter filter = new ScanFilter.Builder().build();
        ArrayList<ScanFilter> scanFilters = new ArrayList<>(1);
        scanFilters.add(filter);
        boolean didStart = fitbitGatt.startSystemManagedPendingIntentScan(fitbitGatt.getAppContext(), scanFilters);
        if(!didStart) {
            log(dumpContext, "Scanner couldn't be started");
        }
    }

    private void findNearbyDevices(DumperContext dumpContext) {
        int n = 180;
        StringBuilder builder = new StringBuilder();
        String status = PASS_STATUS;
        String error = "";
        if (!isJsonFormat) {
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
            format(dumpContext, "| %1$32s | %2$32s | %3$32s | %4$32s | %5$32s\n",
                "Name",
                "Address",
                "BtName", "Origin", "RSSI");
        }
        JSONArray jsonArray = new JSONArray();
        Iterable<String> iterable = clientConnections.keySet();
        try {
            for (String mac : iterable) {
                BluetoothDevice device = fitbitGatt.getBluetoothDevice(mac);
                if (device == null) {
                    continue;
                }
                String type;
                switch (device.getType()) {
                    case DEVICE_TYPE_CLASSIC:
                        type = "classic";
                        break;
                    case DEVICE_TYPE_DUAL:
                        type = "dual";
                        break;
                    case DEVICE_TYPE_LE:
                        type = "low energy";
                        break;
                    default:
                        type = "unknown";
                }
                String deviceName = new GattUtils().debugSafeGetBtDeviceName(device);
                String deviceAddress = device.getAddress();
                String origin = clientConnections.get(mac).getDevice().getOrigin().name();
                String rssi = String.valueOf(clientConnections.get(mac).getDevice().getRssi());
                Map<String, Object> map = new LinkedHashMap<String, Object>() {{
                    put("name", deviceName);
                    put("address", deviceAddress);
                    put("btname", type);
                    put("origin", origin);
                    put("rssi", rssi);
                }};
                JSONObject jsonObject = makeJsonObject(map);
                jsonArray.put(jsonObject);
                if (!isJsonFormat) {
                    format(dumpContext, "| %1$32s | %2$32s | %3$32s | %4$32s | %5$32s\n",
                        deviceName, deviceAddress, type, origin, rssi);
                }
            }
        } catch (Exception e) {
            status = FAIL_STATUS;
            error = Arrays.toString(e.getStackTrace());
        }

        if (!isJsonFormat) {
            builder = new StringBuilder();
            for (int i = 0; i < n; i++) {
                builder.append("=");
            }
            log(dumpContext, builder.toString());
        } else {
            logJsonResult(dumpContext, status, error, jsonArray);
        }
        fitbitGatt.startHighPriorityScan(fitbitGatt.getAppContext());
    }

    private void closeGattClient(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String mac = args.next();
            GattConnection conn = clientConnections.get(mac);
            if (conn == null) {
                logError(dumpContext, new IllegalStateException("No connected client for mac " + mac));
            } else {
                conn.getDevice().addDevicePropertiesChangedListener(this);
                CloseGattTransaction closeTx = new CloseGattTransaction(conn, GattState.CLOSE_GATT_CLIENT_SUCCESS);
                CountDownLatch cdl = new CountDownLatch(1);
                conn.runTx(closeTx, result -> {
                    logSuccessOrFailure(result, dumpContext, "Successfully Closed Gatt Client Interface", "Failed Closing Gatt Client Interface");
                    cdl.countDown();
                    conn.getDevice().removeDevicePropertiesChangedListener(this);
                });
                cdl.await();
            }
        } else {
            logError(dumpContext, new IllegalArgumentException("No mac address provided"));
        }
    }

    private void clearLocalGattServerServices(DumperContext dumpContext) throws InterruptedException {
        ClearServerServicesTransaction clearServices = new ClearServerServicesTransaction(fitbitGatt.getServer(), GattState.CLEAR_GATT_SERVER_SERVICES_SUCCESS);
        CountDownLatch cdl = new CountDownLatch(1);
        fitbitGatt.getServer().runTx(clearServices, result -> {
            logSuccessOrFailure(result, dumpContext, "Successfully Cleared Gatt Server Service", "Failed Clearing Gatt Server Service");
            cdl.countDown();
        });
        cdl.await();
    }

    private void addLocalGattServerService(DumperContext dumpContext, Iterator<String> args) throws InterruptedException {
        if (args.hasNext()) {
            String serviceUuid = args.next();
            BluetoothGattServer server = fitbitGatt.getServer().getServer();
            boolean isDuplicate = false;
            for (BluetoothGattService service : server.getServices()) {
                String currentUuid = service.getUuid().toString();
                if (currentUuid.equals(serviceUuid)) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                BluetoothGattService gattService = new BluetoothGattService(UUID.fromString(serviceUuid), BluetoothGattService.SERVICE_TYPE_PRIMARY);
                AddGattServerServiceTransaction tx = new AddGattServerServiceTransaction(fitbitGatt.getServer(), GattState.ADD_SERVICE_SUCCESS, gattService);
                CountDownLatch cdl = new CountDownLatch(1);
                fitbitGatt.getServer().runTx(tx, result -> {
                    logSuccessOrFailure(result, dumpContext, "Successfully Added Gatt Server Service", "Failed Adding Gatt Server Service");
                    cdl.countDown();
                });
                cdl.await();
            } else {
                logSuccess(dumpContext, "Duplicate service by UUID ");
            }

        } else {
            logError(dumpContext, new IllegalArgumentException("No viable service UUID provided"));
        }
    }

    private void startGatt(DumperContext dumpContext) {
        String status;
        String error = "";
        try {
            fitbitGatt.startGattClient(context);
            fitbitGatt.startGattServer(context);
            List<ParcelUuid> serviceUuids = new ArrayList<>(1);
            serviceUuids.add(ParcelUuid.fromString("ADABFB00-6E7D-4601-BDA2-BFFAA68956BA"));
            fitbitGatt.setScanServiceUuidFilters(serviceUuids);
            fitbitGatt.startHighPriorityScan(fitbitGatt.getAppContext());
            fitbitGatt.initializeScanner(context);
            status = PASS_STATUS;
        } catch (Exception e) {
            status = FAIL_STATUS;
            error = Arrays.toString(e.getStackTrace());
        }

        logResult("init", status, "Gatt Ready", error, dumpContext);
    }

    private void logResult(String command, String status, String result, String error,
                           DumperContext dumpContext) {
        if (isJsonFormat) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(COMMAND_KEY, command);
            map.put(STATUS_KEY, status);
            if (PASS_STATUS.equalsIgnoreCase(status)) {
                map.put(RESULT_KEY, result);
            } else {
                map.put(ERROR_KEY, error);
            }
            String resultJson = makeJsonObject(map).toString();
            log(dumpContext, resultJson);
        } else {
            log(dumpContext, result);
        }
    }

    private void logJsonResult(DumperContext dumpContext, String status, String error, JSONArray result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, getCommand(dumpContext));
        map.put(STATUS_KEY, status);
        if (PASS_STATUS.equalsIgnoreCase(status)) {
            map.put(RESULT_KEY, result);
        } else {
            map.put(ERROR_KEY, error);
        }
        JSONObject jsonRoot = makeJsonObject(map);
        log(dumpContext, jsonRoot.toString());
    }

    private void logJsonResult(DumperContext dumpContext, TransactionResult result, String error, Map<String, Object> resultMap) {
        boolean success = result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS);
        String status = success ? PASS_STATUS : FAIL_STATUS;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, getCommand(dumpContext));
        map.put(STATUS_KEY, status);
        if (PASS_STATUS.equalsIgnoreCase(status)) {
            map.put(RESULT_KEY, makeJsonObject(resultMap));
        } else {
            map.put(ERROR_KEY, error);
        }
        JSONObject jsonRoot = makeJsonObject(map);
        log(dumpContext, jsonRoot.toString());
    }

    protected JSONObject makeJsonObject(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                jsonObject.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void setJsonFormat(DumperContext dumperContext, Iterator<String> args) {
        String isJsonString = ArgsHelper.nextOptionalArg(args, null);
        if ("on".equalsIgnoreCase(isJsonString)) {
            isJsonFormat = true;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(COMMAND_KEY, "set-json-format");
            map.put(STATUS_KEY, PASS_STATUS);
            map.put(RESULT_KEY, "Set JSON on");
            JSONObject jsonObject = makeJsonObject(map);
            log(dumperContext, jsonObject.toString());
        } else if ("off".equalsIgnoreCase(isJsonString)) {
            isJsonFormat = false;
            log(dumperContext, "Set JSON off");
        } else {
            throw new IllegalArgumentException("usage: dumpapp set-json-format on/off");
        }
    }

    private void printAvailableCommands(DumperContext dumpContext) {
        log(dumpContext, "Available commands:");
        GattCommand[] commands = GattCommand.values();
        for (GattCommand command : commands) {
            dumpContext.getStdout().print(command.getFullName());
            if (command.getShortName() != null && !command.getShortName().equals("")) {
                dumpContext.getStdout().print(", " + command.getShortName());
            }
            dumpContext.getStdout().print(", " + command.getDescription());
            logNewLine(dumpContext);
            logNewLine(dumpContext);
            dumpContext.getStdout().print("-------------------------------");
            logNewLine(dumpContext);
            logNewLine(dumpContext);
        }
    }

    protected void logNewLine(DumperContext dumpContext) {
        dumpContext.getStdout().println();
        dumpContext.getStdout().flush();
    }

    private void logSuccess(DumperContext context, String message) {
        log(context, "SUCCESS :" + message);
    }

    private void logFailure(DumperContext context, String message) {
        log(context, "FAILURE :" + message);
    }

    private void logSuccessOrFailure(TransactionResult result, DumperContext dumpContext, String successStr, String failureStr) {
        boolean success = result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS);
        if (isJsonFormat) {
            logResult(getCommand(dumpContext), success ? PASS_STATUS : FAIL_STATUS, result.toString(), result.toString(), dumpContext);
        } else {
            log(dumpContext, result.toString());
            if (success) {
                logSuccess(dumpContext, successStr);
            } else {
                logFailure(dumpContext, failureStr);
            }
        }
    }

    private void asyncLog(String message) {
        PrintStream out = null;
        try {
            out = new PrintStream(System.out, true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        out.println(message);
    }

    protected void log(DumperContext context, String message) {
        context.getStdout().println(message);
        context.getStdout().flush();
    }

    private String getCommand(DumperContext dumpContext) {
        List<String> argsList = dumpContext.getArgsAsList();
        Iterator<String> args = argsList.iterator();
        return ArgsHelper.nextOptionalArg(args, null);
    }

    protected void logError(DumperContext dumpContext, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        String error = "\u001B[31m " + exceptionAsString + " \u001B[0m";
        if (isJsonFormat) {
            logResult(getCommand(dumpContext), FAIL_STATUS, "", error, dumpContext);
        } else {
            dumpContext.getStderr().println(error);
            dumpContext.getStderr().flush();
        }
    }

    private void format(DumperContext dumpContext, String format, Object... args) {
        dumpContext.getStdout().format(format, args);
        dumpContext.getStdout().flush();
    }

    @Override
    public void onBluetoothPeripheralDiscovered(@NonNull GattConnection connection) {
        this.clientConnections.put(connection.getDevice().getAddress(), connection);
        onBluetoothPeripheralDevicePropertiesChanged(connection.getDevice());
        if (FitbitGatt.getInstance().isSlowLoggingEnabled()) {
            Timber.d("Discovered device %s", connection.getDevice());
        }
    }

    @Override
    public void onBluetoothPeripheralDisconnected(GattConnection connection) {
        if (connection != null) {
            this.clientConnections.remove(connection.getDevice().getAddress());
        }
    }

    @Override
    public void onScanStarted() {
        Timber.v("Scan was started");
    }

    @Override
    public void onScanStopped() {
        Timber.v("Scan was stopped");
    }

    @Override
    public void onScannerInitError(BitGattStartException error) {
        //no-op
    }

    @Override
    public void onPendingIntentScanStopped() {
        Timber.v("Pending intent scan stopped");
    }

    @Override
    public void onPendingIntentScanStarted() {
        Timber.v("Pending intent scan started");
    }

    @Override
    public void onBluetoothOff() {
        Timber.v("Bluetooth was turned off");
    }

    @Override
    public void onBluetoothOn() {
        Timber.v("Bluetooth was turned on");
    }

    @Override
    public void onBluetoothTurningOn() {
        Timber.v("Bluetooth turning on was called");
    }

    @Override
    public void onBluetoothTurningOff() {
        Timber.v("Bluetooth turning off was called");
    }

    @Override
    public void onGattServerStarted(GattServerConnection serverConnection) {
        serverConnection.registerConnectionEventListener(serverConnectionListener);
    }

    @Override
    public void onGattServerStartError(BitGattStartException error) {
        //no-op
    }

    @Override
    public void onGattClientStarted() {
        //no-op
    }

    @Override
    public void onGattClientStartError(BitGattStartException error) {

    }

    @Override
    public void onBluetoothPeripheralDevicePropertiesChanged(FitbitBluetoothDevice device) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mac", device.getAddress());
        map.put("name", device.getName());
        map.put("origin", device.getOrigin());
        map.put("rssi", device.getRssi());
        logJson(map, GATT_CLIENT_PERIPHERAL_DEVICE_PROPERTIES_CHANGED);
    }

    @Override
    public void onClientCharacteristicChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {
        Timber.v("[%s] The characteristic changed : %s", connection.getDevice(), result);
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> transactionResult = new LinkedHashMap<>();
        transactionResult.put("responseStatus", result.getResponseCodeString());
        transactionResult.put("requestId", result.getRequestId());
        transactionResult.put("offset", result.getOffset());
        transactionResult.put("serviceUuid", result.getServiceUuid());
        transactionResult.put("characteristicUuid", result.getCharacteristicUuid());
        transactionResult.put("descriptorUuid", result.getDescriptorUuid());
        transactionResult.put("resultState", result.getResultStatus().toString());
        transactionResult.put("data", Bytes.byteArrayToHexString(result.getData()));
        transactionResult.put("rssi", result.getRssi());
        transactionResult.put("mtu", result.getMtu());
        transactionResult.put("transactionName", result.getTransactionName());
        map.put("mac", connection.getDevice().getAddress());
        map.put("name", connection.getDevice().getName());
        map.put("origin", connection.getDevice().getOrigin());
        map.put("rssi", connection.getDevice().getRssi());
        map.put("transactionResult", transactionResult);
        logJson(map, GATT_CLIENT_CHARACTERISTIC_CHANGED);
    }

    @Override
    public void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {
        Timber.v("[%s] The connection state changed : %s", connection.getDevice(), result);
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> transactionResult = new LinkedHashMap<>();
        transactionResult.put("responseStatus", result.getResponseCodeString());
        transactionResult.put("requestId", result.getRequestId());
        transactionResult.put("offset", result.getOffset());
        transactionResult.put("serviceUuid", result.getServiceUuid());
        transactionResult.put("characteristicUuid", result.getCharacteristicUuid());
        transactionResult.put("descriptorUuid", result.getDescriptorUuid());
        transactionResult.put("resultState", result.getResultStatus().toString());
        transactionResult.put("data", Bytes.byteArrayToHexString(result.getData()));
        transactionResult.put("rssi", result.getRssi());
        transactionResult.put("mtu", result.getMtu());
        transactionResult.put("transactionName", result.getTransactionName());
        map.put("mac", connection.getDevice().getAddress());
        map.put("name", connection.getDevice().getName());
        map.put("origin", connection.getDevice().getOrigin());
        map.put("rssi", connection.getDevice().getRssi());
        map.put("transactionResult", transactionResult);
        logJson(map, GATT_CLIENT_CONNECTION_STATE_CHANGED);
    }

    @Override
    public void onServicesDiscovered(@NonNull TransactionResult result, @NonNull GattConnection connection) {
        Timber.v("[%s] The connection services were discovered: %s", connection.getDevice(), result);
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> transactionResult = new LinkedHashMap<>();
        transactionResult.put("responseStatus", result.getResponseCodeString());
        transactionResult.put("requestId", result.getRequestId());
        transactionResult.put("offset", result.getOffset());
        transactionResult.put("serviceUuid", result.getServiceUuid());
        transactionResult.put("characteristicUuid", result.getCharacteristicUuid());
        transactionResult.put("descriptorUuid", result.getDescriptorUuid());
        transactionResult.put("resultState", result.getResultStatus().toString());
        transactionResult.put("data", Bytes.byteArrayToHexString(result.getData()));
        transactionResult.put("rssi", result.getRssi());
        transactionResult.put("mtu", result.getMtu());
        transactionResult.put("transactionName", result.getTransactionName());
        map.put("mac", connection.getDevice().getAddress());
        map.put("name", connection.getDevice().getName());
        map.put("origin", connection.getDevice().getOrigin());
        map.put("rssi", connection.getDevice().getRssi());
        map.put("transactionResult", transactionResult);
        logJson(map, GATT_CLIENT_DISCOVERED_SERVICES);
    }

    @Override
    public void onMtuChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {
        Timber.v("[%s] The connection mtu was changed: %s", connection.getDevice(), result);
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> transactionResult = new LinkedHashMap<>();
        transactionResult.put("responseStatus", result.getResponseCodeString());
        transactionResult.put("requestId", result.getRequestId());
        transactionResult.put("offset", result.getOffset());
        transactionResult.put("serviceUuid", result.getServiceUuid());
        transactionResult.put("characteristicUuid", result.getCharacteristicUuid());
        transactionResult.put("descriptorUuid", result.getDescriptorUuid());
        transactionResult.put("resultState", result.getResultStatus().toString());
        transactionResult.put("data", Bytes.byteArrayToHexString(result.getData()));
        transactionResult.put("rssi", result.getRssi());
        transactionResult.put("mtu", result.getMtu());
        transactionResult.put("transactionName", result.getTransactionName());
        map.put("mac", connection.getDevice().getAddress());
        map.put("name", connection.getDevice().getName());
        map.put("origin", connection.getDevice().getOrigin());
        map.put("rssi", connection.getDevice().getRssi());
        map.put("transactionResult", transactionResult);
        logJson(map, GATT_CLIENT_CHANGED_MTU);
    }

    @Override
    public void onPhyChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {
        Timber.v("[%s] The connection phy was changed: %s", connection.getDevice(), result);
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> transactionResult = new LinkedHashMap<>();
        transactionResult.put("responseStatus", result.getResponseCodeString());
        transactionResult.put("requestId", result.getRequestId());
        transactionResult.put("offset", result.getOffset());
        transactionResult.put("serviceUuid", result.getServiceUuid());
        transactionResult.put("characteristicUuid", result.getCharacteristicUuid());
        transactionResult.put("descriptorUuid", result.getDescriptorUuid());
        transactionResult.put("resultState", result.getResultStatus().toString());
        transactionResult.put("data", Bytes.byteArrayToHexString(result.getData()));
        transactionResult.put("rssi", result.getRssi());
        transactionResult.put("mtu", result.getMtu());
        transactionResult.put("txPhy", result.getTxPhy());
        transactionResult.put("rxPhy", result.getRxPhy());
        transactionResult.put("transactionName", result.getTransactionName());
        map.put("mac", connection.getDevice().getAddress());
        map.put("name", connection.getDevice().getName());
        map.put("origin", connection.getDevice().getOrigin());
        map.put("rssi", connection.getDevice().getRssi());
        map.put("transactionResult", transactionResult);
        logJson(map, GATT_CLIENT_CHANGED_PHY);
    }

    private void logJson(Map<String, Object> map, String key) {
        map.put(COMMAND_KEY, key);
        JSONObject jsonRoot = makeJsonObject(map);
        if (dumperContext != null) {
            asyncLog(jsonRoot.toString());
        }
    }

    class ServerConnectionListener implements ServerConnectionEventListener {
        /**
         * private static final String GATT_SERVER_CHARACTERISTIC_READ_REQUEST_VALUE = "server_characteristic_read_request";
         * private static final String GATT_SERVER_CHARACTERISTIC_WRITE_REQUEST_VALUE = "server_characteristic_write_request";
         * private static final String GATT_SERVER_DESCRIPTOR_READ_REQUEST_VALUE = "server_descriptor_read_request";
         * private static final String GATT_SERVER_DESCRIPTOR_WRITE_REQUEST_VALUE = "server_descriptor_write_request";
         * private static final String GATT_SERVER_CONNECTION_STATE_CHANGE_VALUE = "server_connection_state_change";
         * private static final String GATT_SERVER_MTU_CHANGE_VALUE = "server_mtu_change_value";
         */
        private DumperContext context;

        public void setContext(DumperContext context) {
            this.context = context;
        }

        private void logJson(Map<String, Object> map, String key) {
            map.put(COMMAND_KEY, key);
            JSONObject jsonRoot = makeJsonObject(map);
            asyncLog(jsonRoot.toString());
        }

        @Override
        public void onServerMtuChanged(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("new_mtu_value", result.getMtu());
            logJson(map, GATT_SERVER_MTU_CHANGE_VALUE);
        }

        @Override
        public void onServerConnectionStateChanged(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("server_is_connected", connection.isConnected());
            logJson(map, GATT_SERVER_CONNECTION_STATE_CHANGE_VALUE);
        }

        @Override
        public void onServerCharacteristicWriteRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("service_uuid", result.getServiceUuid());
            map.put("characterstic_uuid", result.getCharacteristicUuid());
            map.put("offset", result.getOffset());
            map.put("requestId", result.getRequestId());
            map.put("data", Bytes.byteArrayToHexString(result.getData()));
            logJson(map, GATT_SERVER_CHARACTERISTIC_WRITE_REQUEST_VALUE);
        }

        @Override
        public void onServerCharacteristicReadRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("service_uuid", result.getServiceUuid());
            map.put("characterstic_uuid", result.getCharacteristicUuid());
            map.put("offset", result.getOffset());
            map.put("requestId", result.getRequestId());
            map.put("data", Bytes.byteArrayToHexString(result.getData()));
            logJson(map, GATT_SERVER_CHARACTERISTIC_READ_REQUEST_VALUE);
        }

        @Override
        public void onServerDescriptorWriteRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("service_uuid", result.getServiceUuid());
            map.put("characterstic_uuid", result.getCharacteristicUuid());
            map.put("descriptor_uuid", result.getDescriptorUuid());
            map.put("offset", result.getOffset());
            map.put("requestId", result.getRequestId());
            map.put("data", Bytes.byteArrayToHexString(result.getData()));
            logJson(map, GATT_SERVER_DESCRIPTOR_WRITE_REQUEST_VALUE);
        }

        @Override
        public void onServerDescriptorReadRequest(@NonNull BluetoothDevice device, @NonNull TransactionResult result, @NonNull GattServerConnection connection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("service_uuid", result.getServiceUuid());
            map.put("characterstic_uuid", result.getCharacteristicUuid());
            map.put("descriptor_uuid", result.getDescriptorUuid());
            map.put("offset", result.getOffset());
            map.put("requestId", result.getRequestId());
            map.put("data", Bytes.byteArrayToHexString(result.getData()));
            logJson(map, GATT_SERVER_DESCRIPTOR_READ_REQUEST_VALUE);
        }
    }
}
