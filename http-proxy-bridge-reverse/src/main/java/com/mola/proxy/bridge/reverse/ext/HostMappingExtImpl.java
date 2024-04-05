package com.mola.proxy.bridge.reverse.ext;

import com.mola.proxy.bridge.core.ext.HostMappingExt;

import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-05 02:12
 **/
public class HostMappingExtImpl implements HostMappingExt {

    private volatile Map<String, String> hostMapping;

    public HostMappingExtImpl(Map<String, String> hostMapping) {
        this.hostMapping = hostMapping;
    }

    @Override
    public String fetchMappedAddress(String host, int port) {
        if (hostMapping == null || hostMapping.size() == 0) {
            return null;
        }
        String mappedValue = null;
        String mappedKey = host + ":" + port;
        if (hostMapping.containsKey(mappedKey)) {
            mappedValue = hostMapping.get(mappedKey);
        } else {
            mappedValue = hostMapping.get(host);
        }

        if (mappedValue == null || mappedValue.length() == 0) {
            return null;
        }

        String[] mappedHostAndPort = mappedValue.split(":");
        if (mappedHostAndPort.length == 2) {
            return mappedValue;
        }
        return mappedValue + ":" + port;
    }
}
