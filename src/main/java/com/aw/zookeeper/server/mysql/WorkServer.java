package com.aw.zookeeper.server.mysql;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.aw.zookeeper.server.enums.ActionType;
import com.aw.zookeeper.server.utils.MysqlUtil;
import com.aw.zookeeper.server.utils.RunningData;
import com.aw.zookeeper.server.vo.SynVo;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.CreateMode;

//import com.aw.zookeeper.util.Utils;
import com.mysql.jdbc.Statement;


/**
 * 实现自动选举主数据库并且数据同步
 *
 * @author jinbao_zhang_ext
 */

public class WorkServer implements Serializable {

    // Master节点对应zookeeper中的节点路径
    private static final String MASTER_PATH = "/master";//临时节点 关闭了会被释放
    private static final String SQLINFO_PATH = "/sqlinfo";
    private static final String MASTER_SQL_PATH = "/master-sql";
    private static final String SELECT_SQL_PATH = "/select-sql";
    private final String SERVERS = "/servers";

    //prx---
    //当前可用的读库
    private List<String> curRunningServer = new ArrayList<>();

    public List<String> getCurRunningServer() {
        List<String> children = zkClient.getChildren(SERVERS);
        for (String child : children) {
            byte[] data = zkClient.readData("/servers/" + child);
            curRunningServer.add(new String(data));
            System.out.println("------------------------" + new String(data));
        }

        return curRunningServer;
    }

    // 记录服务器状态
    private volatile boolean running = false;

    private ZkClient zkClient;

    // 监听Master节点删除事件
    private IZkDataListener dataListener;
    //监听sql节点改动信息
    private IZkDataListener sqlListener;
    // 记录当前节点的基本信息
    private RunningData serverData;
    //记录当前服务执行的sql语句，提供给所有服务
    private String sql;
    // 记录集群中Master节点的基本信息
    private RunningData masterData;
    //线程池
    private ScheduledExecutorService delayExector = Executors.newScheduledThreadPool(1);
    private int delayTime = 5;
    private Statement statement;
    //测试用的ip，用来指定该server对应的是哪台数据库
    private String dataBaseIp;

