package com.mola;

import com.mola.cmd.proxy.client.conf.CmdProxyConf;
import com.mola.cmd.proxy.client.provider.CmdReceiver;
import com.mola.enums.ServerTypeEnum;
import com.mola.ext.ExtManager;
import com.mola.forward.ForwardProxyServer;
import com.mola.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ForwardStarter {

    private static final Logger log = LoggerFactory.getLogger(ForwardStarter.class);

    public static void main(String[] args) {
        LogUtil.debugReject();
        UserIpWhiteListExtImpl userIpWhiteListExt = new UserIpWhiteListExtImpl();
        ExtManager.setUserIpWhiteListExt(userIpWhiteListExt);
        userIpWhiteListExt.start();
        ExtManager.setSocks5AuthExt(new Socks5AuthExtImpl());

        ForwardProxyServer forwardProxyServer = new ForwardProxyServer();

        CmdProxyConf.Receiver.INSTANCE.setListenedSenderAddress(CmdProxyConf.REMOTE_ADDRESS);
        CmdProxyConf.INSTANCE.setServerPort(43234);
        CmdReceiver.INSTANCE.register("forwardShutdown", "1680059511788nQPEXtoolRobot", cmdInvokeParam -> {
            Map<String, String> resultMap = new HashMap<>();
            forwardProxyServer.shutdown();
            resultMap.put("result", "操作成功");
            return resultMap;
        });
        CmdReceiver.INSTANCE.register("forwardStart", "1680059511788nQPEXtoolRobot", cmdInvokeParam -> {
            Map<String, String> resultMap = new HashMap<>();
            forwardProxyServer.start(20432, 10433, ServerTypeEnum.SOCKS5);
            resultMap.put("result", "操作成功");
            return resultMap;
        });

        forwardProxyServer.start(20432, 10433, ServerTypeEnum.SOCKS5);
    }
}