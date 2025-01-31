package com.mola.proxy.bridge.core.enums;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-12-17 20:06
 **/
public enum EncryptionTypeEnum {

    TCP("tcp协议代理"),

    UDP("udp协议代理"),;

    private String msg;

    EncryptionTypeEnum(String msg){
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
