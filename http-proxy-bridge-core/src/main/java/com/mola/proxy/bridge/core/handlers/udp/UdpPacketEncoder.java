package com.mola.proxy.bridge.core.handlers.udp;

import com.mola.proxy.bridge.core.entity.UdpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-01-28 00:26
 **/
public class UdpPacketEncoder extends MessageToByteEncoder<UdpPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, UdpPacket msg, ByteBuf out) throws Exception {
        out.writeBytes(msg.encode());
    }
}
