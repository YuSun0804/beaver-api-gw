package com.beaver.apigw.proxy;



import com.beaver.apigw.common.conf.ServerConfig;
import com.beaver.apigw.common.conf.UpStreamServer;
import com.beaver.apigw.proxy.impl.RoundRobin;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LoadBalancerFactory {

	private final static Map<String, RoundRobin> robinMap = new HashMap<>();

	public static void init(ServerConfig config) {
		Map<String, List<UpStreamServer>> upstreams = config.upstreams();
		if (null == upstreams || upstreams.isEmpty()) {
			return;
		}

		for (Entry<String, List<UpStreamServer>> upstreamEntry : upstreams.entrySet()) {
			robinMap.put(upstreamEntry.getKey(), new RoundRobin(upstreamEntry.getValue()));
		}
	}

	public static RoundRobin getRoundRobin(String proxypass) {
		return robinMap.get(proxypass);
	}
}
