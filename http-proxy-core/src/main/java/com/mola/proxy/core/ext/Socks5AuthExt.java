package com.mola.proxy.core.ext;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-04 12:46
 **/
public interface Socks5AuthExt {

    boolean auth(String username, String password);
}
