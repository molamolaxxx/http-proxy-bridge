package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.entity.EncryptionAuth;
import com.mola.proxy.bridge.core.enums.EncryptionTypeEnum;
import com.mola.proxy.bridge.core.utils.AssertUtil;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-12 15:37
 **/
public class EncryptionServerItemConfig {

    private String remoteHost;

    private Integer remotePort;

    private Integer port = 22222;

    private String appointProxyHeader;

    private String type = EncryptionTypeEnum.TCP.name();

    private EncryptionAuth auth = EncryptionAuth.DEFAULT;

    public void validate() {
        AssertUtil.notNull(remoteHost, "encryption.remoteHost must has value");
        AssertUtil.notNull(remotePort, "encryption.remotePort must has value");
    }

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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAppointProxyHeader() {
        return appointProxyHeader;
    }

    public void setAppointProxyHeader(String appointProxyHeader) {
        this.appointProxyHeader = appointProxyHeader;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EncryptionAuth getAuth() {
        return auth;
    }

    public void setAuth(EncryptionAuth auth) {
        this.auth = auth;
    }
}
