package com.mola.proxy.bridge.core.utils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-12-15 21:24
 **/
public class LoadBalanceSelector {

    private static final int RANDOM = 0;
    private static final int ROUND_ROBBING = 1;

    private static LoadBalanceSelector instance;

    private int mode;

    public static LoadBalanceSelector instance() {
        if (instance != null) {
            return instance;
        }
        synchronized (LoadBalanceSelector.class) {
            if (instance != null) {
                return instance;
            }
            instance = new LoadBalanceSelector();
            instance.mode = ROUND_ROBBING;
            return instance;
        }
    }

    private final AtomicInteger roundRobbingIdx = new AtomicInteger();

    public <T> T select(List<T> sourceList) {
        if (sourceList == null || sourceList.size() == 0) {
            return null;
        }
        int idx;
        switch (mode) {
            case RANDOM: {
                idx = getRandomNextInt(sourceList.size());
                break;
            }
            case ROUND_ROBBING: {
                idx = getRoundRobbingNextInt(sourceList.size());
                break;
            }
            default: {
                throw new RuntimeException("unknown balance mode = " + mode);
            }
        }
        return sourceList.get(Math.min(idx, sourceList.size() - 1));
    }

    private int getRandomNextInt(int size) {
        Random random = new Random();
        return random.nextInt(size);
    }

    private int getRoundRobbingNextInt(int size) {
        if (roundRobbingIdx.get() == Integer.MAX_VALUE - 1) {
            roundRobbingIdx.compareAndSet(Integer.MAX_VALUE - 1, 0);
        }
        return roundRobbingIdx.incrementAndGet() % size;
    }

    private void setMode(int mode) {
        this.mode = mode;
    }
}
