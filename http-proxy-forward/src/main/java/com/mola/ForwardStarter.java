package com.mola;

import com.mola.ext.ExtManager;
import com.mola.forward.ForwardProxyServer;

public class ForwardStarter {
    public static void main(String[] args) {
        UserIpWhiteListExtImpl userIpWhiteListExt = new UserIpWhiteListExtImpl();
        ExtManager.setUserIpWhiteListExt(userIpWhiteListExt);
        userIpWhiteListExt.start();

        ForwardProxyServer forwardProxyServer = new ForwardProxyServer();
        forwardProxyServer.start(10432, 10433);
    }
}