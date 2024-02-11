package com.mola.proxy.bridge.reverse;

import com.mola.proxy.bridge.core.config.ProxyConfig;
import com.mola.proxy.bridge.core.config.ReverseProxyConfig;
import com.mola.proxy.bridge.core.enums.ReverseTypeEnum;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.server.reverse.ReverseProxyServer;
import com.mola.proxy.bridge.core.utils.LogUtil;
import com.mola.proxy.bridge.reverse.ext.HostMappingExtImpl;

public class ReverseStarter {

    public static void main(String[] args) {
        LogUtil.debugReject();

        // 加载配置
        ProxyConfig.load();
        ReverseProxyConfig config = ProxyConfig.fetchReverseProxyConfig();

        // 设置代理域名映射
        ExtManager.setHostMappingExt(new HostMappingExtImpl(config.getHostMapping()));

        // 启动服务
        ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
        reverseProxyServer.start(config.getRemoteHost(), config.getRemotePort(),
                config.getChannelNum(), ReverseTypeEnum.valueOf(config.getType()));
    }
}