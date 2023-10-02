package com.mola;

import com.mola.cmd.proxy.client.conf.CmdProxyConf;
import com.mola.cmd.proxy.client.provider.CmdReceiver;
import com.mola.forward.ForwardProxyServer;
import com.mola.reverse.ReverseProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ReverseStarter {


    private static final Logger log = LoggerFactory.getLogger(ReverseStarter.class);

    public static void main(String[] args) {
        ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
        reverseProxyServer.start("120.27.230.24", 10433, 128);

        CmdProxyConf.Receiver.INSTANCE.setListenedSenderAddress(CmdProxyConf.REMOTE_ADDRESS);
        CmdReceiver.INSTANCE.register("reverse", "1680059511788nQPEXtoolRobot", cmdInvokeParam -> {
            Map<String, String> resultMap = new HashMap<>();
            try {
                String op = cmdInvokeParam.cmdArgs[0];
                if ("shutdown".equals(op)) {
                    reverseProxyServer.shutdown();
                    resultMap.put("result", "关闭http反向代理成功");
                } else if ("start".equals(op)) {
                    reverseProxyServer.start("120.27.230.24", 10433, 128);
                    resultMap.put("result", "开启http反向代理成功");
                } else {
                    resultMap.put("result", "不合法的操作："+op);
                }
                return resultMap;
            } catch (Exception e) {
                resultMap.put("result", "操作异常" + e.getMessage());
                log.error("ReverseStarter operate failed", e);
                return resultMap;
            }
        });
    }
}