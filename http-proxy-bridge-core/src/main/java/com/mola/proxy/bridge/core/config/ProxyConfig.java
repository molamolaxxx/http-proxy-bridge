package com.mola.proxy.bridge.core.config;

import com.mola.proxy.bridge.core.utils.AssertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-02-11 10:41
 **/
public class ProxyConfig {

    private static final Logger log = LoggerFactory.getLogger(ProxyConfig.class);

    private volatile static ProxyConfig proxyConfig;

    private static final String DEFAULT_YML_FILE_PATH = "http-proxy-bridge.yml";

    private ProxyConfig(){}

    private static void load() {
        if (proxyConfig != null) {
            return;
        }
        synchronized (ProxyConfig.class) {
            if (proxyConfig != null) {
                return;
            }
            Properties properties = System.getProperties();
            String filePath = (String) properties.getOrDefault("configFilePath", DEFAULT_YML_FILE_PATH);
            try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
                Yaml yaml = new Yaml();
                proxyConfig = yaml.loadAs(inputStream, ProxyConfig.class);
            } catch (Exception e) {
                log.error("ProxyConfig load exception!", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static EncryptionProxyConfig fetchEncryptionProxyConfig() {
        ProxyConfig.load();
        AssertUtil.notNull(proxyConfig, "proxyConfig is null");
        proxyConfig.encrypt.validate();
        return proxyConfig.encrypt;
    }

    public static ForwardProxyConfig fetchForwardProxyConfig() {
        ProxyConfig.load();
        AssertUtil.notNull(proxyConfig, "proxyConfig is null");
        proxyConfig.forward.validate();
        return proxyConfig.forward;
    }

    public static ReverseProxyConfig fetchReverseProxyConfig() {
        ProxyConfig.load();
        AssertUtil.notNull(proxyConfig, "proxyConfig is null");
        proxyConfig.reverse.validate();
        return proxyConfig.reverse;
    }


    private EncryptionProxyConfig encrypt;

    private ForwardProxyConfig forward;

    private ReverseProxyConfig reverse;

    public EncryptionProxyConfig getEncrypt() {
        return encrypt;
    }

    public void setEncrypt(EncryptionProxyConfig encrypt) {
        this.encrypt = encrypt;
    }

    public ForwardProxyConfig getForward() {
        return forward;
    }

    public void setForward(ForwardProxyConfig forward) {
        this.forward = forward;
    }

    public ReverseProxyConfig getReverse() {
        return reverse;
    }

    public void setReverse(ReverseProxyConfig reverse) {
        this.reverse = reverse;
    }
}
