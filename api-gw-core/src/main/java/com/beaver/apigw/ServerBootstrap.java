package com.beaver.apigw;

import com.beaver.apigw.common.conf.ServerConfig;
import com.beaver.apigw.common.exception.ConfigException;
import com.beaver.apigw.processor.ProcessorFactory;
import com.beaver.apigw.proxy.LoadBalancerFactory;
import com.beaver.apigw.server.impl.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ServerBootstrap {

    private final static String DEFAULT_CONFIG_FILE_PATH = "proxy.yml";

    public static void init(String configFilePath){
        try {
            ServerConfig config = new ServerConfig();
            if (configFilePath == null) {
                config.parse(DEFAULT_CONFIG_FILE_PATH);
            } else {
                config.parse(configFilePath);
            }

            ProcessorFactory.buildProcessorChain(config);
            LoadBalancerFactory.init(config);
            new NettyServer().start(config);
        } catch (ConfigException e) {
            log.error("server start failed", e);
        } catch (IOException e) {
            log.error("server start failed", e);
        }
    }

    public static void main(String[] args) {
        init(null);
    }
}
