package com.mola.proxy.bridge.core.entity;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-01-28 00:26
 **/
public class UdpPacket {

    /**
     * 发送方host
     */
    private String senderHost;

    /**
     * 接收方host
     */
    private String receiverHost;

    /**
     * 数据报文
     */
    public byte[] body;

    public static UdpPacket buildFrom(DatagramPacket datagramPacket) {
        UdpPacket udpPacket = new UdpPacket();

        ByteBuf content = datagramPacket.content();
        udpPacket.body  = new byte[content.readableBytes()];
        content.getBytes(0, udpPacket.body);

        InetSocketAddress sender = datagramPacket.sender();
        udpPacket.senderHost = sender.getHostName() + ":" + sender.getPort();
        InetSocketAddress recipient = datagramPacket.recipient();
        udpPacket.receiverHost = recipient.getHostName() + ":" + recipient.getPort();
        return udpPacket;
    }

    public DatagramPacket buildDatagramPacket(ChannelHandlerContext ctx, InetSocketAddress targetAddress) {
        ByteBuf buffer = ctx.alloc().buffer(body.length);
        buffer.writeBytes(body);
        return new DatagramPacket(buffer, targetAddress);
    }

    public byte[] encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(senderHost).append("\t").append(receiverHost);
        byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);

        // |totalLength|headerLength|header|body
        int totalLength = 4 + header.length + body.length;

        ByteBuffer result = ByteBuffer.allocate(4 + totalLength);
        result.putInt(totalLength);
        result.putInt(header.length);
        result.put(header);
        result.put(body);
        return result.array();
    }

    public void decode(final ByteBuffer byteBuffer) {
        int totalLength = byteBuffer.limit();

        int headerLength = byteBuffer.getInt();
        byte[] headerData = new byte[headerLength];
        byteBuffer.get(headerData);

        // parse header
        String header = new String(headerData, StandardCharsets.UTF_8);
        String[] splitHeader = header.split("\t");
        senderHost = splitHeader[0];
        receiverHost = splitHeader[1];

        // parse body
        int bodyLength = totalLength - 4 - headerLength;
        body = new byte[bodyLength];
        byteBuffer.get(body);
    }

    public String getBodyStr() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
