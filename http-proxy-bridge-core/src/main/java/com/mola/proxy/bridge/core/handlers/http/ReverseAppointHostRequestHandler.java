package com.mola.proxy.bridge.core.handlers.http;

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.utils.AssertUtil;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.Channel;

import java.util.List;
import java.util.Random;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 请求处理器，指定连接host和端口
 * @date : 2024-04-21 16:23
 **/
public class ReverseAppointHostRequestHandler extends HttpRequestHandler {

    private final List<String> appointHosts;

    public ReverseAppointHostRequestHandler(List<String> appointHosts) {
        this.appointHosts = appointHosts;
    }

    @Override
    protected ProxyHttpHeader parseProxyHeader(String header, Channel client2proxyChannel) {
        Random random = new Random();
        RemotingHelper.HostAndPort hostAndPort = RemotingHelper.parseHostAndPort(
                appointHosts.get(random.nextInt(appointHosts.size()))
        );
        AssertUtil.notNull(hostAndPort, "hostAndPort non null");
        return new ProxyHttpHeader(hostAndPort.host, hostAndPort.port, false);
    }
}
