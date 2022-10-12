package com.example.zmj_multicast;//package com.example.zmj_multicast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;




public class WifiMonitoringReceiver extends BroadcastReceiver
{
    private MainActivity mainActivity;

    public WifiMonitoringReceiver(MainActivity paramMainActivity)
    {
        this.mainActivity = paramMainActivity;
    }

    public void onReceive(Context paramContext, Intent paramIntent)
    {
        if (this.mainActivity != null)
        {
            @SuppressLint("WrongConstant") ConnectivityManager localConnectivityManager = (ConnectivityManager)paramContext.getSystemService("connectivity");
            //this.mainActivity.log("Checking Wifi...");
            Log.i(MainActivity.TAG,"正在检查 WiFi......");
            if (!localConnectivityManager.getNetworkInfo(1).isConnected())
            {
                Log.i(MainActivity.TAG,"WiFi 不可用！！");
                this.mainActivity.onWifiDisconnected();
            }
        }
        Log.i(MainActivity.TAG,"检查完了，走了走了....");
    }
}
