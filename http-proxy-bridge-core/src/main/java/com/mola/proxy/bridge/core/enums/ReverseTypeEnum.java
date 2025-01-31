package com.mola.proxy.bridge.core.enums;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-12-17 20:06
 **/
public enum ReverseTypeEnum {

    HTTP("http(s)代理"),

    SSL_HTTP("ssl加密http(s)代理"),

    SOCKS5("socks5代理"),

    SSL_SOCKS5("ssl加密socks5代理"),

    SSL_UDP("ssl加密udp代理"),
    ;

    private String msg;

    ReverseTypeEnum(String msg){
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public boolean isHttpProxy() {
        return this == HTTP || this == SSL_HTTP;
    }

    public boolean isSocks5Proxy() {
        return this == SOCKS5 || this == SSL_SOCKS5;
    }

    public boolean requireEncryption() {
        return this == SSL_HTTP || this == SSL_SOCKS5;
    }

    public boolean isUdp() {
        return this == SSL_UDP;
    }
}
