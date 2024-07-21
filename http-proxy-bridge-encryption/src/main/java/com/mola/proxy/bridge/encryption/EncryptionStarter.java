package com.mola.proxy.bridge.encryption;

import com.mola.proxy.bridge.core.config.EncryptionProxyConfig;
import com.mola.proxy.bridge.core.config.EncryptionServerItemConfig;
import com.mola.proxy.bridge.core.config.ProxyConfig;
import com.mola.proxy.bridge.core.router.RouteRuleLoader;
import com.mola.proxy.bridge.core.server.encryption.SslEncryptionProxyServer;
import com.mola.proxy.bridge.core.utils.LogUtil;

public class EncryptionStarter {
    public static void main(String[] args) {
        LogUtil.debugReject();

        // 读取配置
        EncryptionProxyConfig encryptionProxyConfig = ProxyConfig.fetchEncryptionProxyConfig();

        // 启动规则加载器
        RouteRuleLoader.loadRule(encryptionProxyConfig.getRouteRule());

        // 启动服务
        Thread serverThread = null;
        for (EncryptionServerItemConfig config : encryptionProxyConfig.getServers()) {
            serverThread = new Thread(() -> {
                SslEncryptionProxyServer proxyServer = new SslEncryptionProxyServer();
                proxyServer.start(config);
            });
            serverThread.start();
        }

        if (serverThread == null) {
            return;
        }

        try {
            serverThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}