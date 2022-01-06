package com.beaver.apigw.processor;

import com.beaver.apigw.common.conf.ServerConfig;
import com.beaver.apigw.processor.impl.ProxyProcessor;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ProcessorFactory {

    private final static List<Processor> processorList = new ArrayList<>();

    public static void buildProcessorChain(ServerConfig config) {
        processorList.add(new ProxyProcessor());
    }

    public static void processRequest(Channel channel, FullHttpRequest request, ServerConfig config) throws ExecutionException, InterruptedException, TimeoutException {
        for (Processor processor: processorList) {
            processor.process(channel, request, config);
        }
    }
}
