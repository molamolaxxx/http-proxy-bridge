package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.utils.AssertUtil;

import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-10 18:01
 **/
public class ForwardProxyConfig {

    private List<ForwardServerItemConfig> servers;

    private String ipWhiteListQueryUrl;

    private String ipInterceptNotifyUrl;

    private Socks5Config socks5;

    public List<ForwardServerItemConfig> getServers() {
        return servers;
    }

    public void setServers(List<ForwardServerItemConfig> servers) {
        this.servers = servers;
    }

    public String getIpWhiteListQueryUrl() {
        return ipWhiteListQueryUrl;
    }

    public void setIpWhiteListQueryUrl(String ipWhiteListQueryUrl) {
        this.ipWhiteListQueryUrl = ipWhiteListQueryUrl;
    }

    public String getIpInterceptNotifyUrl() {
        return ipInterceptNotifyUrl;
    }

    public void setIpInterceptNotifyUrl(String ipInterceptNotifyUrl) {
        this.ipInterceptNotifyUrl = ipInterceptNotifyUrl;
    }

    public void validate() {
        AssertUtil.notNull(servers, "forward.servers must has value");
        AssertUtil.isTrue(servers.size() > 0, "forward.servers must has value");
        for (ForwardServerItemConfig server : servers) {
            AssertUtil.notNull(server, "encryption.server must has value");
            server.validate();
        }
    }

    public Socks5Config getSocks5() {
        return socks5;
    }

    public void setSocks5(Socks5Config socks5) {
        this.socks5 = socks5;
    }
}
