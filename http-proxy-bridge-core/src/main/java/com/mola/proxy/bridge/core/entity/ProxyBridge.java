package com.mola.proxy.bridge.core.entity;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 正向代理和反向代理的绑定关系
 * @date : 2023-10-23 22:10
 **/

public class ProxyBridge {

    /**
     * 正向代理端口
     */
    private int forwardPort;

    /**
     * 反向代理端口
     */
    private int reversePort;

    private ProxyBridge(int forwardPort, int reversePort) {
        this.forwardPort = forwardPort;
        this.reversePort = reversePort;
    }

    public static ProxyBridge build(int forwardPort, int reversePort) {
        return new ProxyBridge(forwardPort, reversePort);
    }

    public int getForwardPort() {
        return forwardPort;
    }

    public void setForwardPort(int forwardPort) {
        this.forwardPort = forwardPort;
    }

    public int getReversePort() {
        return reversePort;
    }

    public void setReversePort(int reversePort) {
        this.reversePort = reversePort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyBridge that = (ProxyBridge) o;
        return forwardPort == that.forwardPort && reversePort == that.reversePort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(forwardPort, reversePort);
    }
}
