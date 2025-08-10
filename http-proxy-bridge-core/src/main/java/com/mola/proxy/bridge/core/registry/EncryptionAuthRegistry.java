package com.mola.proxy.bridge.core.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mola.proxy.bridge.core.entity.EncryptionAuth;
import com.mola.proxy.bridge.core.utils.HttpCommonService;
import com.mola.proxy.bridge.core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-07-28 23:05
 **/
public class EncryptionAuthRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionAuthRegistry.class);

    private static Map<String, EncryptionAuth> localAuthMap = new HashMap<>();

    private static EncryptionAuthRegistry instance;

    private EncryptionAuthRegistry() {}

    public static EncryptionAuthRegistry instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (EncryptionAuthRegistry.class) {
            if (instance != null) {
                return instance;
            }
            instance = new EncryptionAuthRegistry();
            return instance;
        }
    }

    public void register(String serverId, EncryptionAuth encryptionAuth) {
        String authKey = encryptionAuth.generateAuthKey();
        if (localAuthMap.containsKey(authKey)) {
            throw new RuntimeException("already register auth named " + authKey);
        }
        localAuthMap.put(String.format("%s-%s", serverId, authKey), encryptionAuth);
    }

    public void register(String serverId, String path) {
        if (path == null || path.length() == 0) {
            register(serverId, EncryptionAuth.DEFAULT);
            return;
        }
        LOGGER.info("fetch auth config start");
        fetchEncryptionAuth(path).forEach(e -> register(serverId, e));
        LOGGER.info("fetch auth config end");
    }

    public EncryptionAuth query(String serverId, String authKey) {
        return localAuthMap.get(String.format("%s-%s", serverId, authKey));
    }

    private static List<EncryptionAuth> fetchEncryptionAuth(String path) {
        if (path == null || path.length() == 0) {
            return Collections.emptyList();
        }
        if (path.startsWith("http")) {
            return fetchRemoteEncryptionAuth(path);
        } else {
            return fetchLocalEncryptionAuth(path);
        }
    }

    private static List<EncryptionAuth> fetchRemoteEncryptionAuth(String httpUrl) {
        try {
            String ruleJson = HttpCommonService.INSTANCE.get(httpUrl, 30000);
            if (ruleJson == null) {
                return Collections.emptyList();
            }
            return JsonUtil.fromJson(ruleJson, new TypeReference<List<EncryptionAuth>>() {});
        } catch (Exception e) {
            LOGGER.error("fetchRemoteEncryptionAuth failed, url = {}", httpUrl, e);
            throw new RuntimeException(e);
        }
    }

    private static List<EncryptionAuth> fetchLocalEncryptionAuth(String filePath) {
        try {
            String ruleJson = new String(Files.readAllBytes(Paths.get(filePath)));
            return JsonUtil.fromJson(ruleJson, new TypeReference<List<EncryptionAuth>>() {});
        } catch (IOException e) {
            LOGGER.error("fetchLocalEncryptionAuth failed, filePath = {}", filePath, e);
            throw new RuntimeException(e);
        }
    }
}
