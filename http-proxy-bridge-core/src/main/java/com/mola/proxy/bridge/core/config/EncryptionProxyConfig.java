package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.utils.AssertUtil;

import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-11 10:34
 **/
public class EncryptionProxyConfig {

    private List<EncryptionServerItemConfig> servers;

    public void validate() {
        AssertUtil.notNull(servers, "encryption.servers must has value");
        AssertUtil.isTrue(servers.size() > 0, "encryption.servers must has value");
        for (EncryptionServerItemConfig server : servers) {
            AssertUtil.notNull(server, "encryption.server must has value");
            server.validate();
        }
    }

    public List<EncryptionServerItemConfig> getServers() {
        return servers;
    }

    public void setServers(List<EncryptionServerItemConfig> servers) {
        this.servers = servers;
    }
}
