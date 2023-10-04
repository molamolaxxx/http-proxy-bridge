package com.mola.forward;

import com.mola.ext.ExtManager;
import com.mola.ext.UserIpWhiteListExt;
import com.mola.pool.ReverseProxyConnectPool;
import com.mola.reverse.ReverseProxyChannelMonitor;
import com.mola.utils.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 15:33
 **/

@ChannelHandler.Sharable
public class WhiteListAccessHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhiteListAccessHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String address = RemotingHelper.parseChannelRemoteAddress(ctx.channel());
        String[] splitRes = address.split(":");
        if (splitRes.length != 2) {
            return;
        }
        UserIpWhiteListExt userIpWhiteListExt = ExtManager.getUserIpWhiteListExt();
        if (userIpWhiteListExt == null) {
            return;
        }
        if (splitRes[0].startsWith("192.168") || splitRes[0].startsWith("127.0.0.1")) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (userIpWhiteListExt.ipWhiteList() != null && userIpWhiteListExt.ipWhiteList()
                .contains(splitRes[0])) {
            ctx.fireChannelRead(msg);
            return;
        }
        userIpWhiteListExt.notifyNotAccess(address);
    }
}
