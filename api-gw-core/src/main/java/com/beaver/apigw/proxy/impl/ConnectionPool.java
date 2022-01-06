package com.beaver.apigw.proxy.impl;

import com.beaver.apigw.common.conf.UpStreamServer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConnectionPool {

    private static ConcurrentMap<UpStreamServer, List<Connection>> connPool = new ConcurrentHashMap<>();

    public static List<Connection>  getConn(UpStreamServer upStreamServer) {
        List<Connection> connections = connPool.get(upStreamServer);
        if (connections == null) {
            connections = new ArrayList<>();
        }
        return connections;
    }

    public static void addConn(UpStreamServer upStreamServer, Connection upStreamConnection) {
        List<Connection> connections = connPool.getOrDefault(upStreamServer, new ArrayList<Connection>());
        connections.add(upStreamConnection);
        connPool.put(upStreamServer, connections);
    }
}

