package com.mola.proxy.bridge.core.server.encryption;

import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.SslAuthExt;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-22 08:29
 **/
public class SslContextFactory {

    private static SSLContext sslContext;

    public static SSLContext fetchSSLContext() {
        if (sslContext == null) {
            sslContext = createSSLContext();
        }
        return sslContext;
    }

    private static synchronized SSLContext createSSLContext() {
        if (sslContext != null) {
            return sslContext;
        }
        SslAuthExt sslAuthExt = ExtManager.getSslAuthExt();
        if (sslAuthExt == null) {
            throw new RuntimeException("sslAuthExt can not be null");
        }
        SSLContext sslContext;
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
            sslContext.init(keyManagerFactory.getKeyManagers(),
                    new TrustManager[]{new EmptyTrustManager()}, null);
            SslContextFactory.sslContext = sslContext;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return sslContext;
    }

    public static class EmptyTrustManager extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
