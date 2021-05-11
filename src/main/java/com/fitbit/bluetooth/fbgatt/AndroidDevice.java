/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;

/**
 * An android device, which is an encapsulation of the build variables in a simple comparable
 * object.  The intended use is for a strategy consumer to create a target android device that
 * will be compared to the values from the present android mobile device.
 * <p>
 * The comparator will consider equivalence to be any defined values in both objects to be matching
 * absent case.
 */

public class AndroidDevice {
    private final HashMap<String, Object> androidProperties = new HashMap<>();
    private final static String MANUFACTURER_NAME_KEY = "manufacturerName";
    private final static String DEVICE_MODEL_KEY = "deviceModel";
    private final static String API_LEVEL_KEY = "apiLevel";
    private final static String RADIO_VERSION_KEY = "radioVersion";
    private final static String BOARD_KEY = "board";
    private final static String BOOTLOADER_KEY = "bootloader";
    private final static String BRAND_KEY = "brand";
    private final static String DISPLAY_KEY = "display";
    private final static String FINGERPRINT_KEY = "fingerprint";
    private final static String DEVICE_KEY = "device";
    private final static String HARDWARE_KEY = "hardware";
    private final static String HOST_KEY = "host";
    private final static String ID_KEY = "id";
    private final static String PRODUCT_KEY = "product";
    private final static String TYPE_KEY = "type";
    private final int numberOfSetProperties;

    private AndroidDevice(String manufacturerName, String deviceModel, Integer apiLevel, String radioVersion,
                          String board, String bootloader, String brand, String display, String fingerprint,
                          String device, String hardware, String host, String id, String product, String type, int numberOfSetProperties) {
        androidProperties.put(MANUFACTURER_NAME_KEY, manufacturerName);
        androidProperties.put(DEVICE_MODEL_KEY, deviceModel);
        androidProperties.put(API_LEVEL_KEY, apiLevel);
        androidProperties.put(RADIO_VERSION_KEY, radioVersion);
        androidProperties.put(BOARD_KEY, board);
        androidProperties.put(BOOTLOADER_KEY, bootloader);
        androidProperties.put(BRAND_KEY, brand);
        androidProperties.put(DISPLAY_KEY, display);
        androidProperties.put(FINGERPRINT_KEY, fingerprint);
        androidProperties.put(DEVICE_KEY, device);
        androidProperties.put(HARDWARE_KEY, hardware);
        androidProperties.put(HOST_KEY, host);
        androidProperties.put(ID_KEY, id);
        androidProperties.put(PRODUCT_KEY, product);
        androidProperties.put(TYPE_KEY, type);

        this.numberOfSetProperties = numberOfSetProperties;
    }

    public HashMap<String, Object> getAndroidProperties() {
        return androidProperties;
    }

    /**
     * The manufacturer name of the android device
     *
     * @return The manufacturer name
     */
    public @Nullable
    String getManufacturerName() {
        return (String) androidProperties.get(MANUFACTURER_NAME_KEY);
    }

    /**
     * The model string of the android device
     *
     * @return The model
     */
    public @Nullable
    String getDeviceModel() {
        return (String) androidProperties.get(DEVICE_MODEL_KEY);
    }

    /**
     * The api level of the android device
     *
     * @return The api level
     */
    public Integer getApiLevel() {
        return (Integer) androidProperties.get(API_LEVEL_KEY);
    }

    /**
     * The radio version string of the android device
     *
     * @return The radio version
     */
    public @Nullable
    String getRadioVersion() {
        return (String) androidProperties.get(RADIO_VERSION_KEY);
    }

    /**
     * The board string of the android device
     *
     * @return The board
     */
    public @Nullable
    String getBoard() {
        return (String) androidProperties.get(BOARD_KEY);
    }

    /**
     * The bootloader string of the android device
     *
     * @return The bootloader
     */
    public @Nullable
    String getBootloader() {
        return (String) androidProperties.get(BOOTLOADER_KEY);
    }

