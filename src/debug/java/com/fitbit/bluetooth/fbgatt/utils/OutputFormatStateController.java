/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.utils;

/**
 * Interface used to manage the formatting of the output.
 */
public interface OutputFormatStateController {
    /**
     * Set the format of the output to JSON (for value TRUE) or non-JSON (for value FALSE).
     *
     * @param isJsonFormat
     */
    void setJsonFormat(boolean isJsonFormat);

    boolean isJsonFormat();
}
