package com.mola.proxy.bridge.reverse;

import com.mola.proxy.bridge.core.config.ProxyConfig;
import com.mola.proxy.bridge.core.config.ReverseProxyConfig;
import com.mola.proxy.bridge.core.config.ReverseServerItemConfig;
import com.mola.proxy.bridge.core.enums.ReverseTypeEnum;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.def.DefaultSocks5AuthExt;
import com.mola.proxy.bridge.core.server.reverse.ReverseProxyServer;
import com.mola.proxy.bridge.core.utils.LogUtil;
import com.mola.proxy.bridge.reverse.ext.HostMappingExtImpl;

public class ReverseStarter {

    public static void main(String[] args) {
        LogUtil.debugReject();

        // 加载配置
        ReverseProxyConfig config = ProxyConfig.fetchReverseProxyConfig();

        // 设置代理域名映射
        ExtManager.setHostMappingExt(new HostMappingExtImpl(config.getHostMapping()));

        // 设置socks5配置
        ExtManager.setSocks5AuthExt(new DefaultSocks5AuthExt(config.getSocks5()));

        // 异步启动服务
        Thread serverThread = null;
        for (ReverseServerItemConfig server : config.getServers()) {
            serverThread = new Thread(() -> {
                ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
                reverseProxyServer.start(server.getRemoteHost(), server.getRemotePort(),
                        server.getChannelNum(), ReverseTypeEnum.valueOf(server.getType()));
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