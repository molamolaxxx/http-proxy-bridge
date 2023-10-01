package com.mola.common;

public class ProxyHttpHeader {

    private String host;

    private int port = 80;

    private boolean isConnectMethod;

    public ProxyHttpHeader(String host, int port, boolean isConnectMethod) {
        this.host = host;
        this.port = port;
        this.isConnectMethod = isConnectMethod;
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

    @Override
    public String toString() {
        return "ProxyHttpHeader{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", isConnectMethod=" + isConnectMethod +
                '}';
    }
}
