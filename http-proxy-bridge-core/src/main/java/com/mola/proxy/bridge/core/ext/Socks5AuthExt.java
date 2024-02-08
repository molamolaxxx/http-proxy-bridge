package com.mola.proxy.bridge.core.ext;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-04 12:46
 **/
public interface Socks5AuthExt {

    boolean auth(String username, String password);
}
