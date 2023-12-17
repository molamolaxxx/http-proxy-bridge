package com.mola.enums;


/**
 * @author : molamola
 * @Project: molachat
 * @Description: chatter评分枚举
 * @date : 2020-05-01 12:12
 **/
public enum ServerTypeEnum {

    HTTP("http(s)代理"),
    SOCKS5("socks5代理"),

    SSL_HTTP("加密机代理"),

    SSL_SOCKS5("ssl+socks5代理"),

    SSL_TRANSFER("ssl转发到反向代理"),
    ;

    private String msg;

    ServerTypeEnum(String msg){
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
