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

import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.COMMAND_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.ERROR_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.FAIL_STATUS;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.PASS_STATUS;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.RESULT_KEY;
import static com.fitbit.bluetooth.fbgatt.tools.GattServerConnectionConsts.STATUS_KEY;

/**
 * Util builder class.
 */
public class JsonBuilder {
    private final PluginLoggerInterface logger;

    public JsonBuilder(PluginLoggerInterface logger) {
        this.logger = logger;
    }

    public JSONObject buildJsonResult(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            logger.logError(e);
        }

        return jsonObject;
    }

    public JSONObject buildJsonResult(String command, TransactionResult.TransactionResultStatus status, String error, Map<String, Object> resultMap) {
        String statusString = status == TransactionResult.TransactionResultStatus.SUCCESS ? PASS_STATUS : FAIL_STATUS;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, command);
        map.put(STATUS_KEY, statusString);
        if (statusString.equals(PASS_STATUS)) {
            map.put(RESULT_KEY, buildJsonResult(resultMap));
        } else {
            map.put(ERROR_KEY, error);
        }

        return buildJsonResult(map);
    }

    public JSONObject buildJsonResult(String command, TransactionResult result) {
        String status = result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS) ? PASS_STATUS : FAIL_STATUS;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, command);
        map.put(STATUS_KEY, status);
        if (result.getResultStatus() == TransactionResult.TransactionResultStatus.SUCCESS) {
            map.put(RESULT_KEY, result.toString());
        } else {
            map.put(ERROR_KEY, result.toString());
        }

        return buildJsonResult(map);
    }

    public JSONObject buildJsonMessage(String command, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, command);
        map.put(STATUS_KEY, PASS_STATUS);
        map.put(RESULT_KEY, message);
        return buildJsonResult(map);
    }

    public JSONObject makeJsonResult(String command, String status, String error, JSONArray result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, command);
        map.put(STATUS_KEY, status);

        if (PASS_STATUS.equalsIgnoreCase(status)) {
            map.put(RESULT_KEY, result);
        } else {
            map.put(ERROR_KEY, error);
        }

        return buildJsonResult(map);
    }

    public JSONObject buildJsonError(String command, Exception e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(COMMAND_KEY, command);
        map.put(STATUS_KEY, FAIL_STATUS);

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        String error = "\u001B[31m " + exceptionAsString + " \u001B[0m";

        map.put(ERROR_KEY, error);

        return buildJsonResult(map);
    }
}
