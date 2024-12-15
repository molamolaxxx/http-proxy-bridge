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
     * 目标地址 host:port
     */
    private List<String> remoteAddressList;

    /**
     * host通配符规则
     */
    private List<String> hostRules;

    public List<String> getHostRules() {
        return hostRules;
    }

    public void setHostRules(List<String> hostRules) {
        this.hostRules = hostRules;
    }

    public List<String> getRemoteAddressList() {
        return remoteAddressList;
    }

    public void setRemoteAddressList(List<String> remoteAddressList) {
        this.remoteAddressList = remoteAddressList;
    }
}
