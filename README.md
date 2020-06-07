# Fitbit Gatt (Bitgatt) Documentation     [![Build Status](https://travis-ci.com/Fitbit/bitgatt.svg?branch=master)](https://travis-ci.com/Fitbit/bitgatt)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FFitbit%2Fbitgatt.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2FFitbit%2Fbitgatt?ref=badge_shield) [![](https://jitpack.io/v/Fitbit/bitgatt.svg)](https://jitpack.io/#Fitbit/bitgatt)

## Table of Contents

1. [Setting up the dependency](#setting-up-the-dependency)
1. [Original Contributors](#original-contributors)
1. [Purpose](#purpose)
1. [Threading](#threading)
1. [Diagram](#diagram)
1. [Architectural Overview](#architectural-overview)
1. [Transactions](#transactions)
1. [Validation](#validation)
1. [Pre/Post Commit (Deprecated)](#pre-post-commit-deprecated)
1. [Composite Transactions](#composite-transactions)
1. [Gatt Server](#gatt-server)
1. [Sample Code](#sample-code)
1. [Bitgatt Scanner](#bitgatt-scanner)
1. [Always Connected Scanner](#always-connected-scanner)
1. [Bluetooth State on Android Device](#bluetooth-state-on-android-device)
1. [Runtime Mocking](#runtime-mocking)
1. [Bitgatt Transaction Manual](#bitgatt-transaction-manual)
1. [License](#license)

## Purpose

The FitbitGatt API is designed to provide a strong state machine around
all Android gatt operations with the aim to make Android BLE development
as bomb-proof as possible.

We created a blog post to explain further why we need such a significant
abstraction on top of the Android Low Energy API : [eng.fitbit.com/what-is-bitgatt-and-why-do-we-need-it/](https://eng.fitbit.com/what-is-bitgatt-and-why-do-we-need-it/)

### Setting up the dependency

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

```gradle
dependencies {
    implementation 'com.github.Fitbit:bitgatt:TAG'
}
```

Where `TAG` can be any of the following
- a release tag such as `v0.9.1`
- a snapshot from master `master-SNAPSHOT`
- a specifici commit `53ebed0415`



### Original Contributors

#### Fitbit Maintainers
Irvin Owens Jr <iowens@fitbit.com>, <0xbadbeef@sigsegv.us> - Creator

Ionut Lepadatescu - <ilepadatescu@fitbit.com> - Maintainer


#### Fitbit Contributors
Andy Branscomb <abranscomb@fitbit.com>

Adriana Draghici - <adraghici@fitbit.com>

Cristian Ichimescu - <cichimescu@fitbit.com>

Murtuza Khan - <murtuza.kahn@fitbit.com>

Ionut Lepadatescu - <ilepadatescu@fitbit.com>

Irvin Owens Jr - <iowens@fitbit.com>




### Getting Started

To use Bitgatt, you will need to add the permission for bluetooth ( and maybe admin ) into your
manifest.  

To allow flexibility each component can be started individually

#### Starting the gatt server 

The gatt server allows your application to host it's own gatt server stack that can be accessed by external bluetooth devices.

You can started in 2 ways:

```java
FitbitGatt.startGattServer(@NonNull Context context) 
```

or 

```java
FitbitGatt.startGattServerWithServices(@NonNull Context context, @Nullable List<BluetoothGattService> services)
```

In both cases the server is started asynchronously and once finished will call all `FitbitGattCallback` registered listeners on `onGattServerStarted(GattServerConnection serverConnection)` or ` onGattServerStartError(BitGattStartException error)` depending on the success state

In component is already started when toggling bluetooth it will try automatically starting the gatt server again. It will call the same apis on success/failure

####  Starting the FitbitGatt scanner 

The ble scanner from FitbitGatt allows discovery of any ble devices based on provided filters.

The scanner component can be started in 2 ways:

```java
FitbitGatt.initializeScanner(@NonNull Context context)
```  

This call is synchronous. If there is an error while trying to initialize it will call `FitbitGattCallback.onScannerInitError(BitGattStartException error)` for the registered listeners.
This method will not start any active scan. It will just initialize the scanner and allowing you to set filters and then start a scan type.

or  

```java
FitbitGatt.startPeriodicalScannerWithFilters(@NonNull Context context, List<ScanFilter> filters)
```  

This second method besides initializing the scanner component it will also setup a periodical scan with the given filters.
If the filter list is empty or the gatt scanner is already it will error out.
If there is an error while trying to initialize it will call `FitbitGattCallback.onScannerInitError(BitGattStartException error)` for the registered listeners.


####  Starting the bitgatt client

If your connection is already present you may wish to start only the gatt client. This can be done by calling `FitbitGatt.startGattClient(@NonNull Context context)`.

This will call `FitbitGattCallback.onGattClientStarted()` or `FitbitGattCallback.onGattClientStartError(BitGattStartException error)` if an error occurs.

If your app is only the client in BLE connection you will use only this component and the scanner to more reliably fetch the device.
 
## Threading

The threading model of the Android gatt is extremely fraught with landmines.
  It was very easy to end up processing data on one of the JNI binder
  threads when a gatt callback returned, or in our legacy implementation
  it was so limited as we had only a single execution thread that required
  pre-emption.
  
  Bitgatt does this differently, instead of providing callbacks on the JNI binder
  thread, Bitgatt intentionally delivers results on the main thread while dispatching
  transactions on a connection thread held by the client or server connection object
  in this way dispatches are not blocked by receipts, and any I/O on the main thread
  issues are caught right away (because it's I/O on the main thread).  This is the same
  pattern as an Android broadcast receiver so it is wise to get off of the main thread as
  soon as possible to create a responsive system.
  
  Responses will be copies of characteristics, descriptors, and services.  These copies are not
  able to be handed back to the system as instantiating new characteristics, descriptors and
  services, then utilizing them with GATT instances will often wedge the stack.  The reason
  for this is that native GATT objects have an instance id inside of them.  When you create a new
  instance, it did not originate from the GATT DB so the instance is null.  By forcing the use of
  copies, bitgatt eliminates the possibility of this error from the developer.

## Diagram 

<pre>

             ╔════════════════════════════════╗                             ┌──────────────────────────────────────────┐
             ║ FitbitGatt hosts instances of  ║                             │FitbitGatt                                │
             ║the GattConnection, they do not ║                             │Looper for asynchronous operations        │
             ║ interfere with each other and  ║                             │characteristic notifications and such     │
             ║ transactions execute serially  ║                             │                                          │
             ║      per connection, but       ║ ┌┬┬─────────────────────────│                                          │──────────┐
             ║   asynchronously with other    ║ │││                         │                                          │          │
             ║          connections           ║ │││                         │                                          │          │
             ╚════════════════════════════════╝ │││                         │                                          │          │
                                                │││                         │                                          │          │
                                                │││                         └──────────────────────────────────────────┘          │
                                                │││                                               ▲                               │
                                                ││▼                                               │                               │
                               ┌────────────────┼▼───────────────────┐                            │                               │
                              ┌┴────────────────▼───────────────────┐│                            │                               ▼
                             ┌┴────────────────────────────────────┐││                            │           ┌───────────────────────────────────────┐
                             │GattConnection                       │││                            │           │GattServerConnection                   │
                             │Looper per connection object         │││                            │           │Looper for the gatt server             │
                             │Looper for gatt transaction timeout  │││                            │           │                                       │
                             │                                     │││                            │           │                                       │
                             │                                     │││                            │           │                                       │
                             │                                     │││                            │           │                                       │
                             │                                     │││                            │           │                                       │
                             │                                     │├┘                            │           │                                       │
                             │                                     ├┘                             │           │                                       │
                             └─────────────────────────────────────┘                              │           └───────────────────────────────────────┘
                                                                                                  │
                                                                                                  │
                                                                                                  │
                             ┌─────────────────────────────────────┐                              │
                             │TrackerScanner                       │                              │
                             │ Main Looper                         │                              │
                             │                                     │                              │
                             │                                     │      Pushes candidate        │
                             │                                     │────────devices into ─────────┘
                             │                                     │         FitbitGatt
                             │                                     │
                             │                                     │
                             └─────────────────────────────────────┘

                              ╔════════════════════════════════╗
                              ║ The tracker scanner will find  ║
                              ║devices that match a filter and ║
                              ║will add them into the cache in ║
                              ║  the gatt.  They will only be  ║
                              ║connected if the business logic ║
                              ║            dictates            ║
                              ║                                ║
                              ╚════════════════════════════════╝

Created with Monodraw                                                                                                                                  </pre>

### Architectural Overview

The general idea behind this is that we want to use the right number of threads.
This means that we will keep the serial execution nature of the legacy implementation
at the GATT level, but allow the business logic to execute gatt commands in any way
desired.  This has the effect of matching the best of concurrency with the stability
of single-threadedness.  In short it's an effort to limit the concurrency only where
we actually have to, but allowing concurrency anywhere else as the business logic requires.

Where necessary bitgatt provides primitives for deploying strategies around hooks to deal with
specific OEM incompatibilities or bad behavior.  The intent behind this is to keep the bitgatt
core code free of "hacks" around bugs in the Android BLE code, or peripheral issues.

*NOTE* The present API is not stable as the library is still at 0.8.x.  Improvements in the API
may still occur, especially around the scanner.

## Transactions

Gatt transactions are single gatt operations such as enable characteristic notifications
on x characteristic, or write to characteristic, etc... transactions can
only be executed if the prior transaction has not ended in failure with notable
exceptions, those exceptions will be enumerated in the manual section.  This will
force a developer to deal with errors explicitly and will not try to hide or to automatically
deal with errors by disconnecting and reconnecting, etc ...

Everything about the execution of gatt operations should be intentional to prevent
side effects that adjust the operation of higher-level functions, as an example
if you have implicit connection attempts, then you have to deal with the situation
where two different higher level operations could start a connection
attempt.  If you make connecting explicit on a single-threaded implementation,
 then this is by nature impossible, and the errors caused by it are eliminated.
 
Transactions are atomic.  A subsequent transaction provided to the connection transaction queue
will not be executed until the prior running transaction completes in success, failure, or timeout.

### Transaction Strategies

The strategies are meant to be used by bitgatt for dealing with the odd phone, tablet, or chromebook
that does not seem to want to play nice with standard GATT operations.  In this case a strategy
should be hooked directly into a transaction wherever it makes sense to mitigate the adverse
behavior against the GATT ( usually in these scenarios a bug should be filed against the OEM ).

The StrategyProvider, Situation, and Strategy classes are all public so that you, as a GATT user
can extend these and implement your own strategies at the business logic level.  This would be, for
example, if you are a mobile developer and a particular firmware on your IOT device does a strange
thing when writing to a descriptor for the first time, but in later versions of that firmware this
is fixed.

You would in this case extend strategy and StrategyProvider, overriding getStrategyForPhoneAndGattConnection(...)
to return your own strategies.  You could determine the firmware version and after that strange thing,
 you could retry, whatever your business logic required.  This you could do without modifying bitgatt,
 and without adversely affecting your basic GATT logic. 

## Validation

The transaction entry states are guarded by a transaction validator
that verifies that the gatt is in a good state and is ready to
be used by a client.  If it isn't it will return a clear transaction result
error that will allow the developer to understand why it isn't working.

## Pre/Post Commit Deprecated

In order to chain transactions we provide a pre/post commit implemention
that will allow you to provide a bundle of transactions that must
execute in order.  Pre-commit transactions will execute before the main body
of the transaction while post-commit transactions will run after

## Composite Transactions

The pre / post transaction API was deprecated in favor of a single composite transaction that takes
as an argument a list of transactions.  These transactions will be executed atomically in the order
in which they are present in the list.  Any failure will halt the chain of transactions and exit
the composite transaction.

## Gatt Server 

The gatt server implementation here is designed to protect the developer from common Android mistakes
such as not responding to write requests or read requests on characteristics or descriptors that
are not implemented.  Bitgatt will respond with error ( to help prevent disconnections ), in cases
where the developer has not registered a listener, or where they are not using a particular
characteristic.

In order to ensure that the gatt server is always responsive after toggling bluetooth services will
be cleared when BT is disabled, this leads to a consistent experience across Android devices.  In
order to use services again, please re-add any gatt server services that you are hosting on the
Android device when BT is turned on again.  You can do this by listening for bt on / off events 
with the FitbitGattCallback.

## Sample Code

Pre-Commit ( Deprecated )
```java
class Test {
    public void doTx(){
        WriteGattDescriptorMockTransaction writeGattDescriptorMockTransaction = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        writeGattCharacteristicMockTransaction.addPreCommitHook(writeGattDescriptorMockTransaction);
        conn.runTx(writeGattCharacteristicMockTransaction, result -> {
            Timber.v("Result provided %s", result);
        });
    }
}

```
Post-Commit ( Deprecated )
```java
class Test {
    public void doTx(){
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        SubscribeToCharacteristicNotificationsMockTransaction subscribe = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        WriteGattDescriptorMockTransaction writeDescriptor = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        writeGattCharacteristicMockTransaction.addPostCommitHook(subscribe);
        writeGattCharacteristicMockTransaction.addPostCommitHook(writeDescriptor);
        conn.runTx(writeGattCharacteristicMockTransaction, result -> {
            Timber.v("Result provided %s", result);
        });
    }
}
```
Composite Transaction
```java
class Test {
    public void doTx(){
        WriteGattCharacteristicMockTransaction writeGattCharacteristicMockTransaction = new WriteGattCharacteristicMockTransaction(conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, characteristic, fakeData, false);
        SubscribeToCharacteristicNotificationsMockTransaction subscribe = new SubscribeToCharacteristicNotificationsMockTransaction(conn, GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS, characteristic, fakeData, false);
        WriteGattDescriptorMockTransaction writeDescriptor = new WriteGattDescriptorMockTransaction(conn, GattState.WRITE_DESCRIPTOR_SUCCESS, descriptor, fakeData, false);
        ArrayList<GattTransaction> transactions = new ArrayList<>();
        transactions.add(writeGattCharacteristicMockTransaction);
        transactions.add(subscribe);
        transactions.add(writeDescriptor);
        CompositeClientTransaction composite = new CompositeClientTransaction(conn, transactions);
        conn.runTx(composite, result -> {
            Timber.v("Result provided %s", result);        
        });
    }
}
```
Chained connect and discover services
```java
class Test {
    public void doTx(){
        // obtain the connection
        GattConnection conn = FitbitGatt.getInstance().getConnection(myBluetoothDevice);
        GattConnectTransaction connTx = new GattConnectTransaction(conn, GattState.CONNECTED);
        conn.runTx(connTx, (result) -> {
            if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                GattClientDiscoverServicesTransaction discoverTx = new GattClientDiscoverServicesTransaction(conn, GattState.DISCOVERY_SUCCESS);
                conn.runTx(discoverTx, (result1) -> {
                    if (result1.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        // yay, connection is ready to use
                    } else {
                        Log.d("test", "something bad happened during discovery %s", result1);
                    }
                });
            } else {
                Log.d("test", "Failed to connect successfully %s", result); // will print out all details
            }
        });
    }
}
```
Scanning (periodical scan) ... remember the idea behind the scanner is that it should be treated
as a system resource, there should be a single periodical scan, and / or intent scan that occurs
with multiple filters.  There can be multiple listeners to scan results.
```java
class Test {
    public void doScan(){
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.initializeScanner(this); // start is idempotent
        gatt.registerGattEventListener(mylistener);  // also idempotent for adding instances
        gatt.addScanServiceUUIDWithMaskFilter(ParcelUuid.fromString("ABCDEFGH-6E7D-4601-BDA2-BFFAA68956BA"), null);
        boolean success = gatt.startPeriodicScan(this);
        if(!success) {
            Timber.v("The scan didn't start, oh noes!!!!");
        }
    }
}
```
Scanning (high priority scan) ... will stop a scan if in progress and deliver the onScanStopped
callback
```java
class Test {
    public void doScan(){
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.initializeScanner(this); // start is idempotent
        gatt.registerGattEventListener(mylistener); // also idempotent for adding instances
        gatt.addScanServiceUUIDWithMaskFilter(ParcelUuid.fromString("ABCDEFGH-6E7D-4601-BDA2-BFFAA68956BA"), null);
        boolean success = gatt.startHighPriorityScan(this);
        if(!success) {
            Timber.v("The scan didn't start, oh noes!!!!");
        }
    }
}
```
Scanning (pending intent scan) ... will deliver callbacks for devices discovered by the system scan
the backoff, and scan intervals are managed by the Android system
```java
class Test {
    public void doScan(){
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.initializeScanner(this); // start is idempotent
        gatt.registerGattEventListener(mylistener); // also idempotent for adding instances
        gatt.addScanServiceUUIDWithMaskFilter(ParcelUuid.fromString("ABCDEFGH-6E7D-4601-BDA2-BFFAA68956BA"), null);
        ArrayList<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setDeviceName("Flex").build());
        boolean success = gatt.startSystemManagedPendingIntentScan(this, scanFilters);
        if(!success) {
            Timber.v("The scan didn't start, oh noes!!!!");
        }
    }
}
```

## Bitgatt Scanner

The Bitgatt scanner is designed around the principle that the developer should have a particular
set of filters that they want to find and always want to know about them.  The FitbitBluetoothDevice
object will keep the scan result with it so that it can be used even if the device remains connected.

The scanner will call a callback if the data changes on a subsequent scan, and will callback when
started or stopped.  There is a pending intent scan available on Oreo and higher that may be used
in addition to the low-duty periodical scanner, or with the high-duty scanner.

If you are going to use the pending intent scanner, it is important to ensure that you cancel the scan
as soon as you can as it consumes an additional gatt_if.  These interfaces are limited in nature and if your application
is using more than one of them you could inadvertently cause bluetooth to stop working properly on 
your users' phone.  Bitgatt will prevent you from using more than one additional if, however it would be better
if you only used a single gatt_if.

If you do not know what a gatt_if is, it is advisable to use the periodical scanner instead.

## [Always Connected Scanner](#always-connected-scanner)

Scanning on Android is quite complex, in many cases however the developer has a peripheral which
they want to remain connected whenever the mobile device is within range.  This could be accomplished
naiively either by starting a low/high-latency scan for a given duration by setting a cancel scan
call as a pending message via the many future wrappers available to modern Android developers.

There are dozens of hidden complexities within this.  What if the user turns BT on / off during
this time, how do you ensure that your scan state matches? What if the Android power manager decides
that your application is now scanning too much and you end up with silent scan-start failures?

To make this easier, bitgatt features a simple always connected scanner that will attempt to protect
the developer from the various problems with Android scanning as well as making it straightforward
to always connect when within range.  To prevent obvious problems, ad-hoc scanning using the bitgatt
peripheral scanner API is prevented while the always connected scanner is in use.  It is expected
that one always connected scanner will be enabled per application.

It is critical to remember that not all OEMs on all Android versions implement all features of
the filter API, you could set a MAC address filter that is ineffective on the HTC M8 running 5.0.2
for example.  Make sure to test thoroughly if you are concerned with Android versions before 9.

### Usage
Simplest case, no scanning in effect and, we want to stay connected all peripherals with a given name,
 also that we do not want to keep scanning after we have found any device that matches the name filter
 
### Find one device matching a name filter and keep it connected

```java
class Test {
    
    AlwaysConnectedScanner alwaysConnectedScanner = FitbitGatt.getInstance().getAlwaysConnectedScanner();
    
    public void startAlwaysConnectedScanner(){
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.initializeScanner(this); // start is idempotent
        ScanFilter filter = new ScanFilter.Builder().setDeviceName("MyCoolIOTThing").build();
        // the always connected scanner will default to discovering 1 device matching the filter and that
        // once it finds a single match it should stop scanning until a device disconnects
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        boolean didStart = alwaysConnectedScanner.start(mockContext);
        if(!didStart) {
            android.util.Log.DEBUG("There was a problem starting the scanner!");
            return;
        }
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(this);
    }
    
    public void stopAlwaysConnectedScanner(Context context){
        alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(this);
        alwaysConnectedScanner.stop(context);
    }
}
```

Slightly more complex case, find one device, but keep looking even after it is connected

### Find one device, but keep scanning even after it is connected

```java
class Test {
    
    AlwaysConnectedScanner alwaysConnectedScanner = FitbitGatt.getInstance().getAlwaysConnectedScanner();
    
    public void startAlwaysConnectedScanner(){
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.initializeScanner(this); // start is idempotent
        ScanFilter filter = new ScanFilter.Builder().setDeviceName("MyCoolIOTThing").build();
        // the always connected scanner will default to discovering 1 device matching the filter and that
        // once it finds a single match it should stop scanning until a device disconnects
        alwaysConnectedScanner.setNumberOfExpectedDevices(1);
        // if expected devices is zero, then the value of should keep looking is not relevant
        // the scanner will just keep going
        alwaysConnectedScanner.setShouldKeepLooking(true);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        boolean didStart = alwaysConnectedScanner.start(mockContext);
        if(!didStart) {
            android.util.Log.DEBUG("There was a problem starting the scanner!");
            return;
        }
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(this);
    }
    
    public void stopAlwaysConnectedScanner(Context context){
        alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(this);
        alwaysConnectedScanner.stop(context);
    }
}
```

Filter more complex with a SRV data mask maybe matching some kind of encrypted user id for several
devices, should stop when all devices are found

### Find a few devices via some sort of SRV data

```java
class Test {
    
    AlwaysConnectedScanner alwaysConnectedScanner = FitbitGatt.getInstance().getAlwaysConnectedScanner();
    
    public void startAlwaysConnectedScanner(){
        FitbitGatt gatt = FitbitGatt.getInstance();
        gatt.initializeScanner(this); // start is idempotent
        // device srvdata
        ParcelUuid srvUuid = new ParcelUuid(UUID.fromString("620CC755-613A-430C-BA60-17258CD6B078"));
        // device one service data
        byte[] serviceDataDeviceOne = new byte[]{ 0x00, 0x18, 0x1A, 0x00, 0x00, 0x00};
        byte[] serviceDataDeviceTwo = new byte[]{0x00, 0x1D, 0xBB, 0x00, 0x00, 0x00};
        byte[] serviceDataDeviceThree = new byte[]{0x00, 0x01, 0x02, 0x00, 0x00, 0x00};
        byte[] userIdServiceDataMask = new byte[] {0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00};
        ScanFilter filterOne = new ScanFilter.Builder()
        .setServiceData(srvUuid, serviceDataDeviceOne, userIdServiceDataMask)
        .build();
        ScanFilter filterTwo = new ScanFilter.Builder()
        .setServiceData(srvUuid, serviceDataDeviceTwo, userIdServiceDataMask)
        .build();
        ScanFilter filterThree = new ScanFilter.Builder()
        .setServiceData(srvUuid, serviceDataDeviceThree, userIdServiceDataMask)
        .build();
        ArrayList<ScanFilter> filters = new ArrayList<>(3);
        filters.add(filterOne);
        filters.add(filterTwo);
        filters.add(filterThree);
        // will remove all filters currently being used and replace them with the given
        // filters, will take effect on the next scan
        alwaysConnectedScanner.setScanFilters(filters);
        // the always connected scanner will default to discovering 1 device matching the filter and that
        // once it finds a single match it should stop scanning until a device disconnects
        alwaysConnectedScanner.setNumberOfExpectedDevices(3);
        // will only take effect when the next device disconnects or connects
        alwaysConnectedScanner.setShouldKeepLooking(false);
        alwaysConnectedScanner.addScanFilter(mockContext, filter);
        boolean didStart = alwaysConnectedScanner.start(mockContext);
        if(!didStart) {
            android.util.Log.DEBUG("There was a problem starting the scanner!");
            return;
        }
        alwaysConnectedScanner.registerAlwaysConnectedScannerListener(this);
    }
    
    public void stopAlwaysConnectedScanner(Context context){
        alwaysConnectedScanner.unregisterAlwaysConnectedScannerListener(this);
        alwaysConnectedScanner.stop(context);
    }
}
```

## Bluetooth State on Android Device

If the user disables Bluetooth, bitgatt will manage that state accordingly, it will drop any pending
transactions in the queue and stop the queues as well as setting it's internal bluetooth state to
off.  All held connection objects will be set to the disconnected state, but they will not be
released.

## [Runtime Mocking](#runtime-mocking)

It is typically difficult to test bluetooth in the Android simulator, currently every
transaction against gatt functionality has a mock transaction that can be used
to simulate succeeding and failing transactions to validate operational behavior.

In the code examples above you can see how this mocking would work.

### Available transaction mocks
Below you will find a list of the available transaction mocks.  You can mock a connection even
with existing real connections if so desired, each connection is self-contained.  With the mocks
you can simulate failure and provide the data to be returned to test content handling.

#### Server
* AddGattServerServiceMockTransaction - Will mock add a service to the phone's GATT server
* BlockingServerTaskTestMockTransaction - Sometimes a developer will want to block the queue
* GattServerConnectMockTransaction - Will mock connect from the GATT server to a peripheral
* GattServerDisconnectMockTransaction - Will mock disconnect from the remote peripheral from the GATT server
* SendGattServerResponseMockTransaction - Will mock up a gatt server response
* NotifyGattServerCharacteristicMockTransaction - Will mock a server notify on a characteristic

#### Client
* BlockingTaskTestMockTransaction - Sometimes a developer will want to block the client connection queue
* CloseGattMockTransaction - Will mock the Android API for gatt close operation
* GattClientDiscoverMockServicesTransaction - Will mock service discovery for a connection
* GatConnectMockTransaction - Will mock the gatt connect operation
* GattDisconnectMockTransaction - Will mock disconnect
* MockNoOpTransaction - Will mock a no-op transaction
* ReadGattCharacteristicMockTransaction - Will mock a characteristic read, can pass in data to be returned as though it were read from the remote device
* ReadGattDescriptorMockTransaction - Will mock a descriptor read, can pass in data to be returned as though it were read from the remote device
* ReadRssiMockTransaction - Will return a mock RSSI value for a remote device
* RequestGattConnectionIntervalMockTransaction - Will perform a mock CI adjustment request, taking the Android const values
* RequestMtuGattMockTransaction - Will request a mock MTU
* SubscribeToCharacteristicNotificationsMockTransaction - This will mock the Android notification setting for a characteristic
* TimeoutTestMockTransaction - This will mock a transaction that times out
* UnSubscribeToGattCharacteristicNotificationsMockTransaction - Will mock an un-subscription from the Android notification
* WriteGattCharacteristicMockTransaction - Will mock a characteristic write to a gatt client connection
* WriteGattDescriptorMockTransaction - Will mock a gatt descriptor write transaction

## Bitgatt Transaction Manual

The transaction manual will explain in detail what each transaction does as well has how strategies
play into transactions.  A strategy can hook into a transaction or transaction subclass to perform
other transactions or to operate on the raw gatt instance.

When below the transaction indicates that it blocks on error, this indicates that the transaction will
leave the connection queue in a state that will not allow subsequent transactions to execute until the
developer runs either the SetClientConnectionStateTransaction, or the SetServerConnectionStateTransaction
explicitly setting the state to one of the IDLE connection states ( DISCONNECTED, IDLE, or CONNECTED ).

What this does is force the developer to acknowledge that the last transaction ran into a severe error, and
that they have done something to mitigate this error or that they are choosing to ignore it.

All transactions have a default timeout of 60s but some are special and will be noted.  Transactions
will callback on the main thread with varying properties contained within the TransactionResult
instance provided to the GattClient or GattServerCallback.

On transaction timeout, the gatt state will be left in whatever in-progress state it was in when
the transaction timed out, this will leave the gatt in an error state and no other transactions will
be able to be run until the state is explicitly reset.  A timeout response will be delivered.

Where possible transactions will return enum values for the GATT status that correspond to the particular
GATT error that occurred instead of just simple error codes.

### AddGattServerServiceCharacteristicDescriptorTransaction

This transaction will add a descriptor to an existing gatt service characteristic.  It is expected that the
caller will check the service to determine whether the descriptor is already hosted or not.

It is also expected that the provided descriptor will already be set up with data
that are desired.

#### Arguments

* GattServerConnection server
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* BluetoothGattDescriptor descriptor
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### AddGattServerServiceCharacteristicTransaction

This transaction will add a characteristic to an existing gatt service.  It is expected that the
caller will check the service to determine whether the characteristic is already hosted or not.

It is also expected that the provided characteristic will already be set up with data or descriptors
that are desired.

#### Arguments

* GattServerConnection server
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### AddGattServerServiceTransaction

Will perform a transaction adding a service to the gatt server.  The caller should check first
as to whether the service exists before adding this service or the result will be a failure.  Keep
in mind that another process or application can manipulate the services and characteristics on
the phone in different ways, so it is always prudent to check and ensure that the state of the
characteristics, descriptors, and services are to your liking before proceeding.

#### Arguments

* GattServerConnection server
* GattState successEndState
* BluetoothGattService service
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if Adapter#getGattServer() returns null
* Includes copy in result? No

### ClearServerServicesTransaction

Will remove all services hosted by this application on the Android devices' GATT server

#### Arguments

* GattServerConnection server
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### CloseGattTransaction

Will close the gatt server, on Marshmallow+ this will also release the [client if|https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/bluetooth/BluetoothGatt.java].  This will leave
the gatt connection in a "disconnected" state.  Please note that this does not mean that the
logical ACL connection is disconnected, the peripheral may well remain connected to the phone.  In
this case it means that the Android process is no longer connected to the phone.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### CreateBondTransaction

It is important to remember that creating a bond in Android is a operation that does not actually
interact with the gatt instance.  Because of the many shifts in connection interval and the other
changes that occur it is wise to not try to attempt other gatt operations while a bond attempt is
in progress. The bond transaction has a different timeout than the other transactions which have
a default timeout of 60s, it has a timeout of 120s.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### GattClientDiscoverServicesTransaction

Service discovery must be performed whenever a new connection is made after scanning to be certain
that the handles that point to resources on the gatt DB are matched up.  Service discovery also must
be performed when the developer suspects that services have changed on the remote side.  On some
Android devices, this will happen automatically when the services change via the service changed
notification, on others refresh will have to be called and discovery performed again.

The operating system handles the GATT DB caching, so there is no need to worry about over-discovering.  If
the service data is fresh it is expected that discovery success will be called quickly.  Discovery
is expensive when it must be done and can take some time.  While discovery is progressing no other
GATT transactions can be executed.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, only if the api discover call returns false
* Includes copy in result? No, but does return a list of remote services discovered

### GattClientRefreshGattTransaction

This transaction should only reluctantly be used by upstream strategies to resolve connection
issues after entirely understanding the problem and after working to resolve it with other
conventional solutions.  This call is likely to not work at some point and it's use is at your
own risk.  In the past it has caused some phones to have inconsistencies between their gatt cache
and what was in the filesystem ( db ) among other issues, please only use this if you know exactly
why you are doing it and in the narrowest of circumstances when you know that it will help.

There is one other reason to use refresh is if you are having a problem with the services changed
characteristic not updating the GATT DB.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### GattConnectTransaction

Given a connection object will attempt to establish a GATT connection with the remote device.  When
a failure occurs, will populate the TransactionResult with as much information as is available.

The gatt connect transaction is shorter than the default 60s and is only 30s, because on all devices
the connection should complete within this time.

This GATT operation is somewhat misleading on Android, what this actually does is perform a high-duty
cycle scan to determine if the specific device is within range, if it is, then it will establish an
ACL connection, go through the standard bluetooth low energy connection negotiation, then establish a
gatt_if within the adapter, culminating in a client_if assigned to the instance of the gatt callback
provided.  There is a maximum number of gatt_if(s), and client_if(s) that are allowed to be assigned
 to a particular peripheral and this varies per OEM device.  Bitgatt is designed to prevent a developer
 from obtaining too many client_if(s) which is a typical problem with many naive GATT implementations
 on Android.
 
 If the device is already connected, this call appears to still perform a scan, at the end of which
 it will either (a) assign a new client_if if this device has never been connected before, or the
 existing client_if has been released, or (b) re-connect your process to the existing client_if which
 in some cases can be a problem.
 
 You can think of the gatt_if as connecting the ACL connection to the Android OS process, and the
 client_if of connecting the connection that Android OS has to the ACL connection to your process.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### GattDisconnectTransaction

Given a connection object will attempt to establish a GATT connection with the remote device.  When
a failure occurs, will populate the TransactionResult with as much information as is available.

The gatt connect transaction is shorter than the default 60s and is only 30s, because on all devices
the connection should complete within this time.  Will wait enough time to allow Android to release
the client_if in the case that the queue is stuck which the developer can not inspect.  Rapid connection / disconnection
cycles are not possible with bitgatt.  If you need to do this, you should obtain the raw GATT
instance from the GattConnection object.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### GattServerConnectTransaction

Is similar to the GattConnectTransaction, except that it will perform the gatt connection from
the gatt server.

#### Arguments

* GattServerConnection server
* GattState successEndState
* FitbitBluetoothDevice device
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### GattServerDisconnectTransaction

Is similar to the GattDisconnectTransaction, except that it will perform the gatt disconnection from
the gatt server.

#### Arguments

* GattServerConnection server
* GattState successEndState
* FitbitBluetoothDevice device
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### GetGattServerServicesTransaction

A transaction to get the gatt server services hosted
by the local gatt server.  This exists primarily to prevent
the internal to the Android stack CME that can occur if we are reading services
and adding, by making this a transaction a caller should not
read and add at the same time.

#### Arguments

* GattServerConnection server
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### NotifyGattServerCharacteristicTransaction

Will notify on a provided characteristic on the local gatt server.  This will send a notification
or indication to a remote device.


Only provide characteristic instances obtained from the local service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* GattServerConnection connection
* FitbitBluetoothDevice device
* GattState successEndState
* BluetoothGattCharacteristic characteristic
* boolean confirm
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the notify fails
* Includes copy in result? No, but does include data in the TransactionResult

### ReadGattCharacteristicTransaction

Will read the data value of a remote characteristic

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the read characteristic fails
* Includes copy in result? No, but does include data in the TransactionResult

### ReadGattClientPhyTransaction

Will read the gatt client physical layer to determine whether the current physical layer that the
GattConnection is using is 2 Msym, 1 Msym, or CODED.

#### Arguments

* GattConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### ReadGattDescriptorTransaction

Will read the data value of a remote descriptor

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* BluetoothGattDescriptor descriptor
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the descriptor read fails
* Includes copy in result? No, but does include data in the TransactionResult

### ReadGattServerCharacteristicDescriptorValueTransaction

Will read a characteristic descriptor from a local gatt server and populate a transaction result with
response.  This and ReadGattServerCharacteristicValueTransaction are sort of conveniences
for testing, there is no clear reason why one wouldn't perform these operations in Java, they have
no impact to the state machine.  But there is no harm in mainstream code using these transactions.

Only provide descriptor instances obtained from the local service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* BluetoothGattDescriptor descriptor
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No, but does include data in the TransactionResult

### ReadGattServerCharacteristicValueTransaction

Will read a characteristic from a local gatt server and populate a transaction result with
response.  This and ReadGattServerCharacteristicDescriptorValueTransaction are sort of conveniences
for testing, there is no clear reason why one wouldn't perform these operations in Java, they have
no impact to the state machine.  But there is no harm in mainstream code using these transactions.

Only provide characteristic instances obtained from the local service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No, but does include data in the TransactionResult

### ReadRssiTransaction

Will read the RSSI from a remote device

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### RemoveGattServerServicesTransaction

Will remove a local gatt server service.  It is a good idea to use the transaction to make sure
that nothing else can be interacting with the service while the developer is removing it.

#### Arguments

* GattServerConnection server
* GattState successEndState
* BluetoothGattService service
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### RequestGattClientPhyChangeTransaction

Will request a PHY change for the gatt client connection.  If the client supports the requested
PHY then it will adjust, if not it will return with failure.  Only Oreo and up.  If this is
called on a non-oreo and higher Android device will be a no-op.

#### Arguments

* GattConnection connection
* GattState successEndState
* int txPhy
* int rxPhy
* int phyOptions
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### RequestGattConnectionIntervalTransaction

 Will request a connection interval change, on Android there are three levels basically low, mid, high
 each one will negotiate an appropriate connection interval for that phone.

 The downside is that while you can request a connection interval, there is no response, the only
 way to know what you got is to look at the logs.  This needs care as well because it's possible
 to jam the gatt if the CI change comes from both the central and peripheral at the same time, so
 tread lightly.
 
 The speeds will use the Speed enum to map to the default Android CI ranges, these are different on
 different versions of Android, please check the Android source to be certain, however roughly
 they are:
 * Fast - 15 ~ 24 CI
 * Medium - 24 ~ 42 CI
 * Slow - 42 ~ 100 CI
 
 Please remember a few things about CI, > 100 seems to increase the likelihood of disconnections,
 changing CIs on some Android versions more frequently than once every 30 seconds will lead to
 disconnections.  If you are seeing excessive disconnections in general, it is worthwhile to
 look at your connection interval management. 

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* Speed connectionSpeed
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### RequestMtuGattTransaction

 Will request a new MTU from the peripheral, the maximum MTU is 517, though it is up to the peripheral
 to determine what it actually supports.  The MTU is not the actual payload size as there is overhead.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* int mtu
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### SendGattServerResponseTransaction

 Will send the gatt server response to a descriptor read / write request.  The actual write / read
 transaction should have completed prior to sending this.

#### Arguments

* GattServerConnection server
* GattState successEndState
* FitbitBluetoothDevice device
* int requestId
* int status
* int offset
* byte[] value
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### SetClientConnectionStateTransaction

 Will set the client connection state to whatever the developer wishes.  If the transaction queue
 gets halted by an error, this can be reset to whatever idle state is appropriate.
 
 This transaction is designed to block the transaction queue while modifying the state of the
 connection.  This should only be used to reset the connection to a usable state after an error
 that has been ADDRESSED.  This should not be used to ignore errors.  Where appropriate create
 a non-gatt library strategy to use this transaction appropriately.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* GattState destinationState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### SetServerConnectionStateTransaction

 Will set the server connection state to whatever the developer wishes.  If the transaction queue
 gets halted by an error, this can be reset to whatever idle state is appropriate.
 
 This transaction is designed to block the transaction queue while modifying the state of the
 connection.  This should only be used to reset the connection to a usable state after an error
 that has been ADDRESSED.  This should not be used to ignore errors.  Where appropriate create
 a non-gatt library strategy to use this transaction appropriately.

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* GattState destinationState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

### SubscribeToCharacteristicNotificationsTransaction

Subscribes to characteristic notifications.  The Android API call does NOT operate in the way one
would expect.  This directs remote GATT server notifications and indications that are received
by the Android Adapter's gatt_if through to the client_if assigned to your application's process.

What this means is that if discovery has not been completed this can fail in strange ways.  It also
means that performing this operation does not actually tell the remote GATT server to start sending
notifications, only writing to the notification descriptor on that characteristic does that.

The API indicates that this particular call is idempotent, however you should avoid both over-calling
this transaction as well as over-writing the descriptor.  Keep your subscription state externally
and only call as required.

Only provide characteristic instances obtained from the remote service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the write ends up in a stack NPE ( usually means the device silently disconnected )
* Includes copy in result? No

### UnSubscribeToGattCharacteristicNotificationsTransaction

Un-subscribes to characteristic notifications.  The Android API call does NOT operate in the way one
would expect.  This directs remote GATT server notifications and indications that are received
by the Android Adapter's gatt_if through to the client_if assigned to your application's process.

What this means is that if discovery has not been completed this can fail in strange ways.  It also
means that performing this operation does not actually tell the remote GATT server to stop sending
notifications, only writing to the notification descriptor on that characteristic does that.

The API indicates that this particular call is idempotent, however you should avoid both over-calling
this transaction as well as over-writing the descriptor.  Keep your subscription state externally
and only call as required.

Only provide characteristic instances obtained from the remote service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the write ends up in a stack NPE ( usually means the device silently disconnected )
* Includes copy in result? No

### WriteGattCharacteristicTransaction

Will perform a gatt characteristic write.  The developer should have populated the value property
of the characteristic object before performing this transaction.

Only provide characteristic instances obtained from the remote service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* BluetoothGattCharacteristic characteristic
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the write ends up in a stack NPE ( usually means the device silently disconnected )
* Includes copy in result? No

### WriteGattDescriptorTransaction

Will perform a gatt descriptor write.  The developer should have populated the value property
of the descriptor object before performing this transaction.

Only provide descriptor instances obtained from the remote service characteristic, if you create them yourself
you will not have a valid instance id, which is an internal property of the descriptor and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattConnection connection
* GattState successEndState
* BluetoothGattDescriptor descriptor
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the write ends up in a stack NPE ( usually means the device silently disconnected )
* Includes copy in result? No

### WriteGattServerCharacteristicDescriptorValueTransaction

Will write a characteristic descriptor from a local gatt server and populate a transaction result with
response.  This and WriteGattServerCharacteristicValueTransaction are sort of conveniences
for testing, there is no clear reason why one wouldn't perform these operations in Java, they have
no impact to the state machine.  But there is no harm in mainstream code using these transactions.

Only provide descriptor instances obtained from the local service, if you create them yourself
you will not have a valid instance id, which is an internal property of the descriptor and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* BluetoothGattDescriptor descriptor
* byte[] data
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the server, service, characteristic, or descriptor doesn't exist
* Includes copy in result? No, but does include data in the TransactionResult

### WriteGattServerCharacteristicValueTransaction

Will write a characteristic from a local gatt server and populate a transaction result with
response.  This and WriteGattServerCharacteristicDescriptorValueTransaction are sort of conveniences
for testing, there is no clear reason why one wouldn't perform these operations in Java, they have
no impact to the state machine.  But there is no harm in mainstream code using these transactions.

Only provide characteristic instances obtained from the local service, if you create them yourself
you will not have a valid instance id, which is an internal property of the characteristic and could
wedge the GATT queue and cause the system to become unresponsive.

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* BluetoothGattService service
* BluetoothGattCharacteristic characteristic
* byte[] data
* (optional) long timeoutMillis

#### Results

* Does it block on failure? Yes, if the characteristic doesn't exist
* Includes copy in result? No, but does include data in the TransactionResult



### CloseGattServerTransaction

Will close the currently held instance of the gatt server, potentially useful if services are not
released when bluetooth is toggled, you can use the clear services transaction and then the
close transaction to ensure that when BT is re-enabled there are no remaining services.  This may also
be used in the turning off callback if you implement your own bluetooth listener.

#### Arguments

* @Nullable GattServerConnection connection
* GattState successEndState
* (optional) long timeoutMillis

#### Results

* Does it block on failure? No
* Includes copy in result? No

## License

    Copyright 2019 Fitbit, Inc. All rights reserved.
    
    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at https://mozilla.org/MPL/2.0/.


[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FFitbit%2Fbitgatt.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2FFitbit%2Fbitgatt?ref=badge_large)
