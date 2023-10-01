package com.mola.common;

public class HeaderParser {

    public static ProxyHttpHeader parse(String header) {
        String[] lines = header.split("\\n");
        String hostTemp = "";
        boolean isConnectMethod = false;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            if (line.split(" ")[0].equalsIgnoreCase("CONNECT")) {
                isConnectMethod = true;
            }
            String[] hostLine = line.split(": ");
            if (hostLine[0].equalsIgnoreCase("host")) {
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
