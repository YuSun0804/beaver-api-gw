package com.beaver.apigw.proxy.impl;


import com.beaver.apigw.common.conf.UpStreamServer;
import com.beaver.apigw.proxy.LoadBalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobin implements LoadBalancer {

    private AtomicInteger idx = new AtomicInteger();
    private List<UpStreamServer> servers;

    public RoundRobin(List<UpStreamServer> servers) {
        this.servers = servers;
    }

    @Override
    public UpStreamServer getNext() {
        return servers.get(Math.abs(idx.getAndIncrement() % servers.size()));
    }
}

