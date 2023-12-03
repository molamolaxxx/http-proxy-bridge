package com.mola;

import com.mola.enums.EncryptionTypeEnum;
import com.mola.ssl.SslEncryptionProxyServer;
import com.mola.utils.ConfigQueryUtil;
import com.mola.utils.LogUtil;

import java.util.Locale;
import java.util.Map;

public class EncryptionStarter {
    public static void main(String[] args) {
        LogUtil.debugReject();

        Map<String, String> config = ConfigQueryUtil.getConfig(args);
        // 配置
        int port = Integer.parseInt(config.getOrDefault("port", "22222"));
        String host = config.getOrDefault("host", "120.27.230.24");
        int remotePort = Integer.parseInt(config.getOrDefault("remotePort", "20434"));
        EncryptionTypeEnum encryptionTypeEnum = EncryptionTypeEnum.valueOf(
                config.getOrDefault("type", "SSL").toUpperCase(Locale.ROOT));

        SslEncryptionProxyServer server = new SslEncryptionProxyServer();
        server.start(port, host, remotePort,
                encryptionTypeEnum);
    }
}