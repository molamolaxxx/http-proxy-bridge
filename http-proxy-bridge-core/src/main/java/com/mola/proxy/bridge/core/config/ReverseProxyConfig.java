package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.enums.ReverseTypeEnum;
import com.mola.proxy.bridge.core.utils.AssertUtil;

import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-11 10:50
 **/
public class ReverseProxyConfig {

    private String remoteHost;

    private Integer remotePort;

    private Integer channelNum = 128;

    private String type = ReverseTypeEnum.HTTP.name();

    public void validate() {
        AssertUtil.notNull(remoteHost, "reverse.remoteHost must has value");
        AssertUtil.notNull(remotePort, "reverse.remotePort must has value");
    }

    private Map<String, String> hostMapping;

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

    public Integer getChannelNum() {
        return channelNum;
    }

    public void setChannelNum(Integer channelNum) {
        this.channelNum = channelNum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getHostMapping() {
        return hostMapping;
    }

    public void setHostMapping(Map<String, String> hostMapping) {
        this.hostMapping = hostMapping;
    }
}
