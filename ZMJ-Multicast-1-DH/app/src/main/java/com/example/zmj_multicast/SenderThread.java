package com.example.zmj_multicast;//package com.example.zmj_multicast;

import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class SenderThread extends MyThread{
    private String send_msg;
    private MessageInfor messageInfor;

    public SenderThread(MainActivity activity, String multicastIP, int multicastPort, MessageInfor
                         messageInfor) {
        super(activity, new Handler(), multicastIP, multicastPort);
        this.messageInfor=messageInfor;
    }


    public void run()
    {
        super.run();
        try
        {
            Log.i(MainActivity.TAG,"发送的消息是："+messageInfor.getMsg());

            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(messageInfor);        //序列化对象
            oout.flush();

            //得到字节数组
            //byte[] arrayOfByte = this.send_msg.getBytes();
            byte[] arrayOfByte = bout.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(arrayOfByte, arrayOfByte.length,
                    InetAddress.getByName(this.multicastIP), this.multicastPort);
            Log.i(MainActivity.TAG,"要发送的数据包是："+sendPacket);
            this.multicastSocket.send(sendPacket);
            //发送完关闭
            this.multicastSocket.close();
            return;
        }catch (IOException localIOException)
        {
            localIOException.printStackTrace();
        }
        //关闭发送线程
//        if (this.multicastSocket != null){
//            this.multicastSocket.close();
//        }

        return;
    }
}
