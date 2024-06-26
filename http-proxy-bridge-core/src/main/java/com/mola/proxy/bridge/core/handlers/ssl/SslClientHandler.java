package com.mola.proxy.bridge.core.handlers.ssl;

import com.mola.proxy.bridge.core.server.encryption.SslContextFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.net.SocketException;

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
        if (ignoreException(ctx.channel(), cause)) {
            return;
        }
        logger.error("SslClientHandler exception happen, channel = " + ctx.channel(), cause);
    }

    public static SslClientHandler create() {
        SSLContext sslContext = SslContextFactory.fetchSSLContext();
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(true);
        return new SslClientHandler(engine);
    }

    private boolean ignoreException(Channel channel, Throwable cause) {
        if (cause instanceof DecoderException) {
            Throwable rootCause = cause.getCause();
            if (rootCause instanceof SSLException
                    && rootCause.getMessage() != null
                    || rootCause.getMessage().contains("handshake alert")
                    || rootCause.getMessage().contains("Received fatal alert")
                    || rootCause.getMessage().contains("Insufficient buffer")
            ) {
                logger.warn("SslClientHandler exception happen, channel = {}, message = {}",
                        channel, rootCause.getMessage());
                return true;
            }
        }
        return false;
    }
}
