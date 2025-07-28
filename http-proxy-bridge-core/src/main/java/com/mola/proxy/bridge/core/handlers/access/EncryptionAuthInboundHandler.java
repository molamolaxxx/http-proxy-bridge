package com.mola.proxy.bridge.core.handlers.access;

import com.mola.proxy.bridge.core.entity.EncryptionAuth;
import com.mola.proxy.bridge.core.registry.EncryptionAuthRegistry;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-07-28 21:57
 **/
public class EncryptionAuthInboundHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptionAuthInboundHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String sourceAuthKey) throws Exception {
        if (EncryptionAuth.OUT_OF_LIMIT.equals(sourceAuthKey)) {
            RemotingHelper.closeChannel(ctx.channel());
            return;
        }
        EncryptionAuth queryResult = EncryptionAuthRegistry.instance().query(sourceAuthKey);
        if (queryResult != null && Objects.equals(sourceAuthKey, queryResult.generateAuthKey())) {
            authSuccess(ctx);
        } else {
            authFailed(ctx, sourceAuthKey);
        }
    }

    private void authFailed(ChannelHandlerContext ctx, String sourceAuthKey) {
        log.warn("EncryptionAuthDecoder authFailed! sourceAuthKey = {}", sourceAuthKey);
        RemotingHelper.closeChannel(ctx.channel());
    }

    private void authSuccess(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
        ctx.pipeline().remove(EncryptionAuthDecoder.class);
    }
}
