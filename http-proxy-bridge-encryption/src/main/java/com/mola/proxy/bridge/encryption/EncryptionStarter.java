package com.mola.proxy.bridge.encryption;

import com.mola.proxy.bridge.core.server.encryption.SslEncryptionProxyServer;
import com.mola.proxy.bridge.core.utils.ConfigQueryUtil;
import com.mola.proxy.bridge.core.utils.LogUtil;

import java.util.Map;

public class EncryptionStarter {
    public static void main(String[] args) {
        LogUtil.debugReject();

        // 配置
        Map<String, String> config = ConfigQueryUtil.getConfig(args);
        int port = Integer.parseInt(config.getOrDefault("port", "22222"));
        String host = config.getOrDefault("host", "120.27.230.24");
        int remotePort = Integer.parseInt(config.getOrDefault("remotePort", "20434"));

        SslEncryptionProxyServer server = new SslEncryptionProxyServer();
        server.start(port, host, remotePort);
    }
}