package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.utils.AssertUtil;

import java.util.List;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-11 10:50
 **/
public class ReverseProxyConfig {

    private List<ReverseServerItemConfig> servers;

    private Socks5Config socks5;

    public void validate() {
        AssertUtil.notNull(servers, "reverse.servers must has value");
        AssertUtil.isTrue(servers.size() > 0, "reverse.servers must has value");
        for (ReverseServerItemConfig server : servers) {
            AssertUtil.notNull(server, "encryption.server must has value");
            server.validate();
        }
    }

    private Map<String, String> hostMapping;

    public List<ReverseServerItemConfig> getServers() {
        return servers;
    }

    public void setServers(List<ReverseServerItemConfig> servers) {
        this.servers = servers;
    }

    public Map<String, String> getHostMapping() {
        return hostMapping;
    }

    public void setHostMapping(Map<String, String> hostMapping) {
        this.hostMapping = hostMapping;
    }

    public Socks5Config getSocks5() {
        return socks5;
    }

    public void setSocks5(Socks5Config socks5) {
        this.socks5 = socks5;
    }
}
