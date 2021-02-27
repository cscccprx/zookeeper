package com.aw.zookeeper.server.mysql;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.aw.zookeeper.server.utils.RunningData;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

public class Server1 {
    private  static String ZOOKEEPER_SERVER="192.168.150.11";
    private List<String> curRunningServer = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ZkClient client = new ZkClient(ZOOKEEPER_SERVER, 50000, 50000, new SerializableSerializer());
        //创建临时且带序号节点 便于查找读库 实现节点上下线的监控
//        client.create("/servers/server" , ZOOKEEPER_SERVER.getBytes(), CreateMode.EPHEMERAL_SEQUENTIAL);

        RunningData ip=new RunningData();
        ip.setName(ZOOKEEPER_SERVER); //服务器信息  就是这个ip
        Object data = null;
        WorkServer server =new WorkServer(ip,data);
        //这是本地服务器IP,实际生产环境不需要设置
        server.setDataBaseIp(ZOOKEEPER_SERVER);//-prx 这里有待推敲
        server.setZkClient(client);
        server.start();
        for(int i=1;i<=100;i++){
//            System.out.println("敲回车添加数据！\n");
//            String s = new BufferedReader(new InputStreamReader(System.in)).readLine();
//            //增 insert into syn (no,name) values("+i+",'zhangjinbao');
//            //删 delete from syn where no = 1;
//            //改 update syn set name = "pp" where no = 2；
//            //查 select * from syn;
//            server.sendSql(s);
            System.out.println("敲回车添加数据！\n");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            server.sendSql("insert into syn (no,name) values("+i+",'zhangjinbao');");
        }
        System.out.println("敲回车退出程序！\n");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
        server.stop();
        server.getZkClient().close();
    }

}

