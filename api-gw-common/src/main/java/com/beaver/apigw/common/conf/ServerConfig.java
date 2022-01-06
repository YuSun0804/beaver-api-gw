package com.beaver.apigw.common.conf;


import com.beaver.apigw.common.exception.ConfigException;
import com.beaver.apigw.common.util.AntPathMatcher;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class ServerConfig {

	private static final String UPSTREAM_POOL_PREFIX = "http://";

	private static final String AUTO = "auto";

	private static final int DEFAULT_HTTP_PORT = 80;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@JsonProperty("listen")
	private int listen;

	@JsonProperty("keepalive_timeout")
	private int keepaliveTimeout;

	@JsonProperty("worker_threads")
	private String workerThreads;
	private int workers;

	@JsonProperty("worker_connections")
	private int workerConnections;

	@JsonProperty("servers")
	private Map<String, List<Location>> servers;

	@JsonProperty("upstreams")
	private Map<String, Upstream> upstreams;

	private Map<String, List<UpStreamServer>> us = new HashMap<>();

	public void parse(String path) throws ConfigException, IOException {


		ClassLoader classLoader = ServerConfig.class.getClassLoader();
		URL url = classLoader.getResources(path).nextElement();
		path = url.getPath();
		File configFile = new File(path);

		log.info("Reading configuration from: " + configFile);
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			if (!configFile.exists()) {
				throw new IllegalArgumentException(configFile.toString() + " file is missing");
			}

			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			parseConfig(mapper.readValue(new File(path), ServerConfig.class));
		} catch (IOException e) {
			throw new ConfigException("Error processing " + path, e);
		} catch (IllegalArgumentException e) {
			throw new ConfigException("Error processing " + path, e);
		}
	}

	public void parseConfig(ServerConfig xproxyConfig) throws ConfigException {
		listen = xproxyConfig.listen;
		keepaliveTimeout = xproxyConfig.keepaliveTimeout;
		workerConnections = xproxyConfig.workerConnections;
		workerThreads = xproxyConfig.workerThreads;

		if (AUTO.equalsIgnoreCase(workerThreads)) {
			workers = Runtime.getRuntime().availableProcessors();
		} else {
			try {
				workers = Integer.parseInt(workerThreads);
			} catch (NumberFormatException e) {
				throw new ConfigException("worker_threads invalid", e);
			}
		}

		upstreams = new HashMap<>();
		for (Entry<String, Upstream> entry : xproxyConfig.upstreams.entrySet()) {
			upstreams.put(UPSTREAM_POOL_PREFIX + entry.getKey(), entry.getValue());
		}

		servers = new HashMap<>();
		if (DEFAULT_HTTP_PORT != listen) {
			for (Entry<String, List<Location>> entry : xproxyConfig.servers.entrySet()) {
				servers.put(entry.getKey() + ":" + listen, entry.getValue());
			}
		} else {
			servers = xproxyConfig.servers;
		}

		List<String> hosts;
		List<UpStreamServer> servers;
		for (Entry<String, Upstream> upstreamEntry : upstreams.entrySet()) {
			hosts = upstreamEntry.getValue().servers();
			if (hosts.isEmpty()) {
				continue;
			}
			servers = new ArrayList<>(1 << 2);
			for (String host : hosts) {
				servers.add(new UpStreamServer(host, upstreamEntry.getValue().keepAlive()));
			}
			us.put(upstreamEntry.getKey(), servers);
		}
	}

	public int listen() {
		return listen;
	}

	public int keepaliveTimeout() {
		return keepaliveTimeout;
	}

	public int workerThreads() {
		return workers;
	}

	public int workerConnections() {
		return workerConnections;
	}

	public Map<String, List<UpStreamServer>> upstreams() {
		return us;
	}

	public String proxyPass(String serverName, String uri) {
		List<Location> locations = servers.get(serverName);
		if (locations.isEmpty()) {
			return null;
		}
		for (Location location : locations) {
			if (pathMatcher.match(location.path(), uri)) {
				return location.proxypass();
			}
		}
		return null;
	}

	static class Location {

		@JsonProperty("path")
		private String path;

		@JsonProperty("proxy_pass")
		private String proxypass;

		public String path() {
			return path;
		}

		public String proxypass() {
			return proxypass;
		}
	}

	static class Upstream {
		// the maximum number of idle keepalive connections to upstream servers
		// that are preserved in the cache of each worker process
		@JsonProperty("keepalive")
		private int keepalive;

		@JsonProperty("servers")
		private List<String> servers;

		public int keepAlive() {
			return keepalive;
		}

		public List<String> servers() {
			return servers;
		}
	}
}
