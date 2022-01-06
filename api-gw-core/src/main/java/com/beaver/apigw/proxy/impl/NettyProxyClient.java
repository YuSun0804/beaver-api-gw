package com.beaver.apigw.proxy.impl;

import com.beaver.apigw.common.conf.UpStreamServer;
import com.beaver.apigw.constant.AttributeKeys;
import com.beaver.apigw.proxy.ProxyClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class NettyProxyClient implements ProxyClient {

    private Channel channel;
    private Bootstrap bootstrap;

    public NettyProxyClient(EventLoop eventLoop, UpStreamServer server, String proxyPass) {
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(eventLoop);
        this.bootstrap.channel(NioSocketChannel.class);

        this.bootstrap.option(ChannelOption.TCP_NODELAY, true);
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // default is pooled direct
        // ByteBuf(io.netty.util.internal.PlatformDependent.DIRECT_BUFFER_PREFERRED)
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        // 32kb(for massive long connections, See
        // http://www.infoq.com/cn/articles/netty-million-level-push-service-design-points)
        // 64kb(RocketMq remoting default value)
        this.bootstrap.option(ChannelOption.SO_SNDBUF, 32 * 1024);
        this.bootstrap.option(ChannelOption.SO_RCVBUF, 32 * 1024);
        // temporary settings, need more tests
        this.bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024));
        // default is true, reduce thread context switching
        this.bootstrap.option(ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP, true);

        this.bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpObjectAggregator(512 * 1024));
                pipeline.addLast(new NettyProxyClientHandler(server, proxyPass));
            }
        });
    }


    @Override
    public void sendRequest(FullHttpRequest request, String ip, int port, Channel responseChannel) throws ExecutionException, InterruptedException, TimeoutException {
        if (channel == null || !channel.isActive()) {
            this.bootstrap.connect(ip, port).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("The client has connected [{}] successful!", ip + port);
                    this.channel = future.channel();
                    doSend(request, responseChannel);
                } else {
                    throw new IllegalStateException();
                }
            });
        } else if (channel.isActive()) {
            doSend(request, responseChannel);
        }
    }

    private void doSend(FullHttpRequest request, Channel responseChannel) {
        // set request context
        channel.attr(AttributeKeys.RESPONSE_CHANNEL_KEY).set(responseChannel);

        request.retain();
        channel.writeAndFlush(request);
    }
}
