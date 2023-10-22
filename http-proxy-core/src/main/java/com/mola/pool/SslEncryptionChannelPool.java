package com.mola.pool;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-22 21:03
 **/
public class SslEncryptionChannelPool {

    private static SslEncryptionChannelPool instance;

    private Map<Channel, Channel> channelMap = new ConcurrentHashMap<>(16);

    private SslEncryptionChannelPool(){}

    public static SslEncryptionChannelPool instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SslEncryptionChannelPool.class) {
            if (instance != null) {
                return instance;
            }
            instance = new SslEncryptionChannelPool();
            return instance;
        }
    }

    public void mapping(Channel client2EncrytionChannel, Channel encrytion2ServerChannel) {
        channelMap.put(client2EncrytionChannel, encrytion2ServerChannel);
    }

    public Channel fetchAnother(Channel channel) {
        return channelMap.get(channel);
    }

    public void removeBothEnd(Channel channel) {
        channelMap.forEach(
                (k,v) -> {
                    if (Objects.equals(k, channel) || Objects.equals(v, channel)) {
                        channelMap.remove(k);
                    }
                }
        );
    }
}
