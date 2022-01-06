package com.beaver.apigw.processor.impl;

import com.beaver.apigw.common.conf.ServerConfig;
import com.beaver.apigw.common.conf.UpStreamServer;
import com.beaver.apigw.domian.RequestContext;
import com.beaver.apigw.processor.Processor;
import com.beaver.apigw.proxy.LoadBalancerFactory;
import com.beaver.apigw.proxy.ProxyClient;
import com.beaver.apigw.proxy.ProxyClientFactory;
import com.beaver.apigw.proxy.impl.Connection;
import com.beaver.apigw.proxy.impl.RoundRobin;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ProxyProcessor implements Processor {

    @Override
    public void process(Channel channel, FullHttpRequest request, ServerConfig config) throws ExecutionException, InterruptedException, TimeoutException {
        boolean keepAlived = HttpUtil.isKeepAlive(request);
        HttpHeaders requestHeaders = request.headers();

        // get Host header
        String serverName = requestHeaders.get(HttpHeaderNames.HOST);
        // get proxy_pass
        String proxyPass = config.proxyPass(serverName, request.uri());

        // get roundRobin
        RoundRobin roundRobin = null;
        UpStreamServer server = null;
        if (null == proxyPass || null == (roundRobin = LoadBalancerFactory.getRoundRobin(proxyPass))
                || null == (server = roundRobin.getNext())) {
            // return 404
            notFound(channel, keepAlived);
            return;
        }

        // rewrite http request(keep alive to upstream)
        request.setProtocolVersion(HttpVersion.HTTP_1_1);
        requestHeaders.remove(HttpHeaderNames.CONNECTION);

        // increase refCount
        request.retain();
        // proxy request
        proxy(server, proxyPass, channel, request);
    }

    public void proxy(UpStreamServer upStreamServer, String proxyPass, Channel channel, FullHttpRequest request) throws ExecutionException, InterruptedException, TimeoutException {
        ProxyClient proxyClient = ProxyClientFactory.getProxyClient(upStreamServer, proxyPass, channel.eventLoop());
        proxyClient.sendRequest(request, upStreamServer.getIp(), upStreamServer.getPort(), channel);
    }


    public void notFound(Channel channel, boolean keepAlived) {
        if (keepAlived) {
            channel.writeAndFlush(RequestContext.notfoundResponse(), channel.voidPromise());
        } else {
            channel.writeAndFlush(RequestContext.notfoundResponse()).addListener(ChannelFutureListener.CLOSE);
        }
    }


}
