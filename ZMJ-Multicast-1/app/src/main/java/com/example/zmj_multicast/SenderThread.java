package com.example.zmj_multicast;

import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import javax.net.ssl.ManagerFactoryParameters;

public class SenderThread extends MyThread{
    private String send_msg;

    public SenderThread(MainActivity activity, String multicastIP, int multicastPort, String send_msg) {
        super(activity, new Handler(), multicastIP, multicastPort);
        this.send_msg=send_msg;
    }


    public void run()
    {
        super.run();
        try
        {
            Log.i(MainActivity.TAG,"发送的消息是："+send_msg);
            //得到字节数组
            byte[] arrayOfByte = this.send_msg.getBytes();
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
