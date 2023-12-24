package com.mola.proxy.core.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-03 15:40
 **/
public class KeyValueParser {

    public static Map<String, String> parse(String str) {
        if (str == null || str.trim().length() == 0) {
            return new HashMap<>();
        }
        String[] split = str.split(";");
        if (split.length == 0) {
            return new HashMap<>();
        }
        Map<String, String> kvMap = new HashMap<>();
        for (String kv : split) {
            String[] kvArr = kv.split("=");
            if (kvArr.length != 2) {
                continue;
            }
            kvMap.put(kvArr[0], kvArr[1]);
        }
        return kvMap;
    }
}
