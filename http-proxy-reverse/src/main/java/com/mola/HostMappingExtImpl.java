package com.mola;

import com.mola.ext.HostMappingExt;
import com.mola.utils.ConfigQueryUtil;

import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-05 02:12
 **/
public class HostMappingExtImpl implements HostMappingExt {

    private final String configKey;

    public HostMappingExtImpl(String configKey) {
        this.configKey = configKey;
    }

    @Override
    public String fetchMappedAddress(String host, int port) {
        Map<String, String> config = ConfigQueryUtil.getConfig(configKey);
        String hosts = config.get("map2Localhost");
        if (hosts != null && hosts.contains(host)) {
            return "localhost:80";
        }
        return null;
    }
}
