package com.mola;

import com.mola.enums.ServerTypeEnum;
import com.mola.ext.ExtManager;
import com.mola.ext.Socks5AuthExtImpl;
import com.mola.forward.ForwardProxyServer;
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

        Map<String, String> config = ConfigQueryUtil.getConfig(args);
        // 配置
        int port = Integer.parseInt(config.getOrDefault("port", "20432"));
        int reversePort = Integer.parseInt(config.getOrDefault("reversePort", "10433"));
        ServerTypeEnum serverTypeEnum = ServerTypeEnum.valueOf(config.getOrDefault("type", "HTTP").toUpperCase(Locale.ROOT));

        // 启动加密服务
        new Thread(() -> {
            ForwardProxyServer encryptionProxyServer = new ForwardProxyServer();
            encryptionProxyServer.start(20434, 10434, ServerTypeEnum.SSL);
        }).start();

        ForwardProxyServer forwardProxyServer = new ForwardProxyServer();
        forwardProxyServer.start(port, reversePort, serverTypeEnum);
    }
}