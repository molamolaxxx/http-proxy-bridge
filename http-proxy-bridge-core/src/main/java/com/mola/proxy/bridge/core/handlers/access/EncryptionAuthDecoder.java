package com.mola.proxy.bridge.core.handlers.access;

import com.mola.proxy.bridge.core.entity.EncryptionAuth;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-07-28 21:57
 **/
public class EncryptionAuthDecoder extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(EncryptionAuthInboundHandler.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readerIndex = in.readerIndex();
        int writerIndex = in.writerIndex();
        if (readerIndex + 4 >= writerIndex) {
            log.warn("EncryptionAuthDecoder authFailed! readerIndex + 4 >= writerIndex, close channel");
            RemotingHelper.closeChannel(ctx.channel());
            return;
        }

        // auth length
        int packLength = in.readInt();
        if (packLength > 128) {
            out.add(EncryptionAuth.OUT_OF_LIMIT);
            return;
        }

        // to auth
        byte[] sourceAuthInfoArr = new byte[packLength];
        in.readBytes(sourceAuthInfoArr);

        out.add(new String(sourceAuthInfoArr, StandardCharsets.UTF_8));
    }
}
