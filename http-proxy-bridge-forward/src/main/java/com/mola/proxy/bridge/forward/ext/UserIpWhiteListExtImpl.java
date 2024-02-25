package com.mola.proxy.bridge.forward.ext;

import com.mola.proxy.bridge.core.config.ForwardProxyConfig;
import com.mola.proxy.bridge.core.config.ProxyConfig;
import com.mola.proxy.bridge.core.ext.UserIpWhiteListExt;
import com.mola.proxy.bridge.core.utils.HttpCommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 20:22
 **/
public class UserIpWhiteListExtImpl extends Thread implements UserIpWhiteListExt {

    private Logger logger = LoggerFactory.getLogger(UserIpWhiteListExtImpl.class);

    private Set<String> notAccessIps = new CopyOnWriteArraySet<>();

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            notAccessIps.clear();
            logger.info("finish clear notAccessIps");
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
            }
        }

        logger.warn("notAccessIps task exist");
    }

    @Override
    public Set<String> ipWhiteList() {
        ForwardProxyConfig config = ProxyConfig.fetchForwardProxyConfig();
        if (config.getIpWhiteListQueryUrl() == null) {
            return new HashSet<>();
        }
        Set<String> ips = new HashSet<>();
        try {
            String res = HttpCommonService.INSTANCE
                    .get(config.getIpWhiteListQueryUrl(), 1000);
            if (res == null || res.length() == 0) {
                return ips;
            }
            String[] split = res.split(";");
            ips.addAll(Arrays.asList(split));
            ips.add("127.0.0.1");
        } catch (Exception e) {
            logger.error("ipWhiteListQueryUrl call error!", e);
        }
        return ips;
    }

    @Override
    public void interceptNotify(String address) {
        ForwardProxyConfig config = ProxyConfig.fetchForwardProxyConfig();
        if (config.getIpInterceptNotifyUrl() == null || !config.getIpInterceptNotifyUrl().contains("%s")) {
            return;
        }
        try {
            String[] split = address.split(":");
            String ip = split[0];
            if (notAccessIps.contains(ip)) {
                return;
            }
            if (ip.startsWith("47.92")) {
                return;
            }
            synchronized (this) {
                if (notAccessIps.contains(ip)) {
                    return;
                }
                String url = String.format(
                                config.getIpInterceptNotifyUrl(),
                        URLEncoder.encode(address+ " has been intercept access to proxy!", "UTF-8"));
                HttpCommonService.INSTANCE.get(url, 1000);
                notAccessIps.add(ip);
            }
        } catch (Exception e) {
            logger.error("ipInterceptNotifyUrl call error! address = {}", address, e);
        }
    }
}
