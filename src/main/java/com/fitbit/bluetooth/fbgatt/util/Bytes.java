/*
 * Copyright 2019 Fitbit, Inc. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.fitbit.bluetooth.fbgatt.util;

import java.util.List;

/**
 * A utility for manipulating byte arrays
 *
 * Created by iowens on 12/6/17.
 */

public class Bytes {
    public static String byteArrayToHexString(byte in[]) {
        return byteArrayToHexString(in, true);
    }

    public static String byteArrayToHexString(byte in[], boolean insertSpaces) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) {
            return null;
        }
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
        StringBuffer out_str_buf = new StringBuffer(in.length * 2);
        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0); // Strip off high nibble
            ch = (byte) (ch >>> 4); // shift the bits down
            ch = (byte) (ch & 0x0F); // must do this is high order bit is on!
            out_str_buf.append(pseudo[ch]); // convert the nibble to a String Character
            ch = (byte) (in[i] & 0x0F); // Strip off low nibble
            out_str_buf.append(pseudo[ch]); // convert the nibble to a String Character
            i++;
            if (insertSpaces && i % 4 == 0) {
                out_str_buf.append(" ");
            }
        }
        String rslt = new String(out_str_buf);
        return rslt;
    }

    public static boolean isValidHexString(String s) {
        int len = s.length();
        if((len >= 2) && ((len & 0x01) == 0)) {
            for(int i = 0; i < len; i++) {
                if((Character.digit(s.charAt(i), 16)) < 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] reverse(byte[] array) {
        if (array == null) {
            return null;
        }

        int len = array.length;
        byte[] reversedArray = new byte[len];
        for (int i = 0; i < len; i++) {
            reversedArray[len - i - 1] = array[i];
        }
        return reversedArray;
    }

    public static int getLength(List<byte[]> data) {
        int length = 0;
        for (byte[] block : data) {
            length += block.length;
        }
        return length;
    }
}

