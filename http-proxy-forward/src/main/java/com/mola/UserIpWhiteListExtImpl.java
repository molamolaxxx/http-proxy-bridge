package com.mola;

import com.mola.ext.UserIpWhiteListExt;
import com.mola.utils.HttpCommonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 20:22
 **/
public class UserIpWhiteListExtImpl extends Thread implements UserIpWhiteListExt {


    private Logger logger = LoggerFactory.getLogger(UserIpWhiteListExtImpl.class);

    private Set<String> notAccessIps = new CopyOnWriteArraySet<>();

    @Override
    public void run() {
        while (true) {
            notAccessIps.clear();
            logger.info("清除黑名单访问记录");
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public Set<String> ipWhiteList() {
        Set<String> ips = new HashSet<>();
        try {
            String res = HttpCommonService.INSTANCE
                    .get("https://molaspace.xyz:8550/chat/kv/proxyWhiteList", 1000);
            if (res == null || res.length() == 0) {
                return ips;
            }
            String[] split = res.split(";");
            ips.addAll(Arrays.asList(split));
            ips.add("127.0.0.1");
        } catch (Exception e) {
        }
        return ips;
    }

    @Override
    public void notifyNotAccess(String address) {
        try {
            String[] split = address.split(":");
            String ip = split[0];
            if (notAccessIps.contains(ip)) {
                return;
            }
            synchronized (this) {
                if (notAccessIps.contains(ip)) {
                    return;
                }
                String url =
                        String.format(
                                "https://molaspace.xyz:8550/chat/robot/push/toolRobot?toChatterId=%s&content=%s",
                                "1680059511788nQPEX", address+ "尝试连接代理，被白名单拦截");
                HttpCommonService.INSTANCE.get(url, 1000);
                notAccessIps.add(ip);
            }
        } catch (Exception e) {
        }
    }
}
