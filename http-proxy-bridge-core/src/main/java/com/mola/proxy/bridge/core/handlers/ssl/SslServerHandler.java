package com.mola.proxy.bridge.core.handlers.ssl;

import com.mola.proxy.bridge.core.server.encryption.SslContextFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: ssl服务端编解码handler
 * @date : 2024-03-30 22:53
 **/
public class SslServerHandler extends SslHandler {

    private static final Logger logger = LoggerFactory.getLogger(SslServerHandler.class);

    public SslServerHandler(SSLEngine engine) {
        super(engine);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ignoreException(ctx.channel(), cause)) {
            return;
        }
        logger.error("SslServerHandler exception happen, channel = " + ctx.channel(), cause);
    }

    public static SslServerHandler create() {
        SSLContext sslContext = SslContextFactory.fetchSSLContext();
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(true);
        SslServerHandler sslServerHandler = new SslServerHandler(engine);
        sslServerHandler.setHandshakeTimeout(120, TimeUnit.SECONDS);
        return sslServerHandler;
    }

    private boolean ignoreException(Channel channel, Throwable cause) {
        if (cause instanceof DecoderException) {
            Throwable rootCause = cause.getCause();
            if (rootCause instanceof SSLException
                    && rootCause.getMessage() != null
                    && (rootCause.getMessage().contains("close_notify")
                    || rootCause.getMessage().contains("handshake alert")
                    || rootCause.getMessage().contains("Received fatal alert")
            )) {
                logger.info("SslServerHandler exception happen, channel = {}, message = {}",
                        channel, rootCause.getMessage());
                return true;
            }
        }
        return false;
    }
}
