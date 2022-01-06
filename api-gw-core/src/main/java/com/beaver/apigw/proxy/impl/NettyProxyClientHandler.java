package com.beaver.apigw.proxy.impl;

import com.beaver.apigw.common.conf.UpStreamServer;
import com.beaver.apigw.constant.AttributeKeys;
import com.beaver.apigw.domian.RequestContext;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.LinkedList;

@Slf4j
public class NettyProxyClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final UpStreamServer server;

    private final String proxyPass;

    public NettyProxyClientHandler(UpStreamServer server, String proxyPass) {
        this.server = server;
        this.proxyPass = proxyPass;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        Channel upstream = ctx.channel();

        // get context and clear
        Channel responseChannel = upstream.attr(AttributeKeys.RESPONSE_CHANNEL_KEY).getAndSet(null);
        boolean keepAlived = true;


        LinkedList<Connection> conns = RequestContext.keepAlivedConntions(proxyPass);

        if (conns.size() == server.getKeepalive()) {
            // the least recently used connection are closed
            log.info(String.format(
                    "[%s]cached connctions exceed the keepalive[%d], the least recently used connection are closed",
                    proxyPass, server.getKeepalive()));
            Channel tmp = conns.pollFirst().getChannel();
            tmp.attr(AttributeKeys.UPSTREAM_ACTIVE_CLOSE_KEY).set(true);
            tmp.close();
        }
        conns.addLast(new Connection(server, upstream));
        if (keepAlived) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            responseChannel.writeAndFlush(response.retain(), responseChannel.voidPromise());
        } else {// close the responseChannel connection
            responseChannel.writeAndFlush(response.retain()).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        boolean activeClose = false;
        if (ctx.channel().hasAttr(AttributeKeys.UPSTREAM_ACTIVE_CLOSE_KEY)
                && ctx.channel().attr(AttributeKeys.UPSTREAM_ACTIVE_CLOSE_KEY).get()) {
            activeClose = true;
        }

        log.warn(String.format("upstream channel[%s] inactive, activeClose:%s", ctx.channel(), activeClose));

        Channel downstream = null;
        Boolean keepAlived = null;
        if (null != (downstream = ctx.channel().attr(AttributeKeys.RESPONSE_CHANNEL_KEY).get())
                && null != (keepAlived = ctx.channel().attr(AttributeKeys.KEEP_ALIVED_KEY).get())) {
            if (keepAlived) {
                downstream.writeAndFlush(RequestContext.errorResponse(), downstream.voidPromise());
            } else {
                downstream.writeAndFlush(RequestContext.errorResponse()).addListener(ChannelFutureListener.CLOSE);
            }
        } else {// remove current inactive channel from cached conns
            LinkedList<Connection> conns = RequestContext.keepAlivedConntions(proxyPass);
            Connection tmp = null;

            for (Iterator<Connection> it = conns.iterator(); it.hasNext(); ) {
                tmp = it.next();
                // find the inactive connection
                if (server == tmp.getUpStreamServer()) {
                    it.remove();
                    break;
                }
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.warn(String.format("upstream channel[%s] writability changed, isWritable: %s", ctx.channel(),
                ctx.channel().isWritable()));
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(String.format("upstream channel[%s] exceptionCaught", ctx.channel()), cause);
    }
}
