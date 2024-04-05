package com.mola.proxy.bridge.core.registry

import com.mola.proxy.bridge.core.entity.ProxyBridge
import spock.lang.Specification

class ProxyBridgeRegistryTest extends Specification {

    static {
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(10000, 20000))
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(10001, 20001))
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(10002, 20002))
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(10003, 20003))
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(10004, 20000))
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(10005, 20000))
    }

    def "FetchBridgeByForwardPort"() {
        expect:
        ProxyBridge pb = ProxyBridgeRegistry.instance().fetchBridgeByForwardPort(forwardPort)
        pb.reversePort == revesePort

        where:
        forwardPort | revesePort
        10000 | 20000
        10001 | 20001
        10002 | 20002
        10003 | 20003
    }
}
