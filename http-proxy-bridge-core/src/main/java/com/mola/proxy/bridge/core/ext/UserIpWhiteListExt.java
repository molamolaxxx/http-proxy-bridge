package com.mola.proxy.bridge.core.ext;

import java.util.Set;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 15:37
 **/
public interface UserIpWhiteListExt {

    /**
     * 可使用服务的ip
     * @return
     */
    Set<String> ipWhiteList();

    /**
     * 未授权登录的通知
     * @param address
     */
    void interceptNotify(String address);
}
