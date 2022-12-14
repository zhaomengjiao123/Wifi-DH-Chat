package com.example.zmj_multicast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MYLOGA";
    public static Handler handler;
    private LayoutInflater inflater;
    private View layout;
    private AlertDialog.Builder builder;
    private TextView titleview;
    private ListView showmsg;
    private EditText sendmsgtext;
    private Button sendmsgbt;
    private Button startmulticast;
    private boolean isContinue = true;
    private String userSendMsg = "",titletext = "ZMJ-?????????";

    private boolean errorDisplayed = false;
    private boolean isDisplayedInHex = false;
    private boolean isListening = false;


    public static Long mID = 0L;
    public static List<MessageInfor> datas = new ArrayList<MessageInfor>();
    private SimpleDateFormat simpleDateFormat;
    private MessageAdapte messageAdapte;
    private static MulticastSocket socket = null;
    //???????????? ?????????????????????
    private static SenderThread senderThread;
    private static ReceiverThread receiverThread;
    private static InetAddress inetAddress = null;
    private int multiPort = 22324;
    private String multicastIP = "224.5.1.7";


    private WifiManager.MulticastLock wifiLock;

    private WifiMonitoringReceiver wifiMonitoringReceiver;

    WifiManager.MulticastLock multicastLock;

    private static final int IMAGE = 1;//??????????????????-????????????
    private static String[] PERMISSIONS_STORAGE = {
            //??????????????????
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();///???????????????
        allowMulticast();
        applypermission(); //????????????
        //???????????????
        InitView();
        //???????????????
        //ContinueSever();


        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == 0){
                    messageAdapte.notifyDataSetInvalidated();
                    messageAdapte.notifyDataSetChanged();
                }
                super.handleMessage(msg);
            }
        };

    }


    /**
     * ???????????????
     */
    private void InitView() {
        titleview = (TextView) findViewById(R.id.titleview);
        showmsg = (ListView) findViewById(R.id.showmsg);
        sendmsgtext = (EditText) findViewById(R.id.sendmsgtext);
        sendmsgbt = (Button) findViewById(R.id.sendmsgbt);
        startmulticast = (Button) findViewById(R.id.startmulticast);
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        messageAdapte = new MessageAdapte();
        showmsg.setAdapter(messageAdapte);
        sendmsgbt.setOnClickListener(this);
        startmulticast.setOnClickListener(this);
        titleview.setText(titletext);


        setWifiMonitorRegistered(true);
    }

    private void allowMulticast(){
        WifiManager wifiManager=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock=wifiManager.createMulticastLock("multicast.test");
        multicastLock.acquire();
    }



    //???????????????????????????????????????onCreat???????????????
    public void applypermission(){
        if(Build.VERSION.SDK_INT>=23){
            boolean needapply=false;
            for(int i=0;i<PERMISSIONS_STORAGE.length;i++){
                int chechpermission= ContextCompat.checkSelfPermission(getApplicationContext(),
                        PERMISSIONS_STORAGE[i]);
                if(chechpermission!= PackageManager.PERMISSION_GRANTED){
                    needapply=true;
                }
            }
            if(needapply){
                ActivityCompat.requestPermissions(MainActivity.this,PERMISSIONS_STORAGE,1);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startmulticast:
                startListening();
                break;
            case R.id.sendmsgbt://?????????????????????
                sendMsgText();
                break;
        }
    }


    private void setWifiLockAcquired(boolean paramBoolean)
    {
        if (paramBoolean)
        {
            if ((this.wifiLock != null) && (this.wifiLock.isHeld()))
                this.wifiLock.release();
            @SuppressLint("WrongConstant") WifiManager localWifiManager = (WifiManager)getSystemService("wifi");
            if (localWifiManager != null)
            {
                this.wifiLock = localWifiManager.createMulticastLock("MulticastTester");
                this.wifiLock.acquire();
            }
        }
        do
            return;
        while ((this.wifiLock == null) || (!this.wifiLock.isHeld()));

        //wifiLock.release();
    }

    private void setWifiMonitorRegistered(boolean paramBoolean)
    {
        if (paramBoolean)
        {
            if (this.wifiMonitoringReceiver != null)
                unregisterReceiver(this.wifiMonitoringReceiver);
            this.wifiMonitoringReceiver = new WifiMonitoringReceiver(this);
            registerReceiver(this.wifiMonitoringReceiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        }
//        do{
//            return;
//        }while (this.wifiMonitoringReceiver == null);
        //unregisterReceiver(this.wifiMonitoringReceiver);
        wifiMonitoringReceiver = null;
        Log.i(TAG,"wifi ????????????");
    }

    @SuppressLint("WrongConstant")
    private void startListening()
    {
        if (!this.isListening)
        {
            Log.i(TAG,"????????????....");
            if (!((ConnectivityManager)getSystemService("connectivity")).getNetworkInfo(1).isConnected()){
                Log.i(TAG,"Error: You are not connected to a WiFi network!");
            }

            setWifiLockAcquired(true);
            if (this.errorDisplayed)
            {
                this.errorDisplayed = false;
            }
            receiverThread = new ReceiverThread(this, multicastIP, multiPort);
            receiverThread.start();
            this.isListening = true;
        }
        return;

    }

    private void stopThreads()
    {
        if (receiverThread != null)
            receiverThread.stopRunning();
        if (senderThread != null)
            senderThread.interrupt();
    }

    protected void onDestroy()
    {
        super.onDestroy();
        if (this.isListening)
            stopListening();
        stopThreads();
    }


    //WiFi ??????
    public void onWifiDisconnected()
    {
        if (this.isListening)
        {
            stopListening();
        }
    }



    //????????????
    void stopListening()
    {
        if (this.isListening)
        {
            this.isListening = false;
            stopThreads();
            setWifiLockAcquired(false);
        }
    }

    protected void onStop()
    {
        super.onStop();
        setWifiMonitorRegistered(false);
    }



    //??????????????????
    private void sendMsgText(){
        //???????????? ???????????????
        if(isListening){
            String message = sendmsgtext.getText().toString();
            if(message==null||"".equals(message)){
                Toast.makeText(MainActivity.this,"????????????????????????",Toast.LENGTH_LONG).show();
                return ;
            }
            long Ltimes = System.currentTimeMillis();
            MessageInfor m = new MessageInfor(message,Ltimes,mID,"1");//?????? ????????? id??????
            //userSendMsg = "{\"isimg\":\"1\",\"msg\":\""+sendmsgtext.getText().toString()+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\"}";

            String send_msg = "{\"isimg\":\"1\",\"msg\":\""+sendmsgtext.getText().toString()+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\"}";

            datas.add(m);
            //??????????????????
            this.senderThread=new SenderThread(this,multicastIP,multiPort,send_msg);
            this.senderThread.start();

            sendmsgtext.setText("");
        }
    }





    class MessageAdapte extends BaseAdapter {

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public MessageInfor getItem(int i) {
            return datas.get(i);
        }

        @Override
        public long getItemId(int i) {
            Long id = datas.get(i).getUserID();
            return id==null?0:id;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MessageHolder holder = null;
            if(view == null){
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.chart_item,null);
                holder = new MessageHolder();
                holder.left = (TextView) view.findViewById(R.id.itemleft);
                holder.right = (TextView) view.findViewById(R.id.itemright);
                holder.lefttime = (TextView) view.findViewById(R.id.itemtimeleft);
                holder.righttime = (TextView) view.findViewById(R.id.itemtimeright);

                holder.rightimgtime = (TextView) view.findViewById(R.id.rightimgtime);
                holder.leftimgtime = (TextView) view.findViewById(R.id.leftimgtime);
                holder.rightimg = (ImageView) view.findViewById(R.id.rightimg);
                holder.leftimg = (ImageView) view.findViewById(R.id.leftimg);

                view.setTag(holder);
            }else {
                holder = (MessageHolder) view.getTag();
            }
            MessageInfor mi = getItem(i);
            //??????
            if (mi.getUserID() == mID){//id??????
                if(mi.getType().equals("1")){//??????
                    holder.leftimg.setVisibility(View.GONE);
                    holder.leftimgtime.setVisibility(View.GONE);
                    holder.rightimg.setVisibility(View.GONE);
                    holder.rightimgtime.setVisibility(View.GONE);

                    holder.left.setVisibility(View.GONE);
                    holder.lefttime.setVisibility(View.GONE);
                    holder.right.setVisibility(View.VISIBLE);
                    holder.righttime.setVisibility(View.VISIBLE);
                    holder.right.setText(mi.getMsg());
                    holder.righttime.setText(simpleDateFormat.format(new Date(mi.getTime())));
                }


            }else {
               if(mi.getType().equals("1")){//??????
                    holder.leftimg.setVisibility(View.GONE);
                    holder.leftimgtime.setVisibility(View.GONE);
                    holder.rightimg.setVisibility(View.GONE);
                    holder.rightimgtime.setVisibility(View.GONE);

                    holder.left.setVisibility(View.VISIBLE);
                    holder.lefttime.setVisibility(View.VISIBLE);
                    holder.right.setVisibility(View.GONE);
                    holder.righttime.setVisibility(View.GONE);
                    holder.left.setText(mi.getMsg());
                    holder.lefttime.setText(simpleDateFormat.format(new Date(mi.getTime())));
                }
            }
            return view;
        }
    }

    class MessageHolder{
        public TextView left;
        public TextView right;
        public TextView lefttime;
        public TextView righttime;
        private TextView rightimgtime;
        private TextView leftimgtime;
        private ImageView rightimg;
        private ImageView leftimg;

    }

    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        //??????????????????

        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            //activityImage(imagePath);
            c.close();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



}

























