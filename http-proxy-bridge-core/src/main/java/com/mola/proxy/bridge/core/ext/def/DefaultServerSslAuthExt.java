package com.mola.proxy.bridge.core.ext.def;

import com.mola.proxy.bridge.core.ext.SslAuthExt;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-22 19:27
 **/
public class DefaultServerSslAuthExt implements SslAuthExt {

    private final static String DEFAULT_PASS = "123456";

    @Override
    public String keyStorePath() {
        return "server_ks.jks";
    }

    @Override
    public String keyStorePassword() {
        return DEFAULT_PASS;
    }

    @Override
    public String trustStorePath() {
        return "clientTrust_ks.jks";
    }

    @Override
    public String trustStorePassword() {
        return DEFAULT_PASS;
    }
}
