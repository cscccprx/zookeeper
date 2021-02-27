package com.aw.zookeeper.server.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Person1 {

    public static void main(String[] args) throws IOException {




        for(int i=1;i<=100;i++){
            System.out.println("敲回车添加数据！\n");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            //server.sendSql("insert into syn (no,name) values("+i+",'zhangjinbao');");
        }
        System.out.println("敲回车退出程序！\n");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

    }

}
