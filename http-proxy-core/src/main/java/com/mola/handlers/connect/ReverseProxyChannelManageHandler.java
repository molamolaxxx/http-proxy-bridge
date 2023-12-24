package com.mola.handlers.connect;

import com.mola.pool.ReverseProxyConnectPool;
import com.mola.utils.RemotingHelper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: my-rpc
 * @Description: 连接管理器
 * @date : 2020-09-04 10:50
 **/

@ChannelHandler.Sharable
public class ReverseProxyChannelManageHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelManageHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 连接成功
        log.debug("[ReverseProxyChannelManageHandler]: onChannelConnect {" + ctx.channel() + "}");
        ReverseProxyConnectPool.instance().addChannel(ctx.channel());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        // 连接关闭
        log.debug("[ReverseProxyChannelManageHandler]: onChannelClose {" + ctx.channel() + "}");
        ReverseProxyConnectPool.instance().removeChannel(ctx.channel());
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
                // 连接空闲
                log.debug("[ReverseProxyChannelManageHandler]: onChannelIdle {" + remoteAddress + "}");
                ReverseProxyConnectPool.instance().removeChannel(ctx.channel());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 连接异常
        log.warn("[ReverseProxyChannelManageHandler]: onChannelException {" + ctx.channel() + "}");
        ReverseProxyConnectPool.instance().removeChannel(ctx.channel());
    }
}
