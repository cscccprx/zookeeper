package com.aw.zookeeper.server.enums;

public enum ActionType {
    INSERT(1),DELETE(2),UPDATE(3),SELECT(4);

    ActionType(int type){
        this.type = type;
    }

    private int type ;



}
