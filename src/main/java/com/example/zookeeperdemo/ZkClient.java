package com.example.zookeeperdemo;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * zk联系api
 */
public class ZkClient {
    private ZooKeeper zkCli;
    private static final String CONNING_STRING = "192.168.150.11:2181," +
            "192.168.150.12:2181,192.168.150.13:2181";
    private static final int SESSION_TIMEOUT = 2000;

    @Before
    public void before() throws IOException {
        zkCli = new ZooKeeper(CONNING_STRING, SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.println("默认回调函数");
            }
        });
    }
    @Test
    public void ls() throws KeeperException, InterruptedException {
        List<String> children = zkCli.getChildren("/", true);
        System.out.println("=========================");
        for (String child : children) {
            System.out.println(child);
        }
        System.out.println("=========================");
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * 循环注册
     * 实现可以循环监听一个节点  当节点数据发生变化时监听到并进行回调
     */
    public void register() throws KeeperException, InterruptedException {
        byte[] data = zkCli.getData("/a", new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                try {
                    register();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, null);
        System.out.println(new String(data));
    }

    @Test
    public void testRegister(){
        try {
            register();
            Thread.sleep(Long.MAX_VALUE);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
