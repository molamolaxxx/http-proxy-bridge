package com.mola.proxy.bridge.core.utils;

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 解析http代理明文的请求头
 * @date : 2023-10-23 22:10
 **/

public class HeaderParser {

    private static final Logger logger = LoggerFactory.getLogger(HeaderParser.class);

    private static final String CONNECT_METHOD_NAME = "CONNECT";

    private static final String HOST = "host";

    public static ProxyHttpHeader parse(String header) {
        String[] lines = header.split("\\n");
        String hostTemp = "";
        boolean isConnectMethod = false;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            if (line.split(" ")[0].equalsIgnoreCase(CONNECT_METHOD_NAME)) {
                isConnectMethod = true;
            }
            String[] hostLine = line.split(": ");
            if (hostLine[0].equalsIgnoreCase(HOST)) {
                hostTemp = hostLine[1];
            }
        }

        String host = "";
        int port = 80;
        String[] hostAndPortArr = hostTemp.split(":");
        if(hostAndPortArr.length > 1) {
            host = hostAndPortArr[0];
            port = tryParsePort(hostAndPortArr);
        }else {
            host = hostAndPortArr[0].split("\\r")[0];
        }

        return new ProxyHttpHeader(host, port, isConnectMethod);
    }

    private static int tryParsePort(String[] hostAndPortArr) {
        try {
            return Integer.parseInt(hostAndPortArr[1].split("\\r")[0]);
        } catch (NumberFormatException nfe) {
            logger.error("HeaderParser tryParsePort error, use default, hostAndPortArr = [{}][{}]",
                    hostAndPortArr[0], hostAndPortArr[1], nfe);
        }
        return -1;
    }
}
