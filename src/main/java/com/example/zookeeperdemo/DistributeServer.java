package com.example.zookeeperdemo;

import com.mysql.jdbc.PingTarget;
import org.apache.zookeeper.*;

import java.io.IOException;

/**
 * 监测上下游节点的状态      服务端
 */
public class DistributeServer {

    private final String CONNECT_STRING = "192.168.150.11:2181," +
            "192.168.150.12:2181,192.168.150.13:2181";
    private final int SESSION_TIMEOUT = 2000;
    ZooKeeper zooKeeper;
    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {

        DistributeServer distributeServer = new DistributeServer();
        distributeServer.getConnect();
        distributeServer.register("host1");

        distributeServer.business();

    }

    private void business() throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }

    private void register(String hostname) throws KeeperException, InterruptedException {
        zooKeeper.create("/servers/server",hostname.getBytes() , ZooDefs.Ids.OPEN_ACL_UNSAFE
                , CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private void getConnect() throws IOException {
        zooKeeper = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {

            }
        });
    }
}
