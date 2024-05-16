package com.mola.proxy.bridge.core.router;

import com.mola.proxy.bridge.core.entity.ConnectionRouteRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-05-15 00:04
 **/
public class ConnectionRouter {

    private Map<HostTrieTree, ConnectionRouteRule> connectionRouteRuleMap = new ConcurrentHashMap<>();

    private static ConnectionRouter instance;

    private ConnectionRouter(){}

    public static ConnectionRouter instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ConnectionRouter.class) {
            if (instance != null) {
                return instance;
            }
            instance = new ConnectionRouter();
            return instance;
        }
    }

    /**
     * 通过host匹配规则
     * @param host
     * @return
     */
    public ConnectionRouteRule match(String host) {
        if (host == null) {
            return null;
        }
        for (HostTrieTree hostTrieTree : connectionRouteRuleMap.keySet()) {
            if (hostTrieTree.match(host)) {
                return connectionRouteRuleMap.get(hostTrieTree);
            }
        }
        return null;
    }

    /**
     * 构建规则匹配树
     * @param routeRuleList
     */
    public void buildRouteRules(List<ConnectionRouteRule> routeRuleList) {
        if (routeRuleList == null || routeRuleList.size() == 0) {
            return;
        }
        for (ConnectionRouteRule connectionRouteRule : routeRuleList) {
            if (connectionRouteRule.getHostRules() == null) {
                continue;
            }
            HostTrieTree root = new HostTrieTree();
            for (String hostRule : connectionRouteRule.getHostRules()) {
                root.consume(hostRule);
            }
            connectionRouteRuleMap.put(root, connectionRouteRule);
        }
    }

    /**
     * host字典树
     */
    static class HostTrieTree {

        private final static HostTrieTree END = new HostTrieTree();

        Map<String, HostTrieTree> childNodeMap = new HashMap<>();

        /**
         *
         * @param hostRule
         */
        public void consume(String hostRule) {
            if (hostRule == null || hostRule.trim().length() == 0) {
                return;
            }
            String[] hostParts = hostRule.split("\\.");
            if (hostParts.length == 1) {
                childNodeMap.put(hostParts[0], END);
                return;
            }
            String lastPart = hostParts[hostParts.length - 1];
            if (lastPart.length() == 0) {
                throw new IllegalArgumentException("hostRule Illegal, lastPart is empty");
            }
            // 递归构建子节点
            String childHostRule = String.join(".",
                    Arrays.copyOfRange(hostParts, 0, hostParts.length - 1));
            if (childNodeMap.containsKey(lastPart)) {
                childNodeMap.get(lastPart).consume(childHostRule);
            } else {
                HostTrieTree child = new HostTrieTree();
                child.consume(childHostRule);
                childNodeMap.put(lastPart, child);
            }
        }

        /**
         * 规则是否包含host
         * @param host
         * @return
         */
        public boolean match(String host) {
            if (host == null || host.trim().length() == 0) {
                return false;
            }
            // 如果已经匹配到最后一个*，无论如何都是通过的
            if (childNodeMap.get("*") == END) {
                return true;
            }
            String[] hostParts = host.split("\\.");
            // 出口1：匹配最后一个串
            if (hostParts.length == 1) {
                return END == childNodeMap.get(hostParts[0]);
            }
            // 如果当前包含*规则，则可以匹配1个 到 hostParts.length - 1个
            if (childNodeMap.containsKey("*")) {
                for (int i = 1 ;; i++) {
                    String childHost = String.join(".",
                            Arrays.copyOfRange(hostParts, 0, hostParts.length - i));
                    if (childHost.length() == 0) {
                        break;
                    }
                    if (childNodeMap.get("*").match(childHost)) {
                        return true;
                    }
                }
            }
            // 如果当前不包含*规则，只能匹配一个
            String lastPart = hostParts[hostParts.length - 1];
            if (childNodeMap.containsKey(lastPart)) {
                String childHost = String.join(".",
                        Arrays.copyOfRange(hostParts, 0, hostParts.length - 1));
                return childNodeMap.get(lastPart).match(childHost);
            }
            return false;
        }
    }
}
