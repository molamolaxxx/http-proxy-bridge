package com.mola.proxy.bridge.encryption;

import com.mola.proxy.bridge.core.config.EncryptionProxyConfig;
import com.mola.proxy.bridge.core.config.ProxyConfig;
import com.mola.proxy.bridge.core.server.encryption.SslEncryptionProxyServer;
import com.mola.proxy.bridge.core.utils.LogUtil;

public class EncryptionStarter {
    public static void main(String[] args) {
        LogUtil.debugReject();

        // 读取配置
        ProxyConfig.load();
        EncryptionProxyConfig config = ProxyConfig.fetchEncryptionProxyConfig();

        // 启动服务
        SslEncryptionProxyServer server = new SslEncryptionProxyServer();
        server.start(config.getPort(), config.getRemoteHost(), config.getRemotePort());
    }
}