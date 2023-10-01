package com.mola.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 通信层一些辅助方法
 */
public class RemotingHelper {


    private static final Logger logger = LoggerFactory.getLogger(RemotingHelper.class);

    /**
     * IP:PORT
     */
    public static SocketAddress string2SocketAddress(final String addr) {
        String[] s = addr.split(":");
        InetSocketAddress isa = new InetSocketAddress(s[0], Integer.valueOf(s[1]));
        return isa;
    }

    public static String parseChannelRemoteAddress(final Channel channel) {
        if (channel == null) {
            return "";
        }
        final SocketAddress remote = channel.remoteAddress();
        final String addr = (remote != null ? remote.toString() : "");

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }


    public static void closeChannel(Channel channel) {
        final String addrRemote = RemotingHelper.parseChannelRemoteAddress(channel);
        channel.close().addListener((ChannelFutureListener) future ->
                logger.info("closeChannel: close the connection to channel[" + channel.toString() + "] result: {" + future.isSuccess() + "}"));
    }

}
