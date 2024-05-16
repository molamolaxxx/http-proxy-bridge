package com.mola.proxy.bridge.core.entity;

import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-05-14 23:55
 **/
public class ConnectionRouteRule {

    /**
     * 目标host
     */
    private String remoteHost;

    /**
     * 目标port
     */
    private Integer remotePort;

    /**
     * host通配符规则
     */
    private List<String> hostRules;

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public List<String> getHostRules() {
        return hostRules;
    }

    public void setHostRules(List<String> hostRules) {
        this.hostRules = hostRules;
    }
}
