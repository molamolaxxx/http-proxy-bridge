package com.mola.proxy.bridge.core.server.reverse;

import com.mola.proxy.bridge.core.config.ReverseServerItemConfig;
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReverseProxyServer {

    private String serverId = UUID.randomUUID().toString();

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyServer.class);

    private ReverseProxyChannelCreator reverseProxyChannelCreator;

    private ReverseProxyChannelMonitor reverseProxyChannelMonitor;

    private final AtomicBoolean start = new AtomicBoolean(false);

    public synchronized void start(ReverseServerItemConfig config) {
        if (start.get()) {
            return;
        }
        reverseProxyChannelCreator = new ReverseProxyChannelCreator(serverId, config);
        try {
            for (int i = 0; i < config.getChannelNum(); i++) {
                reverseProxyChannelCreator.createChannel();
            }
            reverseProxyChannelMonitor = new ReverseProxyChannelMonitor(
                    config.getChannelNum(), reverseProxyChannelCreator
            );
            log.info("ReverseProxyServer start success! remoteHost = {}, remotePort = {}",
                    config.getRemoteHost(), config.getRemotePort());
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

    public String getServerId() {
        return serverId;
    }
}
