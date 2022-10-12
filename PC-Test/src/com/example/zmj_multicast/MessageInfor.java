package com.example.zmj_multicast;

import java.io.Serializable;

public class MessageInfor implements Serializable {
    private static final long serialVersionUID = 26910;
    private int ID;
    private String msg;
    private Long time;
    private Long userID;

    public int flag; //1第一个公钥 2第二个公钥
    public String type; //0公钥 1消息

    public byte[] byt;

    public MessageInfor(){}

    public MessageInfor(String msg, Long time,Long userID) {
        this.msg = msg;
        this.time = time;
        this.userID = userID;
//        this.type = type;
//        this.flag = flag;
    }

    public MessageInfor(Long time, Long userID,byte[] byt) {
        this.time = time;
        this.userID = userID;
        this.byt=byt;
//        this.type = type;
//        this.flag = flag;
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



    public byte[] getByt() {
        return byt;
    }

    public void setByt(byte[] byt) {
        this.byt = byt;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
