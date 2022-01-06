package com.beaver.apigw.constant;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class AttributeKeys {

	public static final AttributeKey<Channel> RESPONSE_CHANNEL_KEY = AttributeKey.valueOf("responseChannel");

	public static final AttributeKey<Boolean> KEEP_ALIVED_KEY = AttributeKey.valueOf("keepalived");

	public static final AttributeKey<Boolean> UPSTREAM_ACTIVE_CLOSE_KEY = AttributeKey.valueOf("upstreamActiveCloseKey");

}
