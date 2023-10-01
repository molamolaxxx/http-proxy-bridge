package com.mola;

import com.mola.reverse.ReverseProxyServer;

public class ReverseStarter {
    public static void main(String[] args) {
        ReverseProxyServer reverseProxyServer = new ReverseProxyServer();
        reverseProxyServer.start("120.27.230.24", 10433, 128);
    }
}