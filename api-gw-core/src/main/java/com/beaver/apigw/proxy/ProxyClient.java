package com.beaver.apigw.proxy;

import com.beaver.apigw.common.conf.UpStreamServer;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface ProxyClient {

    void sendRequest(FullHttpRequest request, String ip, int port, Channel responseChannel) throws ExecutionException, InterruptedException, TimeoutException;
}
