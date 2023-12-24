package com.mola.server.reverse;

import com.mola.enums.ReverseTypeEnum;
import com.mola.server.forward.ForwardProxyServer;
import com.mola.pool.ReverseProxyConnectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReverseProxyServer {

    private static final Logger log = LoggerFactory.getLogger(ForwardProxyServer.class);

    private ReverseProxyChannelCreator reverseProxyChannelCreator;

    private ReverseProxyChannelMonitor reverseProxyChannelMonitor;

    private AtomicBoolean start = new AtomicBoolean(false);

    public synchronized void start(String host, int port, int maxChannelNum, ReverseTypeEnum type) {
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
        log.info("ReverseProxyServer has been shutdown");
        start.compareAndSet(true, false);
    }
}
