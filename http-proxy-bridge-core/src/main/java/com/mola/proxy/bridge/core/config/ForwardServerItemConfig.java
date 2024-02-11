package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.enums.ServerTypeEnum;
import com.mola.proxy.bridge.core.utils.AssertUtil;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-10 18:07
 **/
public class ForwardServerItemConfig {

    private Integer port;

    private Integer reversePort;

    private String type = ServerTypeEnum.HTTP.name();

    public void validate() {
        AssertUtil.notNull(port, "forward.server.[].port must has value");
        AssertUtil.notNull(reversePort, "forward.server.[].reversePort must has value");
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getReversePort() {
        return reversePort;
    }

    public void setReversePort(Integer reversePort) {
        this.reversePort = reversePort;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
