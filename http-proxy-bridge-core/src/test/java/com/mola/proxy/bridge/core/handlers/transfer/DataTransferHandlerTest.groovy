package com.mola.proxy.bridge.core.handlers.transfer

import com.mola.proxy.bridge.core.entity.ProxyBridge
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool
import com.mola.proxy.bridge.core.registry.ProxyBridgeRegistry
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

class DataTransferHandlerTest extends Specification {
    void setup() {
    }

    void cleanup() {
    }

    def "ChannelRead"() {
        given:
        ReverseProxyConnectPool pool = ReverseProxyConnectPool.instance()
        Channel forwardProxyChannel = Mock(Channel)
        Channel reverseProxyChannel = Mock(Channel)
        reverseProxyChannel.isOpen() >> true
        reverseProxyChannel.close() >> Mock(ChannelFuture)

        ProxyBridgeRegistry.instance().register(ProxyBridge.build(23232, 12345))

        // mock channel 地址
        SocketAddress reverseAddr = Mock(SocketAddress)
        reverseAddr.toString() >> "127.0.0.1:12345"
        reverseProxyChannel.localAddress() >> reverseAddr

        SocketAddress forwardAddr = Mock(SocketAddress)
        forwardAddr.toString() >> "127.0.0.1:23232"
        forwardProxyChannel.localAddress() >> forwardAddr

        // 添加channel到池子
        pool.addChannel(reverseProxyChannel)

        DataTransferHandler dataTransferHandler = new DataTransferHandler();
        ChannelHandlerContext ctx = Mock(ChannelHandlerContext)
        ctx.channel() >> forwardProxyChannel

        when:
        def msg = new Object()
        dataTransferHandler.channelRead(ctx, msg)

        then:
        1 * reverseProxyChannel.writeAndFlush(_)
    }
}
