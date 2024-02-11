package com.mola.proxy.bridge.core.utils;

/**
 * @author : molamola
 * @Project: my-rpc
 * @Description: http-proxy-bridge
 * @date : 2023-08-06 16:21
 **/
public class AssertUtil {

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new RuntimeException(message);
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new RuntimeException(message);
        }
    }
}
