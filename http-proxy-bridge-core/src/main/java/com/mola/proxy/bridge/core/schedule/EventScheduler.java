package com.mola.proxy.bridge.core.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 事件调度器
 * @date : 2024-08-17 20:30
 **/
public class EventScheduler {

    private static final int REFRESH_DURING = 1000;

    private static final Logger logger = LoggerFactory.getLogger(EventScheduler.class);

    private static final Map<String, ScheduleEvent> eventMap = new ConcurrentHashMap<>();

    private static final EventScheduleThread eventScheduleThread = new EventScheduleThread();

    private static final AtomicBoolean startFlag = new AtomicBoolean();

    public static void addEvent(String eventName, int intervalSeconds, Runnable runnable) {
        if (eventMap.containsKey(eventName)) {
            throw new RuntimeException("already exist event named " + eventName);
        }
        if (!startFlag.getAndSet(true)) {
            eventScheduleThread.start();
        }
        eventMap.put(eventName, new ScheduleEvent(eventName, intervalSeconds, runnable));
    }

    private static class EventScheduleThread extends Thread {

        public EventScheduleThread() {
            super("event-schedule-thread");
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                for (ScheduleEvent event : eventMap.values()) {
                    if (event.lastScheduleTime < 0
                            || System.currentTimeMillis() - event.lastScheduleTime >
                            event.intervalSeconds * 1000L - 50) {
                        executeEvent(event);
                        event.lastScheduleTime = System.currentTimeMillis();
                    }
                }

                try {
                    Thread.sleep(REFRESH_DURING);
                } catch (InterruptedException e) {
                    throw new RuntimeException("EventScheduler thread interrupted");
                }
            }
        }

        private void executeEvent(ScheduleEvent event) {
            try {
                event.runnable.run();
            } catch (Exception e) {
                logger.error("runScheduleEvent failed, eventName = {}", event.eventName, e);
            }
        }
    }

    private static class ScheduleEvent {

        /**
         * 事件名称
         */
        private final String eventName;

        /**
         * 间隔时间（s）
         */
        private final int intervalSeconds;

        /**
         * 执行方法
         */
        private final Runnable runnable;

        private long lastScheduleTime = -1L;

        public ScheduleEvent(String eventName, int intervalSeconds, Runnable runnable) {
            this.eventName = eventName;
            this.intervalSeconds = intervalSeconds;
            this.runnable = runnable;
        }
    }
}
