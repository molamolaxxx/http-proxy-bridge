package com.mola.proxy.bridge.core.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.IllegalReferenceCountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-03 22:39
 **/
public class RemotingHelper {

    private static final Logger logger = LoggerFactory.getLogger(RemotingHelper.class);

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

    public static int fetchChannelLocalPort(final Channel channel) {
        String address = parseChannelLocalAddress(channel);
        String[] split = address.split(":");
        if (split.length < 2) {
            return -1;
        }
        return Integer.parseInt(split[1]);
    }

    public static IpAndPort fetchChannelLocalIpAndPort(final Channel channel) {
        String address = parseChannelLocalAddress(channel);
        String[] split = address.split(":");
        if (split.length < 2) {
            return null;
        }
        return new IpAndPort(split[0], Integer.parseInt(split[1]));
    }

    public static String parseChannelLocalAddress(final Channel channel) {
        if (channel == null) {
            return "";
        }
        final SocketAddress localAddress = channel.localAddress();
        final String addr = (localAddress != null ? localAddress.toString() : "");

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
        channel.close().addListener((ChannelFutureListener) future ->
                logger.info("closeChannel: close the connection to channel[" + channel + "] result: {" + future.isSuccess() + "}"));
    }

    public static void releaseBuf(ByteBuf byteBuf) {
        try {
            byteBuf.release();
        } catch (IllegalReferenceCountException e) {
            logger.warn("channel ByteBuf has been release");
        } catch (Exception e) {
            logger.error("channel ByteBuf release failed", e);
        }
    }

    public static class IpAndPort {
        public String ip;
        public int port;

        public IpAndPort(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public boolean isEffective() {
            return ip != null && ip.length() > 0 && port > 0;
        }

        public boolean isLAN() {
            return ip.startsWith("192.168") || ip.startsWith("127.0.0.1");
        }
    }
}
