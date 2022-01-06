package com.beaver.apigw.proxy;

import com.beaver.apigw.common.conf.UpStreamServer;
import com.beaver.apigw.proxy.impl.NettyProxyClient;
import io.netty.channel.EventLoop;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProxyClientFactory {

    private static ConcurrentMap<String, ProxyClient> proxies = new ConcurrentHashMap<>();

    public static ProxyClient getProxyClient(UpStreamServer server, String proxyPass, EventLoop eventLoop) {
        ProxyClient proxyClient = proxies.get(proxyPass);
        if (proxyClient == null) {
            proxyClient = new NettyProxyClient(eventLoop,server, proxyPass);
            proxies.put(proxyPass, proxyClient);
        }
        return proxyClient;
    }

}
