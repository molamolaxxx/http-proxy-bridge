package com.mola.reverse;

import com.mola.common.HttpRequestHandler;
import com.mola.common.ReverseProxyChannelManageHandler;
import com.mola.forward.ForwardProxyServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReverseProxyServer {

    private static final Logger log = LoggerFactory.getLogger(ForwardProxyServer.class);

    public void start(String host, int port, int maxChannelNum) {
        ReverseProxyChannelCreator reverseProxyChannelCreator = new ReverseProxyChannelCreator(host, port);

        try {
            for (int i = 0; i < maxChannelNum; i++) {
                reverseProxyChannelCreator.createChannel();
            }
            ReverseProxyChannelMonitor reverseProxyChannelMonitor = new ReverseProxyChannelMonitor(
                    maxChannelNum, reverseProxyChannelCreator);
            reverseProxyChannelMonitor.start();
        }
        catch (Exception e) {
            log.error("ReverseProxyServer start failed!", e);
        }
    }
}
