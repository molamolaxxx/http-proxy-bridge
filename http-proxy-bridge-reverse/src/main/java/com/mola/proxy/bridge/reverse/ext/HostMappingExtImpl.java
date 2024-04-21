package com.mola.proxy.bridge.reverse.ext;

import com.mola.proxy.bridge.core.ext.HostMappingExt;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-05 02:12
 **/
public class HostMappingExtImpl implements HostMappingExt {

    private final Map<String, String> hostMapping;

    private final Map<String, List<String>> appointHostsByProxyHost = new HashMap<>();

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

    @Override
    public List<String> fetchAppointHostByLocalPort(String proxyHost) {
        return appointHostsByProxyHost.getOrDefault(proxyHost, Collections.emptyList());
    }

    public void registerAppointHosts(String proxyHost, List<String> appointHosts) {
        appointHostsByProxyHost.put(proxyHost, appointHosts);
    }
}
