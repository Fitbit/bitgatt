/*
 *
 *  Copyright 2021 Fitbit, Inc. All rights reserved.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package com.fitbit.bluetooth.fbgatt.commands;

import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.fitbit.bluetooth.fbgatt.tools.ConsumerInterface;
import com.fitbit.bluetooth.fbgatt.utils.JsonBuilder;

import org.json.JSONObject;

import java.util.Map;

/**
 * A basic response handler for the Gatt transactions.
 */
public class ResponseHandler implements ResponseHandlerInterface {
    private final String command;
    private final PluginLoggerInterface logger;
    private final JsonBuilder jsonBuilder;

    public ResponseHandler(String command, PluginLoggerInterface logger) {
        this.command = command;
        this.logger = logger;

        this.jsonBuilder = new JsonBuilder(logger);
    }


    @Override
    public void onMessage(PluginCommandConfig config, String message) {
        logger.logMsg(message);
        ConsumerInterface consumer = config.getConsumer();

        if (config.isJsonFormat()) {
            JSONObject jsonResult = jsonBuilder.buildJsonMessage(command, message);
            consumer.consumeJson(jsonResult);
        } else {
            consumer.consumeResult(message);
        }
    }

    @Override
    public void onResponse(PluginCommandConfig config, TransactionResult result) {
        logger.log(result);
        ConsumerInterface consumer = config.getConsumer();

        if (config.isJsonFormat()) {
            JSONObject jsonResult = jsonBuilder.buildJsonResult(command, result);
            consumer.consumeJson(jsonResult);
        } else {
            consumer.consumeResult(result.toString());
        }
    }

    @Override
    public void onResponse(PluginCommandConfig config, TransactionResult result, String error, Map<String, Object> map) {
        logger.log(result);
        ConsumerInterface consumer = config.getConsumer();

        if (config.isJsonFormat()) {
            JSONObject jsonResult = jsonBuilder.buildJsonResult(command, result.getResultStatus(), result.toString(), map);
            consumer.consumeJson(jsonResult);
        } else {
            consumer.consumeResult(result.toString());
        }
    }

    @Override
    public void onError(PluginCommandConfig config, Exception e) {
        logger.logError(e);
        ConsumerInterface consumer = config.getConsumer();

        if (config.isJsonFormat()) {
            JSONObject jsonError = jsonBuilder.buildJsonError(command, e);
            consumer.consumeJson(jsonError);
        } else {
            consumer.consumeError(e);
        }
    }
}
