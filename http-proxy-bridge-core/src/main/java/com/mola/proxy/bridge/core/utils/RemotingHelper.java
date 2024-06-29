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

    public static HostAndPort fetchChannelIpAndPort(final Channel channel) {
        String localAddress = parseChannelLocalAddress(channel);
        String[] split = localAddress.split(":");
        if (split.length < 2) {
            return null;
        }
        int localPort = Integer.parseInt(split[1]);

        String remoteAddress = parseChannelRemoteAddress(channel);
        split = remoteAddress.split(":");
        if (split.length < 2) {
            return null;
        }
        String remoteHost = split[0];
        return new HostAndPort(remoteHost, localPort);
    }


    public static HostAndPort fetchRemoteChannelIpAndPort(final Channel channel) {
        String remoteAddress = parseChannelRemoteAddress(channel);
        String[] split = remoteAddress.split(":");
        if (split.length < 2) {
            return null;
        }
        String remoteHost = split[0];
        int remotePort = Integer.parseInt(split[1]);
        return new HostAndPort(remoteHost, remotePort);
    }

    public static HostAndPort parseHostAndPort(String hostAndPort) {
        if (hostAndPort == null) {
            return null;
        }
        String[] split = hostAndPort.split(":");
        if (split.length < 2) {
            return null;
        }
        String remoteHost = split[0];
        int remotePort = Integer.parseInt(split[1]);
        return new HostAndPort(remoteHost, remotePort);
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

    public static void releaseBuf(Object message) {
        if (message instanceof ByteBuf) {
            releaseBuf((ByteBuf) message);
        }
    }

    public static class HostAndPort {
        public String host;
        public int port;

        public HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public boolean isEffective() {
            return host != null && host.length() > 0 && port > 0;
        }

        public boolean isLAN() {
            return host.startsWith("192.168") || host.startsWith("127.0.0.1");
        }
    }
}
