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

    SOCKS5("socks5代理"),;

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

    public boolean requireEncryption() {
        return this == SSL_HTTP;
    }
}
