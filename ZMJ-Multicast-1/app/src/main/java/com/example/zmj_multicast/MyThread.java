package com.example.zmj_multicast;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.Handler;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyThread extends Thread{

    final MainActivity activity;
    final Handler handler;
    private InetAddress inetAddress;
    final String multicastIP;
    final int multicastPort;
    MulticastSocket multicastSocket;
    final AtomicBoolean running = new AtomicBoolean(true);

    public MyThread(MainActivity activity, Handler handler, String multicastIP, int multicastPort) {
        this.activity = activity;
        this.handler = handler;
        this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
    }


    String getLocalIP()
    {
        return this.inetAddress.getHostAddress();
    }

    public void run()
    {
        try
        {
            @SuppressLint("WrongConstant") int i = ((WifiManager)this.activity.getSystemService("wifi")).getConnectionInfo().getIpAddress();
            byte[] arrayOfByte = new byte[4];
            arrayOfByte[0] = (byte)(i & 0xFF);
            arrayOfByte[1] = (byte)(0xFF & i >> 8);
            arrayOfByte[2] = (byte)(0xFF & i >> 16);
            arrayOfByte[3] = (byte)(0xFF & i >> 24);
            this.inetAddress = InetAddress.getByAddress(arrayOfByte);
            NetworkInterface localNetworkInterface = NetworkInterface.getByInetAddress(this.inetAddress);
            this.multicastSocket.setNetworkInterface(localNetworkInterface);
            //创建组播套接字
            this.multicastSocket = new MulticastSocket(this.multicastPort);
            //加入指定的组播组
            this.multicastSocket.joinGroup(InetAddress.getByName(this.multicastIP));
            this.multicastSocket.setSoTimeout(100);
            //设置TTL
            this.multicastSocket.setTimeToLive(2);
            return;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    void stopRunning()
    {
        this.running.set(false);
    }
}
