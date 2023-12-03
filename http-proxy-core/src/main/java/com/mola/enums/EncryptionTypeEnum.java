package com.mola.enums;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-12-03 14:48
 **/
public enum EncryptionTypeEnum {

    SOCKS5_SSL("socks5+ssl加密"),

    SSL("ssl加密")
    ;

    private String msg;

    EncryptionTypeEnum(String msg){
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
