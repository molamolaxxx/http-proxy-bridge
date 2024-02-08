package com.mola.proxy.reverse;

import com.mola.proxy.core.ext.HostMappingExt;
import com.mola.proxy.core.utils.ConfigQueryUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-05 02:12
 **/
public class HostMappingExtImpl implements HostMappingExt {

    private volatile Map<String, String> cachedMapping;

    public HostMappingExtImpl(Map<String, String> config) {
        this.cachedMapping = config;
    }

    @Override
    public String fetchMappedAddress(String host, int port) {
        String hosts = cachedMapping.get("map2Localhost");
        if (hosts != null && hosts.contains(host)) {
            return "localhost:80";
        }
        return null;
    }
}
