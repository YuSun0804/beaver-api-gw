package com.beaver.apigw.server.impl;

import com.beaver.apigw.common.conf.ServerConfig;
import com.beaver.apigw.server.Server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServer implements Server {

    private final static int PORT = 8000;
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public void start(ServerConfig proxyConfig) {
        doStart(proxyConfig);
    }

    private void doStart(ServerConfig proxyConfig) {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("Boss-Thread"));
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("Worker-Thread"));

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class);

        // connections wait for accept(influenced by max conn)
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.option(ChannelOption.SO_REUSEADDR, true);
        b.childOption(ChannelOption.SO_KEEPALIVE, true);
        b.childOption(ChannelOption.TCP_NODELAY, true);
        b.childOption(ChannelOption.SO_SNDBUF, 32 * 1024);
        b.childOption(ChannelOption.SO_RCVBUF, 32 * 1024);
        // temporary settings, need more tests
        b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024));
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        // default is true, reduce thread context switching
        b.childOption(ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP, true);

        b.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                // 30 秒之内没有收到客户端请求的话就关闭连接
                ChannelPipeline p = ch.pipeline();
                p.addLast(new HttpServerCodec());
                p.addLast(new HttpObjectAggregator(512 * 1024));
                p.addLast(new RequestProcessHandler(proxyConfig));

            }
        });

        serverChannel = b.bind(proxyConfig.listen()).syncUninterruptibly().channel();
        log.info("server start at " + proxyConfig.listen());
    }


    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
