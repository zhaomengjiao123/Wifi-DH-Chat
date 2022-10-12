package com.example.zmj_multicast;//package com.example.zmj_multicast;

import java.io.Serializable;

public class MessageInfor implements Serializable {
    private static final long serialVersionUID = 26910;
    private int ID;
    private String msg; //可能是消息也可能是密钥
    private Long time;
    private Long userID;
    private int flag; // 1 第一个公钥 2第二个公钥
    private String type; // 0 是公钥 1 是消息
    private byte[] byt;

    public MessageInfor(){}

    public MessageInfor(String msg, Long time, Long userID, String type) {
        this.msg = msg;
        this.time = time;
        this.userID = userID;
        this.type = type;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public byte[] getByt() {
        return byt;
    }

    public void setByt(byte[] byt) {
        this.byt = byt;
    }
}
