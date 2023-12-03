package com.mola.bridge;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-23 22:10
 **/

public class ProxyBridge {

    /**
     * 正向代理端口
     */
    private int port;

    /**
     * 反向代理端口
     */
    private int reversePort;

    public ProxyBridge(int port, int reversePort) {
        this.port = port;
        this.reversePort = reversePort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
        return port == that.port && reversePort == that.reversePort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, reversePort);
    }
}
