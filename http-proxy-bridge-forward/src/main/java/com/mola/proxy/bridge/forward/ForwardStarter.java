package com.mola.proxy.bridge.forward;

import com.mola.proxy.bridge.core.config.ForwardProxyConfig;
import com.mola.proxy.bridge.core.config.ForwardServerItemConfig;
import com.mola.proxy.bridge.core.config.ProxyConfig;
import com.mola.proxy.bridge.core.enums.ServerTypeEnum;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.def.DefaultHostMappingExtImpl;
import com.mola.proxy.bridge.core.ext.def.DefaultSocks5AuthExt;
import com.mola.proxy.bridge.core.server.forward.ForwardProxyServer;
import com.mola.proxy.bridge.core.utils.LogUtil;
import com.mola.proxy.bridge.forward.ext.UserIpWhiteListExtImpl;

public class ForwardStarter {

    public static void main(String[] args) {
        LogUtil.debugReject();

        UserIpWhiteListExtImpl userIpWhiteListExt = new UserIpWhiteListExtImpl();
        ExtManager.setUserIpWhiteListExt(userIpWhiteListExt);
        userIpWhiteListExt.start();

        // 加载配置
        ForwardProxyConfig config = ProxyConfig.fetchForwardProxyConfig();

        // 设置socks5配置
        ExtManager.setSocks5AuthExt(new DefaultSocks5AuthExt(config.getSocks5()));
        // 设置代理域名映射
        ExtManager.setHostMappingExt(new DefaultHostMappingExtImpl(config.getHostMapping()));

        // 异步启动服务
        Thread serverThread = null;
        for (ForwardServerItemConfig server : config.getServers()) {
            serverThread = new Thread(() -> {
                ForwardProxyServer forwardProxyServer = new ForwardProxyServer();
                forwardProxyServer.start(server.getPort(), server.getReversePort(),
                        ServerTypeEnum.valueOf(server.getType()));
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