    public WorkServer(RunningData rd,Object data) {
        this.serverData = rd; // 记录服务器基本信息
        this.dataListener = new IZkDataListener() {

            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.println("**server :" + dataBaseIp + "处理删除监听" + dataPath);
                //takeMaster();

                if (masterData != null && masterData.getName().equals(serverData.getName())) {
                    // 自己就是上一轮的Master服务器，则直接抢
                    takeMaster();
                } else {
                    // 否则，延迟5秒后再抢。主要是应对网络抖动，给上一轮的Master服务器优先抢占master的权利，避免不必要的数据迁移开销
                    delayExector.schedule(new Runnable() {
                        public void run() {
                            try {
                                takeMaster();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }, delayTime, TimeUnit.SECONDS);
                }


            }

            public void handleDataChange(String dataPath, Object data) {
                System.out.println("**server :" + dataBaseIp + "处理数据改变监听" + dataPath +
                        "数据是 ：" + data);
                //从节点监听这个路径
                if (dataPath.equals(SQLINFO_PATH)) {
                    System.out.println(dataPath + "---changed,the data became " + data);
                    //statement=Utils.getSt();//实际生产环境获取的statement对象
                    //将获取到的sql语句执行到本地数据库，实现同步操作
                    try {
                        //也就是同步的时候如果一个节点出现了问题                              
                        statement = (Statement) MysqlUtil.getConnByIp(getDataBaseIp()).createStatement();//测试环境获取的statement对象
                        statement.executeUpdate(data.toString());
                    } catch (SQLException e) {

                        // TODO Auto-generated catch block
                        e.printStackTrace();//可以选择不打印 进行处理
                    } finally {
                        try {
                            statement.close();//测试环境关闭，释放内存，实际环境单例模式，可不用执行后立马关闭
                        } catch (SQLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
//                else if (dataPath.equals(MASTER_SQL_PATH)) {
//                    // 主节点监听master-sql-path  往从节点写sql 进行写更新
//                    zkClient.writeData(SQLINFO_PATH, sql.getBytes());
//                }
//                else if (dataPath.equals(SELECT_SQL_PATH)) {
//                    // 读操作 从节点读
//                    try {
//                        //也就是同步的时候如果一个节点出现了问题
//                        statement = (Statement) MysqlUtil.getConnByIp(getDataBaseIp()).createStatement();//测试环境获取的statement对象
//                        ResultSet resultSet = statement.executeQuery(sql);//将查询的结果放入ResultSet结果集中
//                        SynVo synVo = new SynVo();
//                        while (resultSet.next()) {
//                            int no = resultSet.getInt("no");
//                            String name = resultSet.getString("name");
//                            synVo.setName(name);
//                            synVo.setNo(no);
//                        }
//                        data = synVo;
//                    } catch (SQLException e) {
//
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();//可以选择不打印 进行处理
//                    } finally {
//                        try {
//                            statement.close();//测试环境关闭，释放内存，实际环境单例模式，可不用执行后立马关闭
//                        } catch (SQLException e) {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
//                    }
//                }
            }
        };

    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }


    public String getDataBaseIp() {
        return dataBaseIp;
    }

    public void setDataBaseIp(String dataBaseIp) {
        this.dataBaseIp = dataBaseIp;
    }

    // 启动服务器
    public void start() throws Exception {
        if (running) {
            throw new Exception("server has startup...");
        }
        running = true;

        // 订阅Master节点删除事件   根据 master 判断选master
//        if (dataBaseIp.equals())
        zkClient.subscribeDataChanges(MASTER_PATH, dataListener);
        zkClient.subscribeDataChanges(SQLINFO_PATH, dataListener);//有更新的就写在这里

        // 争抢Master权利
        takeMaster();

    }

    // 停止服务器
    public void stop() throws Exception {
        if (!running) {
            throw new Exception("server has stoped");
        }
        running = false;

        delayExector.shutdown();
        // 取消Master节点事件订阅
        zkClient.unsubscribeDataChanges(MASTER_PATH, dataListener);
        // 释放Master权利
        releaseMaster();

    }

    // 争抢Master
    private void takeMaster() throws SQLException {
        if (!running)
            return;
        try {
            // 尝试创建Master临时节点  在这才宣布的master 地位
            zkClient.create(MASTER_PATH, serverData, CreateMode.EPHEMERAL);
            masterData = serverData;
            //谁抢到谁是服务器
            System.out.println("master database ip 为" + masterData.getName() + "接下服务连接master该数据库");

            // master 监听 master-sql-path
//            zkClient.subscribeDataChanges(MASTER_SQL_PATH, dataListener);

            //作为演示，我们让服务器每隔5秒释放一次Master权利
            delayExector.schedule(new Runnable() {
                public void run() {
                    // TODO Auto-generated method stub
                    if (checkMaster()) {
                        releaseMaster();
                    }
                }
            }, 5, TimeUnit.SECONDS);

        } catch (ZkNodeExistsException e) { // 已被其他服务器创建了
            // 读取Master节点信息
            RunningData runningData = zkClient.readData(MASTER_PATH, true);
//            Object o = zkClient.readData(MASTER_PATH, true);
            if (runningData == null) {
                takeMaster(); // 没读到，读取瞬间Master节点宕机了，有机会再次争抢
            } else {
                //slave活过来从数据要更新与主数据保存一致
                masterData = runningData;
                //创建master数据库的statement对象
                //获取本地(从)数据库的statement对象
                Statement masterStatement = null;
                Statement statement = null;
                ResultSet rs = null;
                try {
                    masterStatement = (Statement) MysqlUtil.getConnByIp(masterData.getName()).createStatement();
                    statement = (Statement) MysqlUtil.getConnByIp(getDataBaseIp()).createStatement();
                    //清空本地数据库
                    statement.execute("delete from syn");
                    rs = masterStatement.executeQuery("select * from syn");
                    while (rs.next()) {
                        int no = rs.getInt("no");
                        String name = rs.getString("name");
                        System.out.println("no is " + no + ",name is" + name);
                        String sql = "insert into syn (no,name) values (" + no + ",'" + name + "')";
                        statement.addBatch(sql);
                    }
                    statement.executeBatch();

                } catch (SQLException e1) {
                    e1.printStackTrace();
                } finally {
                    if (masterStatement != null) {
                        masterStatement.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                }
            }
        }
    }

    // 释放Master权利
    private void releaseMaster() {
        if (checkMaster()) {
            zkClient.delete(MASTER_PATH);
        }
    }

    // 检测自己是否为Master
    private boolean checkMaster() {
        try {
            RunningData eventData = zkClient.readData(MASTER_PATH);
            masterData = eventData;
            if (masterData.getName().equals(serverData.getName())) {
                return true;
            }
            return false;
        } catch (ZkNoNodeException e) {
            return false; // 节点不存在，自己肯定不是Master了
        } catch (ZkInterruptedException e) {
            return checkMaster();
        } catch (ZkException e) {
            return false;
        }
    }

    //发送sql到zookeeper服务器上
    public void sendSql(String sql) {
        //在这里去进行分发 其实这里起到一个分发器的作用

        ActionType type = null;
        String[] sqls = sql.split(" ");
        if (sqls[0].toLowerCase().equals("insert")) {
            type = ActionType.INSERT;
        } else if (sqls[0].toLowerCase().equals("delete")) {
            type = ActionType.DELETE;
        } else if (sqls[0].toLowerCase().equals("update")) {
            type = ActionType.UPDATE;
        } else if (sqls[0].toLowerCase().equals("select")) {
            type = ActionType.SELECT;
        } else {
            System.out.println("输入的sql有误");
        }

        switch (type) {
            case DELETE:
            case INSERT:
            case UPDATE:
                if (zkClient.exists(SQLINFO_PATH)) {
                    zkClient.writeData(SQLINFO_PATH, sql);
                    System.out.println(SQLINFO_PATH + "所有数据库同步完成增删改操作");
                } else {
                    //创建永久节点  if 不存在
                    zkClient.create(SQLINFO_PATH, sql, CreateMode.PERSISTENT);
                }
            case SELECT:
                // 在这 直接监听上下线 返回一个就行了   接下来就是处理这个点
                if (zkClient.exists(SELECT_SQATH)) {
                    zkClient.writeData(SELECT_SQL_PATH, sql);
                    System.out.println(SELECT_SQL_PATH + "is exist");
                } else {
                    //创建永久节点
                    zkClient.create(SELECT_SQL_PATH, sql, CreateMode.PERSISTENT);
                }
        }
    }


}



