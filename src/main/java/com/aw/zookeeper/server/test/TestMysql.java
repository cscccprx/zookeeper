package com.aw.zookeeper.server.test;

import com.aw.zookeeper.server.utils.MysqlUtil;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;

public class TestMysql {

    @Test
    public void test01() {
        Statement st = null;
        try {
            st = MysqlUtil.getConnByIp("192.168.150.11").createStatement();
//            st.execute("delete from syn");
//            int no = 1;String name = "asd";
//            String sql = "insert into syn (no,name) values (" + no + ",'" + name + "')";
//            st.addBatch(sql);
            String sql = "select * from syn ";
            st.executeUpdate(sql);
        } catch (SQLException throwables) {
            System.out.println("连接数据库失败");
            throwables.printStackTrace();
        }
    }
}
