package com.mola.proxy.bridge.core.entity;

public class ProxyHttpHeader {

    private String host;

    private int port = 80;

    private boolean isConnectMethod;

    private boolean appoint;

    public ProxyHttpHeader(String host, int port, boolean isConnectMethod) {
        this.host = host;
        this.port = port;
        this.isConnectMethod = isConnectMethod;
    }

    public ProxyHttpHeader(String host, int port, boolean isConnectMethod, boolean appoint) {
        this.host = host;
        this.port = port;
        this.isConnectMethod = isConnectMethod;
        this.appoint = appoint;
    }

    public String getTargetAddress() {
        return host + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isConnectMethod() {
        return isConnectMethod;
    }

    public void setConnectMethod(boolean connectMethod) {
        isConnectMethod = connectMethod;
    }

    public boolean isAppoint() {
        return appoint;
    }

    public void setAppoint(boolean appoint) {
        this.appoint = appoint;
    }

    @Override
    public String toString() {
        return "ProxyHttpHeader{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", isConnectMethod=" + isConnectMethod +
                '}';
    }
}
