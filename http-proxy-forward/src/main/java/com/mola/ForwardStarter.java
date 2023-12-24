package com.mola;

import com.mola.enums.ServerTypeEnum;
import com.mola.ext.ExtManager;
import com.mola.ext.Socks5AuthExtImpl;
import com.mola.server.forward.ForwardProxyServer;
import com.mola.utils.ConfigQueryUtil;
import com.mola.utils.LogUtil;

import java.util.Locale;
import java.util.Map;

public class ForwardStarter {

    public static void main(String[] args) {
        LogUtil.debugReject();

        UserIpWhiteListExtImpl userIpWhiteListExt = new UserIpWhiteListExtImpl();
        ExtManager.setUserIpWhiteListExt(userIpWhiteListExt);
        userIpWhiteListExt.start();
        ExtManager.setSocks5AuthExt(new Socks5AuthExtImpl());

        // 启动加密服务
        new Thread(() -> {
            ForwardProxyServer encryptionProxyServer = new ForwardProxyServer();
            encryptionProxyServer.start(20434, 10434, ServerTypeEnum.SSL_HTTP);
        }).start();

        // 启动ssl纯转发代理
        new Thread(() -> {
            ForwardProxyServer encryptionProxyServer = new ForwardProxyServer();
            encryptionProxyServer.start(20435, 10433, ServerTypeEnum.SSL_TRANSFER);
        }).start();

        // 启动普通http代理
        Map<String, String> config = ConfigQueryUtil.getConfig(args);
        int port = Integer.parseInt(config.getOrDefault("port", "20432"));
        int reversePort = Integer.parseInt(config.getOrDefault("reversePort", "10433"));
        ServerTypeEnum serverTypeEnum = ServerTypeEnum.valueOf(config.getOrDefault("type", "HTTP").toUpperCase(Locale.ROOT));
        ForwardProxyServer forwardProxyServer = new ForwardProxyServer();
        forwardProxyServer.start(port, reversePort, serverTypeEnum);
    }
}