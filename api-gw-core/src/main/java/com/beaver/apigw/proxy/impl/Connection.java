package com.beaver.apigw.proxy.impl;

import com.beaver.apigw.common.conf.UpStreamServer;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Connection {
    private UpStreamServer upStreamServer;
    private Channel channel;
}

