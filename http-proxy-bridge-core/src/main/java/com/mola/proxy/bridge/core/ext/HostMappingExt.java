package com.mola.proxy.bridge.core.ext;

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
}
