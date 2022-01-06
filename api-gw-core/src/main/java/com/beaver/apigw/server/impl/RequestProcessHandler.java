package com.beaver.apigw.server.impl;

import com.beaver.apigw.common.conf.ServerConfig;
import com.beaver.apigw.processor.ProcessorFactory;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class RequestProcessHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ServerConfig config;

    public RequestProcessHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        log.info(request.toString());
        Channel channel = ctx.channel();
        ProcessorFactory.processRequest(channel, request, config);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.warn(String.format("downstream channel[%s] writability changed, isWritable: %s", ctx.channel(),
                ctx.channel().isWritable()));
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn(String.format("downstream channel[%s] inactive", ctx.channel()));
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(String.format("downstream channel[%s] exceptionCaught", ctx.channel()), cause);
    }
}
