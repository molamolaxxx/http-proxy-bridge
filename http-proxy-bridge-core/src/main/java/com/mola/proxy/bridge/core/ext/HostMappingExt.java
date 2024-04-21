package com.mola.proxy.bridge.core.ext;

import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-05 02:04
 **/
public interface HostMappingExt {

    /**
     * @param host
     * @param port
     * @return ip:port
     */
    String fetchMappedAddress(String host, int port);

    /**
     * 通过本地端口，获取固定连接的host
     * @param proxyHost
     * @return ip:port
     */
    List<String> fetchAppointHostByLocalPort(String proxyHost);
}
