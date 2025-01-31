package com.mola.proxy.bridge.core.handlers.udp;

import com.mola.proxy.bridge.core.entity.UdpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-01-28 00:26
 **/
public class UdpPacketDecoder extends LengthFieldBasedFrameDecoder {

    public UdpPacketDecoder() {
        // 长度域起始下标：0，长度4byte（32位），用于分包
        super(Integer.MAX_VALUE, 0, 4, 0, 4);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        try {
            if (frame == null) {
                return null;
            }

            UdpPacket udpPacket = new UdpPacket();
            udpPacket.decode(frame.nioBuffer());
            return udpPacket;
        } finally {
            if (frame != null) {
                frame.release();
            }
        }

    }
}
