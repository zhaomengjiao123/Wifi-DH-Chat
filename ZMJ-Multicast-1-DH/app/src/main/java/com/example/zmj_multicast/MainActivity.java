package com.example.zmj_multicast;//package com.example.zmj_multicast;

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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

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
    private String userSendMsg = "",titletext = "ZMJ-聊天室";

    private boolean errorDisplayed = false;
    private boolean isDisplayedInHex = false;
    private boolean isListening = false;

    public static boolean isResponse=false; //是否要发送应答消息？


    public static Long mID = 0L;
    //数据源
    public static List<MessageInfor> datas = new ArrayList<MessageInfor>();
    private SimpleDateFormat simpleDateFormat;
    private MessageAdapte messageAdapte;
    private static MulticastSocket socket = null;
    //两个线程 一个收，一个发
    private static SenderThread senderThread;
    private static ReceiverThread receiverThread;
    private static InetAddress inetAddress = null;
    public static int multiPort = 22324;
    public static String multicastIP = "224.5.1.7";

    //DH加密
    public static String publicb;
    //密钥对
    public static KeyPair receiverKeyPair;
    public static SecretKey receiverDesKey;
    public static Cipher cipher;
    public static byte[] receiverPublicKeyEnc;
    public static Map<String, SecretKey> map= new HashMap<String,SecretKey>();


    private WifiManager.MulticastLock wifiLock;

    private WifiMonitoringReceiver wifiMonitoringReceiver;

    WifiManager.MulticastLock multicastLock;

    private static final int IMAGE = 1;//调用系统相册-选择图片
    private static String[] PERMISSIONS_STORAGE = {
            //依次权限申请
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();///隐藏标题栏
        allowMulticast();
        applypermission(); //申请权限
        //初始化控件
        InitView();
        //连接服务器
        //ContinueSever();

        //初始化mID
        //mID=System.currentTimeMillis();
        mID=System.currentTimeMillis()-1;


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
     * 初始化控件
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



    //定义判断权限申请的函数，在onCreat中调用就行
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
            case R.id.sendmsgbt://客户端发送消息
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
        Log.i(TAG,"wifi 设置完毕");
    }

    @SuppressLint("WrongConstant")
    private void startListening()
    {
        if (!this.isListening)
        {
            Log.i(TAG,"开始监听....");
            if (!((ConnectivityManager)getSystemService("connectivity")).getNetworkInfo(1).isConnected()){
                Log.i(TAG,"Error: You are not connected to a WiFi network!");
            }

            setWifiLockAcquired(true);
            if (this.errorDisplayed)
            {
                this.errorDisplayed = false;
            }
            Log.i(TAG,"即将启动接收线程");
            //发送我方公钥
            sendMyPublic();
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

    //WiFi 断连
    public void onWifiDisconnected()
    {
        if (this.isListening)
        {
            stopListening();
        }
    }

    //停止监听
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

    //发送普通加密消息
    private void sendMsgText(){
        //在监听中 才可以发送
        if(isListening){
            String message = sendmsgtext.getText().toString();
            if(message==null||"".equals(message)){
                Toast.makeText(MainActivity.this,"发送消息不能为空",Toast.LENGTH_LONG).show();
                return ;
            }
            long Ltimes = System.currentTimeMillis();

            MessageInfor mm = new MessageInfor();//时间戳 ID 加密后的消息
            mm.setTime(Ltimes);
            mm.setType("1");
            mm.setUserID(mID);

            //获取Map中的所有key
            Set<String> keySet = map.keySet();
          //遍历存放所有key的Set集合
            Iterator<String> it =keySet.iterator();
            Log.i(TAG,"走到了遍历之前");
            while(it.hasNext()){                         //利用了Iterator迭代器**
                //得到每一个key
                String key = it.next();
                //通过key获取对应的value
                SecretKey  value = map.get(key);
                Log.i(TAG,"找到的本地密钥是："+value);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE,value);
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
                byte[] result = new byte[0];
                try {
                    Log.i(TAG,"消息加密之前："+message);
                    //只将消息内容加密
                    result = cipher.doFinal(message.getBytes());
                    Log.i(TAG,"消息加密后是："+result);
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }


                mm.setByt(result);
                mm.setMsg("[我]："+message+" 加密后："+result.toString());
                //userSendMsg = "{\"type\":\"1\",\"msg\":\""+result+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\"}";

                //启动发送线程
                this.senderThread=new SenderThread(this,multicastIP,multiPort,mm);
                this.senderThread.start();
            }
            datas.add(mm);


            sendmsgtext.setText("");
        }
    }

    //作为应答方，为应答对方，发送第二个公钥
    public void sendResponsePublic() throws NoSuchAlgorithmException {
        long Ltimes = System.currentTimeMillis();

        //String msg=receiverPublicKeyEnc.toString();
        MessageInfor ms=new MessageInfor();
        ms.setByt(receiverPublicKeyEnc);
        ms.setType("0");
        ms.setFlag(2);
        ms.setTime(Ltimes);
        ms.setUserID(mID);
        ms.setMsg("[我]发送了应答公钥："+receiverPublicKeyEnc.toString());
        //0 是公钥 1 是普通消息
        // flag 2 应答公钥
        //String send_2_Pub = "{\"type\":\"0\",\"flag\":\"2\",\"msg\":\""+receiverPublicKeyEnc+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\"}";
        datas.add(ms);
        //启动发送线程
        senderThread=new SenderThread(this,multicastIP,multiPort,ms);
        senderThread.start();

        //ms.setMsg("已发送公钥2:"+receiverPublicKeyEnc);
        Log.i(TAG,"发送了应答公钥");

    }

    //初始化安全密钥相关----作为初始发送方，首先发送公钥,只要点击开始就发送这个
    //作为甲方，初始化自己密钥
    private void sendMyPublic(){
        long Ltimes = System.currentTimeMillis();
        KeyPairGenerator senderKeyPairGenerator = null;
        try {
            //实例化密钥对生成器
            senderKeyPairGenerator = KeyPairGenerator.getInstance("DH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //初始化密钥对生成器
        senderKeyPairGenerator.initialize(512);
        //生成密钥对
        receiverKeyPair=senderKeyPairGenerator.generateKeyPair();
        //甲方公钥
        //发送方公钥，发送给接收方（网络、文件。。。）
        receiverPublicKeyEnc =  receiverKeyPair.getPublic().getEncoded();
        Log.i(TAG,"成功生成发送方公钥："+receiverPublicKeyEnc);
        //System.out.println(senderKeyPairGenerator);

        publicb=String.valueOf(receiverPublicKeyEnc);
        //公布甲方公钥
        MessageInfor messageInfor=new MessageInfor();
        //messageInfor.setMsg("已发送公钥1:"+receiverPublicKeyEnc);
        messageInfor.setUserID(mID);
        messageInfor.setType("0");
        messageInfor.setFlag(1);
        messageInfor.setByt(receiverPublicKeyEnc);
        messageInfor.setTime(Ltimes);
        messageInfor.setMsg("[我]发送公钥:"+receiverPublicKeyEnc.toString());
        datas.add(messageInfor);

        //启动发送线程
        this.senderThread=new SenderThread(this,multicastIP,multiPort,messageInfor);
        this.senderThread.start();
        Log.i(TAG,"加入群聊，发送公钥："+messageInfor.getMsg());
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
            //显示
            if (mi.getUserID() == mID){//id相等

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



            }else {

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
        //获取图片路径

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

























