package com.mola.enums;


/**
 * @author : molamola
 * @Project: molachat
 * @Description: chatter评分枚举
 * @date : 2020-05-01 12:12
 **/
public enum ServerTypeEnum {

    HTTP(1,"http(s)代理"),
    SOCKS5(10,"socks5代理")
    ;

    private Integer code;

    private String msg;

    ServerTypeEnum(Integer point , String msg){
        this.code = point;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
