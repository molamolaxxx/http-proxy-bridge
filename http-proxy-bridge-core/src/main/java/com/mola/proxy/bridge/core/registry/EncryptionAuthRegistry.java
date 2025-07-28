package com.mola.proxy.bridge.core.registry;

import com.mola.proxy.bridge.core.entity.EncryptionAuth;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-07-28 23:05
 **/
public class EncryptionAuthRegistry {

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

    public void register(EncryptionAuth encryptionAuth) {
        String authKey = encryptionAuth.generateAuthKey();
        if (localAuthMap.containsKey(authKey)) {
            throw new RuntimeException("already register auth named " + authKey);
        }
        localAuthMap.put(authKey, encryptionAuth);
    }

    public EncryptionAuth query(String authKey) {
        return localAuthMap.get(authKey);
    }
}
