package com.example.zmj_multicast;

import static com.example.zmj_multicast.Utils.bytesToHex;

import android.os.Handler;
import android.util.Log;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class ReceiverThread extends MyThread{

    public ReceiverThread(MainActivity activity, String multicastIP, int multicastPort) {
        super(activity, new Handler(), multicastIP, multicastPort);
    }


    public void run()
    {
        super.run();
        byte[] buf = new byte[1024];
        DatagramPacket localDatagramPacket = new DatagramPacket(buf, buf.length);

        while (true)
        {
            if (this.running.get()){
                localDatagramPacket.setData(buf);
                //Log.i(MainActivity.TAG,"....");
            }
            try
            {
                if (this.multicastSocket != null)
                {
                    //Log.i(MainActivity.TAG,"正在监听....");
                    //接收
                    this.multicastSocket.receive(localDatagramPacket);

                    String content=new String(localDatagramPacket.getData());
                    Log.i(MainActivity.TAG,"收到来自["+localDatagramPacket.getAddress().getHostAddress()+"]的组播消息："+content.trim());

                    MessageInfor messageInfor;
                    ByteArrayInputStream bint=new ByteArrayInputStream(buf);
                    ObjectInputStream oint=new ObjectInputStream(bint);
                    messageInfor=(MessageInfor) oint.readObject();

                   Log.i(MainActivity.TAG,"成功转为了对象："+messageInfor.getMsg());

                    try {
                        //得到系统时间
                        long Ltimes = System.currentTimeMillis();
                        long mID_now=Ltimes;
                        content=content.trim();
                        //设置格式
                        //content="{\"isimg\":\"1\",\"msg\":\""+content.trim()+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID_now+"\"}";
                        JSONObject json = new  JSONObject(content);
                        //判断是否是来自自身的消息
                        if(messageInfor.getID() != MainActivity.mID){
                            Log.i(MainActivity.TAG,"JSON："+json);

                            String msg_toshow="["+localDatagramPacket.getAddress().getHostAddress().toString()+"]："+messageInfor.getMsg();
                            MessageInfor accept=new MessageInfor(msg_toshow,messageInfor.getTime(),messageInfor.getUserID(),"1");
                            MainActivity.datas.add(accept);


                        }
                        //通知主线程消息源发生变化
                        MainActivity.handler.sendEmptyMessage(0);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }

            }
            catch (IOException | ClassNotFoundException localIOException)
            {
            }
        }
    }
}
