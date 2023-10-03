package com.mola;

import com.mola.forward.ForwardProxyServer;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-04 02:14
 **/
public class StartLooper extends Thread {

    private ForwardProxyServer forwardProxyServer;

    public StartLooper(ForwardProxyServer forwardProxyServer) {
        this.forwardProxyServer = forwardProxyServer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(30* 60 *1000);
            } catch (InterruptedException e) {
            }
            forwardProxyServer.shutdown();
        }
    }
}
