package com.mola.proxy.forward.ext;

import com.mola.proxy.core.ext.Socks5AuthExt;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-04 13:15
 **/
public class Socks5AuthExtImpl implements Socks5AuthExt {

    @Override
    public boolean auth(String username, String password) {
        if ("mola".equalsIgnoreCase(username) && "mola".equals(password)) {
            return true;
        }
        return false;
    }
}
