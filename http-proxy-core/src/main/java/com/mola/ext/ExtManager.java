package com.mola.ext;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 15:39
 **/
public class ExtManager {

    private static UserIpWhiteListExt userIpWhiteListExt;

    private static Socks5AuthExt socks5AuthExt;

    private static HostMappingExt hostMappingExt;

    private static SslAuthExt sslAuthExt;


    public static void setUserIpWhiteListExt(UserIpWhiteListExt userIpWhiteListExt) {
        ExtManager.userIpWhiteListExt = userIpWhiteListExt;
    }

    public static void setSocks5AuthExt(Socks5AuthExt socks5AuthExt) {
        ExtManager.socks5AuthExt = socks5AuthExt;
    }

    public static UserIpWhiteListExt getUserIpWhiteListExt() {
        return userIpWhiteListExt;
    }

    public static Socks5AuthExt getSocks5AuthExt() {
        return socks5AuthExt;
    }

    public static void setHostMappingExt(HostMappingExt hostMappingExt) {
        ExtManager.hostMappingExt = hostMappingExt;
    }

    public static HostMappingExt getHostMappingExt() {
        return hostMappingExt;
    }

    public static SslAuthExt getSslAuthExt() {
        return sslAuthExt;
    }

    public static void setSslAuthExt(SslAuthExt sslAuthExt) {
        ExtManager.sslAuthExt = sslAuthExt;
    }
}
