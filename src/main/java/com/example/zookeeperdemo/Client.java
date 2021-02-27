package com.example.zookeeperdemo;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 监测上下游节点  客户端
 */
public class Client {

    private final String CONNECT_STRING = "192.168.150.11:2181," +
            "192.168.150.12:2181,192.168.150.13:2181";
    private final int SESSION_TIMEOUT = 2000;
    ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        Client client = new Client();
        client.getConnect();
        client.getChild();
        client.business();
    }

    private void business() throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }

    private void getChild() throws KeeperException, InterruptedException {
        List<String> children = zooKeeper.getChildren("/servers", true);
        ArrayList<String> strings = new ArrayList<>();
        for (String child : children) {
            byte[] data = zooKeeper.getData("/servers/" + child, false, null);
            strings.add(new String(data));
        }
        System.out.println(strings);
    }

    private void getConnect() throws IOException {
        zooKeeper = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                try {
                    getChild();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
