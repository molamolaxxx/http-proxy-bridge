package com.mola.proxy.bridge.core.pool;

import com.mola.proxy.bridge.core.entity.ReverseChannelHandle;
import com.mola.proxy.bridge.core.schedule.EventScheduler;
import com.mola.proxy.bridge.core.utils.ConcurrentHashSetBuilder;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 08:51
 **/
public class ReverseProxyConnectPool {

    private final Set<Channel> reverseProxyChannelSet = ConcurrentHashSetBuilder.build(128);

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyConnectPool.class);

    /**
     * key: 客户端 -> forward 的channel
     * value: forward -> reverse的channel
     * 双端映射
     */
    private final Map<Channel, Channel> doubleEndChannelMap = new ConcurrentHashMap<>();

    private final Map<Channel, ReverseChannelHandle> handleMap = new ConcurrentHashMap<>();

    private final Set<Channel> allocatedReverseChannel = ConcurrentHashSetBuilder.build(128);

    private static ReverseProxyConnectPool instance;

    private ReverseProxyConnectPool() {
        EventScheduler.addEvent("clearReverseProxyConnectPool", 10, this::clearPool);
    }

    public static ReverseProxyConnectPool instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ReverseProxyConnectPool.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ReverseProxyConnectPool();
            return instance;
        }
    }

    private void clearPool() {
        Set<Channel> channelToRemove = new HashSet<>();
        for (Channel channel : reverseProxyChannelSet) {
            if (!channel.isOpen() || !channel.isActive()) {
                channel.close();
                channelToRemove.add(channel);
            }
        }
        channelToRemove.forEach(this::removeChannel);
        log.info("scheduled remove channel, size = " + channelToRemove.size());
    }

    public void addChannel(Channel channel) {
        reverseProxyChannelSet.add(channel);
    }

    /**
     * 移除反向代理连接
     * @param reverseChannel
     */
    public synchronized void removeChannel(Channel reverseChannel) {
        if (reverseChannel.isOpen() || reverseChannel.isActive()) {
            RemotingHelper.closeChannel(reverseChannel);
        }
        reverseProxyChannelSet.remove(reverseChannel);
        List<Channel> invalidateKeys = new ArrayList<>();
        doubleEndChannelMap.forEach((k, allocatedChannel) -> {
            if (Objects.equals(reverseChannel, allocatedChannel)) {
                invalidateKeys.add(k);
            }
        });

        invalidateKeys.forEach(doubleEndChannelMap::remove);
        doubleEndChannelMap.remove(reverseChannel);
        allocatedReverseChannel.remove(reverseChannel);

        // 针对reverse的特殊逻辑
        if (handleMap.containsKey(reverseChannel)) {
            ReverseChannelHandle reverseChannelHandle = handleMap.get(reverseChannel);
            reverseChannelHandle.shutdown();
            handleMap.remove(reverseChannel);
        }
    }

    public Channel fetchReverse(Channel forwardChannel) {
        return doubleEndChannelMap.get(forwardChannel);
    }

    public synchronized Channel fetchForward(Channel reverseChannel) {
        return doubleEndChannelMap.get(reverseChannel);
    }

    public Channel allocate(Channel forwardProxyChannel, int reversePort) {
        if (doubleEndChannelMap.containsKey(forwardProxyChannel)) {
            return doubleEndChannelMap.get(forwardProxyChannel);
        }
        synchronized (this) {
            if (doubleEndChannelMap.containsKey(forwardProxyChannel)) {
                return doubleEndChannelMap.get(forwardProxyChannel);
            }
            List<Channel> channels = reverseProxyChannelSet
                    .stream().filter(ch -> !allocatedReverseChannel.contains(ch) && ch.isOpen())
                    .filter(ch -> RemotingHelper.fetchChannelLocalPort(ch) == reversePort)
                    .collect(Collectors.toList());

            if (channels.size() == 0) {
                return null;
            }
            Random random = new Random();
            Channel reverseChannel =  channels.get(random.nextInt(channels.size()));
            if (reverseChannel == null) {
                log.info("reverseChannel allocate failed, channel is null" +
                        "forward = " + forwardProxyChannel);
                return null;
            }
            // 分配reverseChannel成功
            log.info("reverseChannel allocate success, " +
                    "forward = " + forwardProxyChannel + ", reverse = " + reverseChannel);
            doubleEndChannelMap.put(forwardProxyChannel, reverseChannel);
            doubleEndChannelMap.put(reverseChannel, forwardProxyChannel);
            allocatedReverseChannel.add(reverseChannel);
            return reverseChannel;
        }
    }

    public Set<Channel> getReverseProxyChannels() {
        return reverseProxyChannelSet;
    }

    public Set<Channel> getReverseProxyChannels(int reversePort) {
        return reverseProxyChannelSet.stream().filter(
                channel -> RemotingHelper.fetchChannelLocalPort(channel) ==  reversePort
        ).collect(Collectors.toSet());
    }

    public void clearChannels(int reversePort) {
        int cnt = 0;
        for (Channel channel : reverseProxyChannelSet) {
            if (channel.isOpen() || channel.isActive()
                    || RemotingHelper.fetchChannelLocalPort(channel) != reversePort) {
                continue;
            }
            RemotingHelper.closeChannel(channel);
            cnt++;
        }

        log.info("clearChannels success, cnt = " + cnt);
    }

    public void addReverseChannelHandle(Channel reverseChannel, ReverseChannelHandle handle) {
        handleMap.put(reverseChannel, handle);
    }

    public void shutdown() {
        reverseProxyChannelSet.forEach(Channel::close);
        reverseProxyChannelSet.clear();
        doubleEndChannelMap.forEach((ch1, ch2) -> {
            ch1.close();
            ch2.close();
        });
        doubleEndChannelMap.clear();
        handleMap.forEach((ch1, h1) -> {
            ch1.close();
            h1.shutdown();
        });
        handleMap.clear();
        allocatedReverseChannel.forEach(Channel::close);
        allocatedReverseChannel.clear();
    }
}
