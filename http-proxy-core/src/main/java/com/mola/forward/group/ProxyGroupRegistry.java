package com.mola.forward.group;

import com.mola.pool.ReverseProxyConnectPool;

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
public class ProxyGroupRegistry {

    private static ProxyGroupRegistry instance;

    private ProxyGroupRegistry(){}

    public static ProxyGroupRegistry instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ProxyGroupRegistry.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ProxyGroupRegistry();
            return instance;
        }
    }
    private Set<ProxyGroup> groups = new CopyOnWriteArraySet<>();

    public void register(ProxyGroup proxyGroup) {
        if (groups.contains(proxyGroup)) {
            throw new RuntimeException("proxyGroup already exist");
        }
        List<ProxyGroup> same = groups.stream().filter(group -> proxyGroup.getPort() == group.getPort()
                        || proxyGroup.getReversePort() == group.getReversePort())
                .collect(Collectors.toList());
        if (same.size() > 0) {
            throw new RuntimeException("proxyGroup already exist");
        }
        groups.add(proxyGroup);
    }

    public ProxyGroup fetchGroupByForwardPort(int port) {
        return groups.stream().filter(group -> group.getPort() == port)
                .findAny().orElse(null);
    }

    public ProxyGroup fetchGroupByReversePort(int reversePort) {
        return groups.stream().filter(group -> group.getReversePort() == reversePort)
                .findAny().orElse(null);
    }
}
