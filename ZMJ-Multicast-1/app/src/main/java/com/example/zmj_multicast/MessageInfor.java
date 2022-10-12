package com.example.zmj_multicast;
public class MessageInfor {
    private int ID;
    private String msg;
    private Long time;
    private Long userID;

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

    private String type;


}
