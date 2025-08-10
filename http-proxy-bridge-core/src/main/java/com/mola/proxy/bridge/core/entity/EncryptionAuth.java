package com.mola.proxy.bridge.core.entity;

import com.mola.proxy.bridge.core.utils.HashUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 加密机 账号密码校验
 * @date : 2025-07-28 22:00
 **/
public class EncryptionAuth {

    public static final String OUT_OF_LIMIT = "OUT_OF_LIMIT";

    public static final EncryptionAuth DEFAULT = new EncryptionAuth("default", "default");

    private String username;

    private String password;

    public EncryptionAuth(){
    }

    public EncryptionAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] generateAuthKeyArr() {
        return generateAuthKey().getBytes(StandardCharsets.UTF_8);
    }

    public String generateAuthKey() {
        String pwdEncryption = Integer.toHexString(HashUtil.getHash(password));
        String allEncryption = Integer.toHexString(HashUtil.getHash(String.format("%s:%s", username, pwdEncryption)));
        return username.concat(pwdEncryption).concat(allEncryption);
    }

    public boolean match(String authKey) {
        return Objects.equals(authKey, generateAuthKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptionAuth that = (EncryptionAuth) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
