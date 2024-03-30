package com.mola.proxy.bridge.core.handlers.ssl;

import com.mola.proxy.bridge.core.server.encryption.SslContextFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: ssl客户端编解码handler
 * @date : 2024-03-30 22:51
 **/
public class SslClientHandler extends SslHandler {

    private static final Logger logger = LoggerFactory.getLogger(SslClientHandler.class);

    public SslClientHandler(SSLEngine engine) {
        super(engine);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("SslClientHandler exception happen, channel = " + ctx.channel(), cause);
    }

    public static SslClientHandler create() {
        SSLContext sslContext = SslContextFactory.createSSLContext();
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(true);
        return new SslClientHandler(engine);
    }
}
