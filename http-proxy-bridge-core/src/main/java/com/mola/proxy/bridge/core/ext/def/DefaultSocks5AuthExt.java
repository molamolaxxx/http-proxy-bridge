package com.mola.proxy.bridge.core.ext.def;

import com.mola.proxy.bridge.core.config.Socks5Config;
import com.mola.proxy.bridge.core.ext.Socks5AuthExt;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 默认socks5鉴权扩展
 * @date : 2024-03-31 23:39
 **/
public class DefaultSocks5AuthExt implements Socks5AuthExt {

    private final Socks5Config socks5Config;

    public DefaultSocks5AuthExt(Socks5Config socks5Config) {
        this.socks5Config = socks5Config;
    }

    @Override
    public boolean auth(String username, String password) {
        if (socks5Config == null) {
            return false;
        }
        return Objects.equals(socks5Config.getPasswd(), password)
                && Objects.equals(socks5Config.getUsername(), username);
    }

    @Override
    public boolean requireAuth() {
        if (socks5Config == null) {
            return false;
        }
        return socks5Config.requireAuth();
    }
}
