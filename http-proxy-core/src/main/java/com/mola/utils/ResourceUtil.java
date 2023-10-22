package com.mola.utils;

import java.net.URL;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-22 19:31
 **/
public class ResourceUtil {

    public static String fetchResourcePath(String filename) {
        URL resourceUrl = ResourceUtil.class.getClassLoader().getResource(filename);
        if (resourceUrl != null) {
            return resourceUrl.getPath();
        }
        throw new RuntimeException("can not found file " + filename + "in resource");
    }
}
