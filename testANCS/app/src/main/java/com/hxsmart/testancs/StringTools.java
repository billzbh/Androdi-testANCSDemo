package com.hxsmart.testancs;

import java.nio.ByteBuffer;

/**
 * Created by hxsmart on 2018/3/22.
 */

public class StringTools {
    /**
     * @param b      字节对象
     * @param offset 字节位移
     * @param count  字节数
     * @return hexString字符串
     */
    public static String Bytes2HexString(byte[] b, int offset, int count) {

        if (offset > b.length) {
            return null;
        }
        if (offset + count > b.length) {
            return null;
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < count; i++) {
            String hex = Integer.toHexString(b[offset + i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret.append(hex.toUpperCase());
        }
        return ret.toString();
    }
    /*左右空格都去掉*/
    public static String trim(String str) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            //return leftTrim(rightTrim(str));
            return str.replaceAll("^[　 ]+|[　 ]+$","");
        }
    }
    /*去左空格*/
    public static String leftTrim(String str) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            return str.replaceAll("^[　 ]+", "");
        }
    }
    /*去右空格*/
    public static String rightTrim(String str) {
        if (str == null || str.equals("")) {
            return str;
        } else {
            return str.replaceAll("[　 ]+$", "");
        }
    }

    public static boolean fetchByteBuffer(ByteBuffer byteBuffer, byte[] des, int offset, int len, int delLen) {
        int bytesLength = byteBuffer.position();
        byte[] inputBuffer=byteBuffer.array();

        if (bytesLength < len + offset || bytesLength < delLen)
            return false;
        System.arraycopy(inputBuffer, offset, des, 0, len);
        byteBuffer.position(delLen);
        byteBuffer.compact();
        byteBuffer.position(bytesLength - delLen);
        return true;
    }
}
