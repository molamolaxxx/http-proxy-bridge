package com.mola.proxy.bridge.core.pool


import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import spock.lang.Specification

class ReverseProxyConnectPoolTest extends Specification {
    void setup() {
    }

    def "Allocate"() {
        given:
        ReverseProxyConnectPool pool = ReverseProxyConnectPool.instance()
        Channel forwardProxyChannel = Mock(Channel)
        Channel reverseProxyChannel = Mock(Channel)
        reverseProxyChannel.isOpen() >> isOpen
        reverseProxyChannel.close() >> Mock(ChannelFuture)

        // mock channel 地址
        SocketAddress reverseAddr = Mock(SocketAddress)
        reverseAddr.toString() >> address
        reverseProxyChannel.localAddress() >> reverseAddr

        // 添加channel到池子
        pool.addChannel(reverseProxyChannel)
        int reversePort = 12345
        if (removeChannel) {
            pool.removeChannel(reverseProxyChannel)
        }

        when:
        Channel reverseChannel = pool.allocate(forwardProxyChannel, reversePort)

        then:
        (reverseChannel == reverseProxyChannel) == result
        pool.shutdown()

        where:
        isOpen | removeChannel | address | result
        true | false |"127.0.0.1:12345" | true
        false|  false |"127.0.0.1:12346" | false
        true | false |"127.0.0.1:12346" | false
        false | false |"127.0.0.1:12345" | false
        true | true |"127.0.0.1:12345" | false
    }


}
