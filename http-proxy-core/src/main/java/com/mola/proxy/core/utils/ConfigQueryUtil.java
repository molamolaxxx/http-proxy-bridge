package com.mola.proxy.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-04 19:20
 **/
public class ConfigQueryUtil {

    private static final Logger log = LoggerFactory.getLogger(ConfigQueryUtil.class);

    public static Map<String, String> getConfig(String[] args) {
        Map<String, String> parseRes = new HashMap<>();
        if (args != null && args.length != 0) {
            String configKey = args[0];
            try {
                String res = HttpCommonService.INSTANCE
                        .get("https://molaspace.xyz:8550/chat/kv/" + configKey, 1000);
                parseRes = KeyValueParser.parse(res);
            } catch (Exception e) {
                log.error("getConfig failed", e);
            }
        }
        return parseRes;
    }

    public static Map<String, String> getConfig(String configKey) {
        try {
            String res = HttpCommonService.INSTANCE
                    .get("https://molaspace.xyz:8550/chat/kv/" + configKey, 1000);
            return KeyValueParser.parse(res);
        } catch (Exception e) {
            log.error("getConfig failed", e);
        }
        return new HashMap<>();
    }
}
