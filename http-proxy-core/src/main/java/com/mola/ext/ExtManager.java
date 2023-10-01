package com.mola.ext;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 15:39
 **/
public class ExtManager {

    private static UserIpWhiteListExt userIpWhiteListExt;


    public static void setUserIpWhiteListExt(UserIpWhiteListExt userIpWhiteListExt) {
        ExtManager.userIpWhiteListExt = userIpWhiteListExt;
    }

    public static UserIpWhiteListExt getUserIpWhiteListExt() {
        return userIpWhiteListExt;
    }
}
