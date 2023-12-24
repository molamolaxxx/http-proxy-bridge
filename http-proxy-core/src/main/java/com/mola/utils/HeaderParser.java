package com.mola.utils;

import com.mola.entity.ProxyHttpHeader;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: 解析http代理明文的请求头
 * @date : 2023-10-23 22:10
 **/

public class HeaderParser {

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
        if(hostTemp.split(":").length>1) {
            host = hostTemp.split(":")[0];
            port = Integer.valueOf(hostTemp.split(":")[1].split("\\r")[0]);
        }else {
            host = hostTemp.split(":")[0].split("\\r")[0];
        }

        return new ProxyHttpHeader(host, port, isConnectMethod);
    }
}
