package com.example.zmj_multicast;



import android.os.Handler;
import android.util.Log;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

public class ReceiverThread extends MyThread {
    MainActivity activity;

    public ReceiverThread(MainActivity activity, String multicastIP, int multicastPort) {

        super(activity, new Handler(), multicastIP, multicastPort);
        this.activity=activity;

        Log.i(MainActivity.TAG,"构造函数没有问题");
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
                //Log.i(com.example.zmj_multicast.MainActivity.TAG,"....");
            }
            try
            {
                if (this.multicastSocket != null)
                {
                    //Log.i(com.example.zmj_multicast.MainActivity.TAG,"正在监听....");
                    //接收
                    this.multicastSocket.receive(localDatagramPacket);

                    String content=new String(localDatagramPacket.getData());
                    Log.i(MainActivity.TAG,"收到来自["+localDatagramPacket.getAddress().getHostAddress()+"]的组播消息："+content.trim());

                    MessageInfor messageInfor;
                    ByteArrayInputStream bint=new ByteArrayInputStream(buf);
                    ObjectInputStream oint=new ObjectInputStream(bint);
                    messageInfor=(MessageInfor) oint.readObject();

                    Log.i(MainActivity.TAG,"成功转为了对象："+messageInfor.getMsg());
                    String sender_ip=localDatagramPacket.getAddress().getHostAddress();
                    try {
                        //得到系统时间
                        long Ltimes = System.currentTimeMillis();
                        long mID_now=Ltimes;
                        //设置格式
                        //content="{\"type\":\"1\",\"msg\":\""+content.trim()+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID_now+"\"}";
                        //JSONObject json = new JSONObject(content);
                        //判断是否是来自自身的消息
                        if(!messageInfor.getUserID().equals(MainActivity.mID)){
                            MessageInfor accept;
                            String msg_toshow;
                            //Log.i(com.example.zmj_multicast.MainActivity.TAG,"JSON："+json);
                            //先判断是不是消息
                            //1 是消息
                            if(messageInfor.getType().equals("1")){
                                Log.i(MainActivity.TAG,"是消息类型");
                                //是消息---显示
                                MainActivity.cipher.init(Cipher.DECRYPT_MODE,MainActivity.map.get(sender_ip));
                                byte[] bu_msg = new byte[1024];
                                bu_msg=MainActivity.cipher.doFinal(messageInfor.getByt());
                                String c=new String(bu_msg);
                                Log.i(MainActivity.TAG,"解密后的消息是："+c);

                                msg_toshow="["+localDatagramPacket.getAddress().getHostAddress()+"]："+messageInfor.getMsg()+" 解密为："+c;
                                accept=new MessageInfor(msg_toshow, messageInfor.getTime(), messageInfor.getUserID(),"1");
                                MainActivity.datas.add(accept);

                            }else if(messageInfor.getType().equals("0")){
                                //是公钥
                                Log.i(MainActivity.TAG,"是公钥类型");
                                if(messageInfor.getFlag()==2){
                                    //是对方发过来的公钥
                                    Log.i(MainActivity.TAG,"收到的公钥类型是2");
                                    msg_toshow="["+localDatagramPacket.getAddress().getHostAddress().toString()+"]："+messageInfor.getMsg();
                                    accept=new MessageInfor(msg_toshow, messageInfor.getTime(), messageInfor.getUserID(),"1");
                                    MainActivity.datas.add(accept);
                                    //初始化密钥工厂
                                    KeyFactory receiverKeyFactory = KeyFactory.getInstance("DH");
                                    //byte[] bytes=json.getString("msg").getBytes(StandardCharsets.UTF_8);
                                    //解析对方公钥
                                    X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(messageInfor.getByt());
                                    PublicKey receiverPublicKey = receiverKeyFactory.generatePublic(x509EncodedKeySpec);
                                    KeyAgreement receiverKeyAgreement = KeyAgreement.getInstance("DH");
                                    receiverKeyAgreement.init(MainActivity.receiverKeyPair.getPrivate());
                                    receiverKeyAgreement.doPhase(receiverPublicKey, true);
                                    //生成本地密钥
                                    MainActivity.receiverDesKey = receiverKeyAgreement.generateSecret("DES");
                                    MainActivity.cipher = Cipher.getInstance("DES");
                                    //存储
                                    MainActivity.map.put(sender_ip,MainActivity.receiverDesKey);
                                }else {
                                    Log.i(MainActivity.TAG,"收到的公钥类型是1");
                                    msg_toshow="["+localDatagramPacket.getAddress().getHostAddress().toString()+"]："+messageInfor.getMsg();
                                    accept=new MessageInfor(msg_toshow, messageInfor.getTime(), messageInfor.getUserID(),"1");
                                    MainActivity.datas.add(accept);
                                    //收到的是对方发的公钥，此时己方应该立刻响应，也发出一个公钥
                                    //根据甲方公钥构造乙方密钥
                                    //实例化密钥工厂
                                    KeyFactory receiverKeyFactory = KeyFactory.getInstance("DH");
                                    //byte[] bytes=json.getString("msg").getBytes(StandardCharsets.UTF_8);
                                    //解析甲方公钥
                                    //转换
                                    X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(messageInfor.getByt());
                                    //产生公钥
                                    PublicKey receiverPublicKey = receiverKeyFactory.generatePublic(x509EncodedKeySpec);
                                    //由甲方公钥构建乙方密钥
                                    DHParameterSpec dhParameterSpec = ((DHPublicKey)receiverPublicKey).getParams();
                                    //实例化密钥对生成器
                                    KeyPairGenerator receiverKeyPairGenerator = KeyPairGenerator.getInstance("DH");
                                    receiverKeyPairGenerator.initialize(dhParameterSpec);
                                    //产生密钥对
                                    KeyPair receiverKeypair = receiverKeyPairGenerator.generateKeyPair();
                                    //乙方密钥
                                    PrivateKey receiverPrivateKey = receiverKeypair.getPrivate();
                                    //乙方公钥
                                    MainActivity.receiverPublicKeyEnc = receiverKeypair.getPublic().getEncoded();
                                    KeyAgreement receiverKeyAgreement = KeyAgreement.getInstance("DH");
                                    receiverKeyAgreement.init(receiverPrivateKey);
                                    receiverKeyAgreement.doPhase(receiverPublicKey, true);
                                    //生成本地密钥
                                    MainActivity.receiverDesKey = receiverKeyAgreement.generateSecret("DES");
                                    MainActivity.cipher = Cipher.getInstance("DES");
                                    //存储
                                    MainActivity.map.put(sender_ip,MainActivity.receiverDesKey);
                                    //发送 2 响应公钥
                                    activity.sendResponsePublic();
                                    //com.example.zmj_multicast.MainActivity.sendResponsePublic();

                                }

                            }else{
                                Log.i(MainActivity.TAG,"普通消息");
                            }
                        }
                        //通知主线程消息源发生变化
                        MainActivity.handler.sendEmptyMessage(0);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    } catch (NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    } catch (BadPaddingException e) {
                        e.printStackTrace();
                    } catch (IllegalBlockSizeException e) {
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
