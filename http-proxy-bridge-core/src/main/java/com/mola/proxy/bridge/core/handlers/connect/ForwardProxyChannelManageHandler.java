package com.mola.proxy.bridge.core.handlers.connect;

import com.mola.proxy.bridge.core.entity.ProxyBridge;
import com.mola.proxy.bridge.core.registry.ProxyBridgeRegistry;
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: my-rpc
 * @Description: 连接管理器
 * @date : 2020-09-04 10:50
 **/
@ChannelHandler.Sharable
public class ForwardProxyChannelManageHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(ForwardProxyChannelManageHandler.class);

    private ReverseProxyConnectPool reverseProxyConnectPool = ReverseProxyConnectPool.instance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
        super.channelActive(ctx);
        // 连接成功
        log.debug("[ForwardProxyChannelManageHandler]: onChannelConnect {" + remoteAddress + "}");
        // 提前分配
        ProxyBridgeRegistry bridgeRegistry = ProxyBridgeRegistry.instance();
        int port = RemotingHelper.fetchChannelLocalPort(ctx.channel());
        ProxyBridge proxyBridge = bridgeRegistry.fetchBridgeByForwardPort(port);
        reverseProxyConnectPool.allocate(ctx.channel(), proxyBridge.getReversePort());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
        super.channelInactive(ctx);
        // 连接关闭
        log.debug("[ForwardProxyChannelManageHandler]: onChannelClose {" + remoteAddress + "}");
        closeReverseChannel(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
                // 连接空闲
                log.warn("[ForwardProxyChannelManageHandler]: onChannelIdle {" + remoteAddress + "}");
                RemotingHelper.closeChannel(ctx.channel());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddress = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
        // 连接异常
        RemotingHelper.closeChannel(ctx.channel());
        log.error("[ForwardProxyChannelManageHandler]: onChannelException {" + remoteAddress + "}");
        closeReverseChannel(ctx.channel());
    }

    private void closeReverseChannel(Channel forwardChannel) {
        Channel channel = reverseProxyConnectPool.fetchReverse(forwardChannel);
        if (Objects.isNull(channel)) {
            log.warn("[ForwardProxyChannelManageHandler]: can not fetchReverse, forward =  {" + forwardChannel + "}");
            return;
        }
        if (!channel.isOpen()) {
            log.warn("[ForwardProxyChannelManageHandler]: reverse channel {" + channel + "} has been closed");
            return;
        }
        RemotingHelper.closeChannel(channel);
    }
}