    /**
     * The user brand string of the android device
     *
     * @return The brand
     */
    public @Nullable
    String getBrand() {
        return (String) androidProperties.get(BRAND_KEY);
    }

    /**
     * The display string of the android device
     *
     * @return The display
     */
    public @Nullable
    String getDisplay() {
        return (String) androidProperties.get(DISPLAY_KEY);
    }

    /**
     * The device fingerprint string of the android device
     *
     * @return The fingerprint
     */
    public @Nullable
    String getFingerprint() {
        return (String) androidProperties.get(FINGERPRINT_KEY);
    }

    /**
     * The device string of the android device
     *
     * @return The device string
     */
    public @Nullable
    String getDevice() {
        return (String) androidProperties.get(DEVICE_KEY);
    }

    /**
     * The hardware string of the android device
     *
     * @return The hardware
     */
    public @Nullable
    String getHardware() {
        return (String) androidProperties.get(HARDWARE_KEY);
    }

    /**
     * The host string of the android device
     *
     * @return The host
     */
    public @Nullable
    String getHost() {
        return (String) androidProperties.get(HOST_KEY);
    }

    /**
     * The id string of the android device
     *
     * @return The id
     */
    public @Nullable
    String getId() {
        return (String) androidProperties.get(ID_KEY);
    }

    /**
     * The product name string of the android device
     *
     * @return The product
     */
    public @Nullable
    String getProduct() {
        return (String) androidProperties.get(PRODUCT_KEY);
    }

    /**
     * The type string of the android device
     *
     * @return The type
     */
    public @Nullable
    String getType() {
        return (String) androidProperties.get(TYPE_KEY);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AndroidDevice) {
            AndroidDevice otherDevice = (AndroidDevice) obj;
            return this.androidProperties.entrySet().size() == otherDevice.androidProperties.entrySet().size() &&
                    this.androidProperties.entrySet().containsAll(otherDevice.androidProperties.entrySet());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return  androidProperties.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "AndroidDevice[%s, %s, %s, %s, %s, %s]", getManufacturerName(), getDeviceModel(), getApiLevel(), getHardware(), getBrand(), getBoard());
    }

    /**
     * Builder for an Android device
     */

    public static class Builder {
        String manufacturerName;
        String deviceModel;
        Integer apiLevel;
        String radioVersion;
        String board;
        String bootloader;
        String brand;
        String display;
        String fingerprint;
        String device;
        String hardware;
        String host;
        String id;
        String product;
        String type;
        int numberOfParamsSet = 0;

        public Builder manufacturerName(String manufacturerName) {
            this.manufacturerName = manufacturerName;
            if (this.manufacturerName != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder deviceModel(String deviceModel) {
            this.deviceModel = deviceModel;
            if (this.deviceModel != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder apiLevel(Integer apiLevel) {
            this.apiLevel = apiLevel;
            if (this.apiLevel != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder radioVersion(String radioVersion) {
            this.radioVersion = radioVersion;
            if (this.radioVersion != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder board(String board) {
            this.board = board;
            if (this.board != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder bootloader(String bootloader) {
            this.bootloader = bootloader;
            if (this.bootloader != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            if (this.brand != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder display(String display) {
            this.display = display;
            if (this.display != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder device(String device) {
            this.device = device;
            if (this.device != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            if (this.fingerprint != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder hardware(String hardware) {
            this.hardware = hardware;
            if (this.hardware != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            if (this.host != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            if (this.id != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder product(String product) {
            this.product = product;
            if (this.product != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            if (this.type != null) {
                numberOfParamsSet++;
            }
            return this;
        }

        public AndroidDevice build() {
            return new AndroidDevice(manufacturerName, deviceModel, apiLevel, radioVersion, board,
                    bootloader, brand, display, fingerprint, device, hardware, host, id, product, type, numberOfParamsSet);
        }
    }
}
