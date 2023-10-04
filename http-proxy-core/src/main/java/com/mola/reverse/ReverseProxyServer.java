package com.mola.reverse;

import com.mola.common.HttpRequestHandler;
import com.mola.common.ReverseProxyChannelManageHandler;
import com.mola.enums.ServerTypeEnum;
import com.mola.forward.ForwardProxyServer;
import com.mola.pool.ReverseProxyConnectPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReverseProxyServer {

    private static final Logger log = LoggerFactory.getLogger(ForwardProxyServer.class);

    private ReverseProxyChannelCreator reverseProxyChannelCreator;

    private ReverseProxyChannelMonitor reverseProxyChannelMonitor;

    private AtomicBoolean start = new AtomicBoolean(false);

    public synchronized void start(String host, int port, int maxChannelNum, ServerTypeEnum type) {
        if (start.get()) {
            return;
        }
        reverseProxyChannelCreator = new ReverseProxyChannelCreator(host, port, type);

        try {
            for (int i = 0; i < maxChannelNum; i++) {
                reverseProxyChannelCreator.createChannel();
            }
            reverseProxyChannelMonitor = new ReverseProxyChannelMonitor(
                    maxChannelNum, reverseProxyChannelCreator);
            reverseProxyChannelMonitor.start();
        }
        catch (Exception e) {
            log.error("ReverseProxyServer start failed!", e);
        }
        start.compareAndSet(false, true);
    }

    public void shutdown() {
        reverseProxyChannelMonitor.shutdown();
        ReverseProxyConnectPool.instance().shutdown();
        System.gc();
        log.info("ReverseProxyServer has been shutdown");
        start.compareAndSet(true, false);
    }
}
