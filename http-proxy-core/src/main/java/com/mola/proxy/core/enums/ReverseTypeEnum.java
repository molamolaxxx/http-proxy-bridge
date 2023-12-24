package com.mola.proxy.core.enums;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-12-17 20:06
 **/
public enum ReverseTypeEnum {

    HTTP("http(s)代理"),
    SOCKS5("socks5代理"),;

    private String msg;

    ReverseTypeEnum(String msg){
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
