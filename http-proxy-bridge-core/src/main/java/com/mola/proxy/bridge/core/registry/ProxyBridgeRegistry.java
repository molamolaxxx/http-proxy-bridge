package com.mola.proxy.bridge.core.registry;

import com.mola.proxy.bridge.core.entity.ProxyBridge;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 正向代理和反向代理的绑定关系注册
 * @date : 2023-10-22 08:18
 **/
public class ProxyBridgeRegistry {

    private static ProxyBridgeRegistry instance;

    /**
     * 底层存储，使用线程安全的set
     */
    private final Set<ProxyBridge> bridges = new CopyOnWriteArraySet<>();

    private ProxyBridgeRegistry(){}

    public static ProxyBridgeRegistry instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ProxyBridgeRegistry.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ProxyBridgeRegistry();
            return instance;
        }
    }

    public void register(ProxyBridge proxyBridge) {
        if (bridges.contains(proxyBridge)) {
            throw new RuntimeException("proxy bridge already exist");
        }
        bridges.add(proxyBridge);
    }

    public ProxyBridge fetchBridgeByForwardPort(int port) {
        return bridges.stream().filter(bridge -> bridge.getPort() == port)
                .findAny().orElse(null);
    }
}
