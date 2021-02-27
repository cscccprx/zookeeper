package com.aw.zookeeper.server.utils;

import com.aw.zookeeper.server.vo.SynVo;
import lombok.Data;

import java.io.Serializable;

@Data
public class RunningData implements Serializable {
    // 传输的数据必须要实现序列化
    private String name;
    private Object data;
}
