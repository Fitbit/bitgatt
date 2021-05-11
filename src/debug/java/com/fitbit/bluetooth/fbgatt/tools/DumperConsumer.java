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

import com.fitbit.bluetooth.fbgatt.logger.PluginLoggerInterface;
import com.facebook.stetho.dumpapp.DumperContext;
import org.json.JSONObject;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Default consumer based on the Stetho commands.
 */
class DumperConsumer implements ConsumerInterface {
    private final PrintStream printStream;
    private final PrintStream errStream;
    private final PluginLoggerInterface logger;

    DumperConsumer(DumperContext dumperContext, PluginLoggerInterface logger) {
        this.printStream = dumperContext.getStdout();
        this.errStream = dumperContext.getStderr();
        this.logger = logger;
    }

    @Override
    public void consumeResult(String resultString) {
        consumeMessage(resultString);
    }

    @Override
    public void consumeJson(JSONObject jsonObject) {
        consumeMessage(jsonObject.toString());
    }

    @Override
    public void consumeError(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        String exceptionAsString = sw.toString();
        String error = "\u001B[31m " + exceptionAsString + " \u001B[0m";

        logger.logError(e);

        errStream.println(error);
        errStream.flush();
    }

    private void consumeMessage(String message) {
        logger.logMsg("Dumped message: " + message);

        try {
            printStream.println(message);
            printStream.flush();
        } catch (Exception e) {
            logger.logError(e);
        }
    }
}
