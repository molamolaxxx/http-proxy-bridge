package com.mola;

import com.mola.cmd.proxy.client.conf.CmdProxyConf;
import com.mola.cmd.proxy.client.provider.CmdReceiver;
import com.mola.ext.ExtManager;
import com.mola.forward.ForwardProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ForwardStarter {

    private static final Logger log = LoggerFactory.getLogger(ForwardStarter.class);

    public static void main(String[] args) {

        UserIpWhiteListExtImpl userIpWhiteListExt = new UserIpWhiteListExtImpl();
        ExtManager.setUserIpWhiteListExt(userIpWhiteListExt);
        userIpWhiteListExt.start();

        ForwardProxyServer forwardProxyServer = new ForwardProxyServer();
//
//        CmdProxyConf.Receiver.INSTANCE.setListenedSenderAddress(CmdProxyConf.REMOTE_ADDRESS);
//        CmdProxyConf.INSTANCE.setServerPort(43234);
//        CmdReceiver.INSTANCE.register("forward", "1680059511788nQPEXtoolRobot", cmdInvokeParam -> {
//            Map<String, String> resultMap = new HashMap<>();
//            try {
//                String op = cmdInvokeParam.cmdArgs[0];
//                if ("shutdown".equals(op)) {
//                    forwardProxyServer.shutdown();
//                    resultMap.put("result", "关闭http正向代理成功");
//                } else if ("start".equals(op)) {
//                    forwardProxyServer.start(10432, 10433);
//                    resultMap.put("result", "开启http正向代理成功");
//                } else {
//                    resultMap.put("result", "不合法的操作："+op);
//                }
//                return resultMap;
//            } catch (Exception e) {
//                resultMap.put("result", "操作异常" + e.getMessage());
//                log.error("ForwardStarter operate failed", e);
//                return resultMap;
//            }
//        });

        forwardProxyServer.start(10432, 10433);
    }
}