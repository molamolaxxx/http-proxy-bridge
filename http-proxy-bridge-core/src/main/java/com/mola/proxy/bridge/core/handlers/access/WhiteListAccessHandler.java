package com.mola.proxy.bridge.core.handlers.access;

import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.UserIpWhiteListExt;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 白名单拦截器
 * @date : 2023-09-30 15:33
 **/

@ChannelHandler.Sharable
public class WhiteListAccessHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhiteListAccessHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RemotingHelper.HostAndPort hostAndPort = RemotingHelper.fetchChannelIpAndPort(ctx.channel());
        if (hostAndPort == null || !hostAndPort.isEffective()) {
            return;
        }
        UserIpWhiteListExt userIpWhiteListExt = ExtManager.getUserIpWhiteListExt();
        if (userIpWhiteListExt == null || !userIpWhiteListExt.requireWhiteListsVerify(hostAndPort.port)) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (hostAndPort.isLAN()) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (userIpWhiteListExt.ipWhiteList() != null && userIpWhiteListExt.ipWhiteList()
                .contains(hostAndPort.host)) {
            ctx.fireChannelRead(msg);
            return;
        }
        log.info("http proxy request has been intercept! address = " + hostAndPort.host);
        userIpWhiteListExt.interceptNotify(hostAndPort.host);
    }
}
