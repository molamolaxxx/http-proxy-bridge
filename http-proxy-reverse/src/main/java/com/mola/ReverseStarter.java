package com.mola;

import com.mola.cmd.proxy.client.conf.CmdProxyConf;
import com.mola.cmd.proxy.client.provider.CmdReceiver;
import com.mola.enums.ServerTypeEnum;
import com.mola.forward.ForwardProxyServer;
import com.mola.reverse.ReverseProxyServer;
import com.mola.utils.HttpCommonService;
import com.mola.utils.KeyValueParser;
import com.mola.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ReverseStarter {


    private static final Logger log = LoggerFactory.getLogger(ReverseStarter.class);

    public static void main(String[] args) {
        LogUtil.debugReject();
        Map<String, String> config = getConfig(args);
        String host = config.getOrDefault("host", "120.27.230.24");
        int port = Integer.parseInt(config.getOrDefault("port", "10433"));
        int channelNum = Integer.parseInt(config.getOrDefault("channelNum", "128"));



        ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
        reverseProxyServer.start(host, port, channelNum, ServerTypeEnum.SOCKS5);

        CmdProxyConf.Receiver.INSTANCE.setListenedSenderAddress(CmdProxyConf.REMOTE_ADDRESS);
        CmdReceiver.INSTANCE.register("reverse", "1680059511788nQPEXtoolRobot", cmdInvokeParam -> {
            Map<String, String> configNext = getConfig(args);
            String hostNext = configNext.getOrDefault("host", "120.27.230.24");
            int portNext = Integer.parseInt(configNext.getOrDefault("port", "10433"));
            int channelNumNext = Integer.parseInt(configNext.getOrDefault("channelNum", "128"));
            Map<String, String> resultMap = new HashMap<>();
            try {
                String op = cmdInvokeParam.cmdArgs[0];
                if ("shutdown".equals(op)) {
                    reverseProxyServer.shutdown();
                    resultMap.put("result", "关闭http反向代理成功");
                } else if ("start".equals(op)) {
                    reverseProxyServer.start(hostNext, portNext, channelNumNext, ServerTypeEnum.SOCKS5);
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

    private static Map<String, String> getConfig(String[] args) {
        Map<String, String> parseRes = new HashMap<>();
        if (args != null && args.length != 0) {
            String configKey = args[0];
            try {
                String res = HttpCommonService.INSTANCE
                        .get("https://molaspace.xyz:8550/chat/kv/" + configKey, 1000);
                parseRes = KeyValueParser.parse(res);
            } catch (Exception e) {
                log.error("getConfig failed", e);
            }
        }
        return parseRes;
    }
}