/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.tools;

import org.json.JSONObject;

/**
 * Interface for dumping results to a consumer.
 */
public interface ConsumerInterface {
    /**
     * Consume a String result
     *
     * @param resultString the result in String format
     */
    void consumeResult(String resultString);

    /**
     * Consume a JSON result
     *
     * @param jsonObject the result in JSON format
     */
    void consumeJson(JSONObject jsonObject);

    /**
     * Consume an Exception
     *
     * @param e the exception
     */
    void consumeError(Exception e);
}
