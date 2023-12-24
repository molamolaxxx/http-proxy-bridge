package com.mola.proxy.core.handlers.socks5;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class Socks5InitialRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    private static final Logger log = LoggerFactory.getLogger(Socks5InitialRequestInboundHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) {
        boolean failure = msg.decoderResult().isFailure();
        if (failure) {
            log.error("init socks5 failed，please check!");
            ReferenceCountUtil.retain(msg);
            ctx.fireChannelRead(msg);
            return;
        }

        // 鉴权
        Socks5InitialResponse socks5InitialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
        ctx.writeAndFlush(socks5InitialResponse);
        ctx.pipeline().remove(this);
        ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
    }
}
