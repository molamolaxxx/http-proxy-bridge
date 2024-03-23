package com.mola.proxy.bridge.core.server.reverse;

import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
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

    private int maxChannelNum;

    private ReverseProxyChannelCreator channelCreator;

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
                    for (int i = 0; i < maxChannelNum - reverseProxyChannels.size(); i++) {
                        channelCreator.createChannel();
                    }
                    log.info("[ReverseProxyChannelMonitor] create channel num :" + (maxChannelNum - reverseProxyChannels.size()));
                }
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("createChannel exception", e);
            }

        }
    }

    public void shutdown() {
        this.shutdown = true;
    }
}
