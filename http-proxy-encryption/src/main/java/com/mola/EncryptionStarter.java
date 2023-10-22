package com.mola;

import com.mola.ssl.SslEncryptionProxyServer;
import com.mola.utils.LogUtil;

public class EncryptionStarter {
    public static void main(String[] args) {
        LogUtil.debugReject();

        SslEncryptionProxyServer server = new SslEncryptionProxyServer();
        server.start(22222, "120.27.230.24", 20434);
    }
}