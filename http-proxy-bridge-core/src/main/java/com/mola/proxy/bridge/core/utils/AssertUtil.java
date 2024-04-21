package com.mola.proxy.bridge.core.utils;

import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
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

    public static void notEmpty(List<?> list, String message) {
        if (list == null || list.size() == 0) {
            throw new RuntimeException(message);
        }
    }
}
