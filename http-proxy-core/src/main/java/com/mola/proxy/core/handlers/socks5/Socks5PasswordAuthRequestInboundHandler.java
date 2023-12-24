package com.mola.proxy.core.handlers.socks5;

import com.mola.proxy.core.ext.ExtManager;
import com.mola.proxy.core.ext.Socks5AuthExt;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class Socks5PasswordAuthRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        Socks5AuthExt socks5AuthExt = ExtManager.getSocks5AuthExt();
        if (socks5AuthExt == null) {
            throw new RuntimeException("socks5AuthExt can not be null! please call ExtManager.setSocks5AuthExt");
        }
        //认证成功
        if (socks5AuthExt.auth(msg.username(), msg.password())) {
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
            ctx.writeAndFlush(passwordAuthResponse);
            ctx.pipeline().remove(this);
            ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder.class);
            return;
        }
        Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
        //发送鉴权失败消息，完成后关闭channel
        ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
    }
}
