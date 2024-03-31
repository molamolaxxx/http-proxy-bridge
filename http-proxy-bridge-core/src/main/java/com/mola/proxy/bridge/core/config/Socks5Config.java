package com.mola.proxy.bridge.core.config;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: sock5账号密码验证
 * @date : 2024-03-31 23:28
 **/
public class Socks5Config {

    private String username;

    private String passwd;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public boolean requireAuth() {
        return username != null && passwd != null;
    }
}
