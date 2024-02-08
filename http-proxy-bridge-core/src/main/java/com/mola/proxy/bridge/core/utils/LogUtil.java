package com.mola.proxy.bridge.core.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-10-03 22:39
 **/
public class LogUtil {
    public static void debugReject() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getTurboFilterList().add(new TurboFilter() {
            @Override
            public FilterReply decide(Marker marker, Logger logger, Level level, String s, Object[] objects, Throwable throwable) {
                return level.levelStr.equalsIgnoreCase("debug")
                        || level.levelStr.equalsIgnoreCase("trace")
                        ? FilterReply.DENY : FilterReply.ACCEPT;
            }
        });
    }
}
