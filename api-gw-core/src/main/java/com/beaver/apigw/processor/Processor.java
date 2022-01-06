package com.beaver.apigw.processor;

import com.beaver.apigw.common.conf.ServerConfig;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Processor {

    void process(Channel channel, FullHttpRequest request, ServerConfig config) throws ExecutionException, InterruptedException, TimeoutException;
}
