package com.aw.zookeeper.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlUtil {
    private String name;
    private final static String USER = "root";
    private final static String PASSWD = "123456";
    private final static String PORT = "8013";

    public static Connection getConnByIp(String dataBaseIp){
        try {
            String finalUrl = "jdbc:mysql://"+dataBaseIp+":"+PORT+"/testsyn";
            return DriverManager.getConnection(finalUrl,USER,PASSWD);
        } catch (SQLException throwables) {
            System.out.println("连接sql出现问题\n" + "问题机器IP ： dataBaseIp");
            //此处进行邮箱或者短信通知提醒
            throwables.printStackTrace();
        }
        return null;
    }

    public static void sendSql(String s){


    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
