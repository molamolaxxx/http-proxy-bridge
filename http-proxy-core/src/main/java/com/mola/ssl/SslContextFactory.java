package com.mola.ssl;

import com.mola.ext.ExtManager;
import com.mola.ext.SslAuthExt;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-22 08:29
 **/
public class SslContextFactory {

    private static SSLContext createSSLContext()  {
        SslAuthExt sslAuthExt = ExtManager.getSslAuthExt();
        if (sslAuthExt == null) {
            throw new RuntimeException("sslAuthExt can not be null");
        }
        SSLContext sslContext = null;
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream keyStoreInput = Files.newInputStream(Paths.get(sslAuthExt.keyStorePath()))) {
                keyStore.load(keyStoreInput, sslAuthExt.keyStorePassword().toCharArray());
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, sslAuthExt.keyStorePassword().toCharArray());

            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreInput = Files.newInputStream(Paths.get(sslAuthExt.trustStorePath()))) {
                trustStore.load(trustStoreInput, sslAuthExt.trustStorePassword().toCharArray());
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return sslContext;
    }

    public static SslHandler createSslHandler(boolean isClient) {
        SSLContext sslContext = createSSLContext();
        SSLEngine engine = sslContext.createSSLEngine();
        if (isClient) {
            engine.setUseClientMode(true);
        } else {
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(true);
        }
        return new SslHandler(engine);
    }
}
