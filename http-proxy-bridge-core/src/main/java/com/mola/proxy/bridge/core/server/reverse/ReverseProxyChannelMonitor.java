package com.mola.proxy.bridge.core.server.reverse;

import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 14:54
 **/
public class ReverseProxyChannelMonitor extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelMonitor.class);

    private boolean shutdown;

    private final int maxChannelNum;

    private final ReverseProxyChannelCreator channelCreator;

    public ReverseProxyChannelMonitor(int maxChannelNum, ReverseProxyChannelCreator channelCreator) {
        this.maxChannelNum = maxChannelNum;
        this.channelCreator = channelCreator;
    }

    @Override
    public void run() {
        while (true) {
            if (shutdown) {
                return;
            }
            try {
                ReverseProxyConnectPool connectPool = ReverseProxyConnectPool.instance();
                Set<Channel> reverseProxyChannels = connectPool.getReverseProxyChannels()
                        .stream().filter(ch -> {
                            RemotingHelper.HostAndPort hostAndPort = RemotingHelper.fetchRemoteChannelIpAndPort(ch);
                            if (hostAndPort != null
                                    && Objects.equals(hostAndPort.host, channelCreator.getHost())
                                    && Objects.equals(hostAndPort.port, channelCreator.getPort())) {
                                return true;
                            }
                            return false;
                        }).collect(Collectors.toSet());

                log.info("[ReverseProxyChannelMonitor] activate reverse channel :" + reverseProxyChannels.size());

                if (reverseProxyChannels.size() < maxChannelNum) {
                    // 待创建的channel数量 >= 1
                    int channelToCreateNum = maxChannelNum - reverseProxyChannels.size();
                    // 先同步创建一个连接
                    Channel channel = channelCreator.createChannel();
                    if (channel == null) {
                        log.info("[ReverseProxyChannelMonitor] try create one channel failed, will not create others");
                        waitForNextBatch();
                        continue;
                    }
                    List<Future<Channel>> futureList = new ArrayList<>();
                    for (int i = 0; i < channelToCreateNum - 1; i++) {
                        Future<Channel> future = channelCreator.createChannelAsync();
                        futureList.add(future);
                    }
                    for (Future<Channel> future : futureList) {
                        future.get();
                    }
                    log.info("[ReverseProxyChannelMonitor] create channel num :" + (maxChannelNum - reverseProxyChannels.size()));
                }

                waitForNextBatch();
            } catch (Exception e) {
                log.error("createChannel exception", e);
            }
        }
    }

    private static void waitForNextBatch() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignore) {
        }
    }

    public void shutdown() {
        this.shutdown = true;
    }
}
