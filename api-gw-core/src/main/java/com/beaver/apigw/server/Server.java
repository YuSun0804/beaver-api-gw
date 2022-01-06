package com.beaver.apigw.server;

import com.beaver.apigw.common.conf.ServerConfig;

public interface Server {

    void start(ServerConfig proxyConfig);

    void stop();
}
