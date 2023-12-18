package com.mola.bridge;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-22 08:18
 **/
public class ProxyBridgeRegistry {

    private static ProxyBridgeRegistry instance;

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
    private Set<ProxyBridge> bridges = new CopyOnWriteArraySet<>();

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
