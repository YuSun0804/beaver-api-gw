package com.beaver.apigw.proxy;

import com.beaver.apigw.common.conf.UpStreamServer;

public interface LoadBalancer {
    UpStreamServer getNext();
}
