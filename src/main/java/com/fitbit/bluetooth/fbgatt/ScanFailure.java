/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt;

/**
 * An enum to map the scan failure reasons to plain english, values are coming from the
 * Android Pie source
 */

public enum ScanFailure {
    /**
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;

    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;

    public static final int SCAN_FAILED_INTERNAL_ERROR = 3;

    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;

    public static final int SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5;

    public static final int SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6;

    static final int NO_ERROR = 0;
     */
    NO_ERROR(0),
    SCAN_FAILED_ALREADY_STARTED(1),
    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED(2),
    SCAN_FAILED_INTERNAL_ERROR(3),
    SCAN_FAILED_FEATURE_UNSUPPORTED(4),
    SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES(5),
    SCAN_FAILED_SCANNING_TOO_FREQUENTLY(6),
    UNKNOWN(999);

    private int failReason;

    ScanFailure(int reason) {
        this.failReason = reason;
    }

    /**
     * Will fetch the failure reason for a given response from the system scanner, if the reason
     * integer doesn't match, will return unknown
     * @param reason The failure reason integer
     * @return A failure reason
     */

    public static ScanFailure getFailureForReason(int reason) {
        for(ScanFailure failure : values()) {
            if(failure.failReason == reason) {
                return failure;
            }
        }
        return UNKNOWN;
    }

    /**
     * Will return the failure reason code for this enum
     * @return The failure reason code
     */

    public int getFailReason(){
        return failReason;
    }
}
