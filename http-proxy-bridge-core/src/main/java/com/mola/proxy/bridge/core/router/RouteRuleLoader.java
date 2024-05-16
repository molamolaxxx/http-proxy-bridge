package com.mola.proxy.bridge.core.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mola.proxy.bridge.core.entity.ConnectionRouteRule;
import com.mola.proxy.bridge.core.utils.HttpCommonService;
import com.mola.proxy.bridge.core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-05-16 22:16
 **/
public class RouteRuleLoader {

    private static final String HTTP_PREFIX = "http";

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteRuleLoader.class);


    private static List<ConnectionRouteRule> fetchConnectionRouteRules(String routeRulePath) {
        if (routeRulePath == null || routeRulePath.length() == 0) {
            throw new RuntimeException("routeRulePath is null");
        }
        if (routeRulePath.startsWith(HTTP_PREFIX)) {
            return fetchRemoteRouteRules(routeRulePath);
        } else {
            return fetchLocalRouteRules(routeRulePath);
        }
    }

    private static List<ConnectionRouteRule> fetchRemoteRouteRules(String httpUrl) {
        try {
            String ruleJson = HttpCommonService.INSTANCE.get(httpUrl, 30000);
            if (ruleJson == null) {
                return Collections.emptyList();
            }
            return JsonUtil.fromJson(ruleJson, new TypeReference<List<ConnectionRouteRule>>() {});
        } catch (Exception e) {
            LOGGER.error("fetchRemoteRouteRules failed, url = {}", httpUrl, e);
            throw new RuntimeException(e);
        }
    }

    private static List<ConnectionRouteRule> fetchLocalRouteRules(String filePath) {
        try {
            String ruleJson = new String(Files.readAllBytes(Paths.get(filePath)));
            return JsonUtil.fromJson(ruleJson, new TypeReference<List<ConnectionRouteRule>>() {});
        } catch (IOException e) {
            LOGGER.error("fetchLocalRouteRules failed, filePath = {}", filePath, e);
            throw new RuntimeException(e);
        }
    }

    public static void loadRule(String filePath) {
        LOGGER.info("fetch rule start");
        List<ConnectionRouteRule> connectionRouteRules = fetchConnectionRouteRules(filePath);
        LOGGER.info("load rule start");
        if (connectionRouteRules != null) {
            ConnectionRouter.instance().buildRouteRules(connectionRouteRules);
        }
        LOGGER.info("load rule end");
    }
}
