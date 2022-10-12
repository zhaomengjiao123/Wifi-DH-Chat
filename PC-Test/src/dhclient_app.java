import com.example.zmj_multicast.MessageInfor;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;


public class dhclient_app {
    private static String  groupHost = "224.5.1.7";
    private static Integer port = 22324;
    private MulticastSocket multicastSocket;
    private DatagramSocket datagramSocket;
    public static String publicb;
    private KeyPair receiverKeyPair;
    public SecretKey receiverDesKey;
    private static Cipher cipher;
    public static byte[] receiverPublicKeyEnc;
    private JTextArea jta;
    //每次都需要改
    String ownIp="/192.168.7.251";
    public static Map<InetAddress, SecretKey> map= new HashMap<InetAddress,SecretKey>();


    public dhclient_app() {
    }

    public void init(){
        try {
            multicastSocket = new MulticastSocket(port);
            InetAddress inetAddress = InetAddress.getByName(groupHost); //组地址
            multicastSocket.joinGroup(inetAddress); //加入到组播组中
            datagramSocket = new DatagramSocket(); //DatagramSocket实例


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void start(){
        receives();
        send();
    }



    public void receives(){
        new Thread(){

            @Override
            public void run() {
                while(true){
                    try{
                        byte[] buf = new byte[1024]; //接收数据缓冲
                        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); //接收数据的数据报


                        multicastSocket.receive(datagramPacket); //接收数据
                        InetAddress sender_ip=datagramPacket.getAddress();
                        System.out.println("读取的发送方的IP是："+sender_ip.toString());


                        MessageInfor message;
                        ByteArrayInputStream bint=new ByteArrayInputStream(buf);
                        ObjectInputStream oint=new ObjectInputStream(bint);
                        message=(MessageInfor) oint.readObject();

                        System.out.println(message.getType());
                        //0  公钥
                        if(!(ownIp).equals(sender_ip.toString())&&map.get(sender_ip)==null&&message.getType().equals("0")){
                            //System.out.println("type 00");
                            if(message.getFlag()==2){
                                System.out.println("是对方发过来的公钥：");
                                KeyFactory receiverKeyFactory = KeyFactory.getInstance("DH");
                                //byte[] bytes_2=json.getString("msg").getBytes(StandardCharsets.UTF_8);
                                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(message.getByt());
                                PublicKey receiverPublicKey = receiverKeyFactory.generatePublic(x509EncodedKeySpec);

                                KeyAgreement receiverKeyAgreement = KeyAgreement.getInstance("DH");
                                receiverKeyAgreement.init(receiverKeyPair.getPrivate());
                                receiverKeyAgreement.doPhase(receiverPublicKey, true);
                                receiverDesKey = receiverKeyAgreement.generateSecret("DES");
                                cipher = Cipher.getInstance("DES");
                                map.put(sender_ip,receiverDesKey);
                                System.out.println(sender_ip+"发过来的是："+message.getByt().toString());
                            }else{
                                //收到的是对方发的公钥，此时己方应该立刻响应，也发出一个公钥
                                KeyFactory receiverKeyFactory = KeyFactory.getInstance("DH");
                                System.out.println("是需要响应一个公钥：");
                               // System.out.println(json.getString("msg"));
                                //byte[] bytes_3=json.getString("msg").getBytes("ISO-8859-1");
                                //System.out.println(bytes_3);
                                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(message.getByt());
                                System.out.println(x509EncodedKeySpec);
                                PublicKey receiverPublicKey = receiverKeyFactory.generatePublic(x509EncodedKeySpec);
                                DHParameterSpec dhParameterSpec = ((DHPublicKey)receiverPublicKey).getParams();
                                KeyPairGenerator receiverKeyPairGenerator = KeyPairGenerator.getInstance("DH");
                                receiverKeyPairGenerator.initialize(dhParameterSpec);
                                KeyPair receiverKeypair = receiverKeyPairGenerator.generateKeyPair();
                                PrivateKey receiverPrivateKey = receiverKeypair.getPrivate();
                                receiverPublicKeyEnc = receiverKeypair.getPublic().getEncoded();
                                KeyAgreement receiverKeyAgreement = KeyAgreement.getInstance("DH");
                                receiverKeyAgreement.init(receiverPrivateKey);
                                receiverKeyAgreement.doPhase(receiverPublicKey, true);
                                receiverDesKey = receiverKeyAgreement.generateSecret("DES");
                                cipher = Cipher.getInstance("DES");
                                map.put(sender_ip,receiverDesKey);
                                System.out.println("我响应的是："+receiverDesKey);
                                //cipher.init(Cipher.ENCRYPT_MODE, receiverDesKey);
                                sendPublic();
                            }


                        }else if(!(ownIp).equals(sender_ip.toString())&&map.get(sender_ip)!=null&&message.getType().equals("1")){
                            //是普通的消息类型
                            System.out.println("client收到的加密消息为："+message.getByt().toString());
                            //byte[] bytes_4=json.getString("msg").getBytes(StandardCharsets.UTF_8);

                            cipher.init(Cipher.DECRYPT_MODE, map.get(sender_ip));
                            buf = cipher.doFinal(message.getByt());
                            String a=new String(buf);
                            System.out.println("解密后 : " + a);
                            System.out.println("收到的解密消息为："+a);
                            String info = sender_ip+": " + a + "\r\n";
                            jta.append(info);
                        }
                    } catch (EOFException e) {
                        e.printStackTrace();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }

    //返回响应公钥
    public void sendPublic() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        long Ltimes = System.currentTimeMillis();
        long mID= Ltimes;
        MessageInfor ms=new MessageInfor(Ltimes,mID,receiverPublicKeyEnc);
        ms.setType("0");
        ms.setFlag(2);

        //String re=new String(receiverPublicKeyEnc,"UTF-8");
       // String userSendMsg = "{\"type\":\"0\",\"flag\":\"2\",\"msg\":\""+re+"\",\"times\":\""+Ltimes+"\",\"id\":\""+mID+"\"}";
        ms.setMsg("发送响应公钥:"+receiverPublicKeyEnc);

        this.send(ms);//通过DatagramPacket发送对象
    }


    public void send(MessageInfor message){
        try{

            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(message);        //序列化对象
            oout.flush();
            //oout.close();
            byte[] buf=bout.toByteArray();
            InetAddress inetAddress = InetAddress.getByName(groupHost); //组播地址
            DatagramPacket datagramPacket= new DatagramPacket(buf, buf.length, inetAddress, port); //发送数据报
            datagramSocket.send(datagramPacket); //发送数据
            //System.out.println("已发送："+ message);
            //datagramSocket.close(); //关闭端口
        }catch(Exception e){

        }
    }





    public void send(){
        new Thread(){

            @Override
            public void run() {
                Scanner s = new Scanner(System.in);
                while(true){
                    try{
                        String message = s.nextLine();

                        byte[] buf = message.getBytes("UTF-8"); //发送信息
                        InetAddress inetAddress = InetAddress.getByName(groupHost); //组播地址
                        DatagramPacket datagramPacket= new DatagramPacket(buf, buf.length, inetAddress, port); //发送数据报
                        datagramSocket.send(datagramPacket); //发送数据
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }

        }.start();
    }
    public void close(){
        try{
            if(null!=multicastSocket){
                multicastSocket.close();
            }
            if(null!=datagramSocket){
                datagramSocket.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }



    public static void main(String[] qrgs){
        dhclient_app m = new dhclient_app();

        JFrame frmMain = new JFrame("DH client界面");
        frmMain.setSize(600, 600);
        frmMain.setLocation(400, 200);
        frmMain.setLayout(null);

        m.jta= new JTextArea();
        JScrollPane jsp = new JScrollPane(m.jta);
        jsp.setBounds(150, 120, 300, 300);
        jsp.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        frmMain.add(jsp);
        m.jta.setEditable(false);

        JButton b1 = new JButton("加入群聊");
        JButton b2 = new JButton("发送");
        JLabel lblName = new JLabel("信息：");
        JTextArea txtName = new JTextArea("");
        txtName.setPreferredSize(new Dimension(80, 30));

        frmMain.add(lblName);
        frmMain.add(txtName);

        b1.setBounds(250, 30, 100, 50);
        b2.setBounds(450, 450, 100, 50);
        lblName.setBounds(100, 450, 50, 50);
        txtName.setBounds(150, 450, 270, 50);

        // 给按钮增加监听----加入群聊
        b1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                m.init();
                m.start();


                MessageInfor messageInfor=new MessageInfor();
                messageInfor.setType("0");
                messageInfor.setFlag(1);

                KeyPairGenerator senderKeyPairGenerator = null;
                try {
                    senderKeyPairGenerator = KeyPairGenerator.getInstance("DH");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                senderKeyPairGenerator.initialize(512);
                m.receiverKeyPair=senderKeyPairGenerator.generateKeyPair();
                receiverPublicKeyEnc =  m.receiverKeyPair.getPublic().getEncoded();//发送方公钥，发送给接收方（网络、文件。。。）
                System.out.println("作为第一个公钥是："+senderKeyPairGenerator);
                publicb=String.valueOf(receiverPublicKeyEnc);
                //发送第一个公钥
                long Ltimes = System.currentTimeMillis();
                messageInfor.setTime(Ltimes);
                messageInfor.setUserID(Ltimes);
                messageInfor.setByt(receiverPublicKeyEnc);

                System.out.println(receiverPublicKeyEnc);
                System.out.println("查看公钥的真面目：");
                String ss="";
                System.out.println(receiverPublicKeyEnc.length);
                for(int i=0;i< receiverPublicKeyEnc.length/2;i++){
                    ss=ss+receiverPublicKeyEnc[i]+" ";
                    System.out.print(receiverPublicKeyEnc[i]+" ");
                }

                messageInfor.setMsg("已发送公钥1:"+receiverPublicKeyEnc);
                m.send(messageInfor);//通过DatagramPacket发送对象




            }

        });
        //发送消息按钮
        b2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String content=txtName.getText();
                System.out.println("发送的消息为："+content);
                if(content != null && !content.trim().equals("")){
                    m.jta.append("我:"+content+"\n");
                    //获取Map中的所有key
                    Set<InetAddress> keySet = m.map.keySet();
                    //遍历存放所有key的Set集合
                    Iterator<InetAddress> it =keySet.iterator();
                    while(it.hasNext()){                         //利用了Iterator迭代器**
                        //得到每一个key
                        InetAddress key = it.next();
                        //通过key获取对应的value
                        SecretKey value = m.map.get(key);
                        System.out.println("遍历map得到："+value);
                        try {
                            cipher.init(Cipher.ENCRYPT_MODE,value);
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        }
                        byte[] result = new byte[0];
                        try {
                            result = cipher.doFinal(content.getBytes());
                            System.out.println("加密后 : " + result);
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                        }
                        long Ltimes = System.currentTimeMillis();
                        long mID=Ltimes;
                        MessageInfor mm = new MessageInfor(Ltimes,mID,result);//时间戳 ID 加密后的消息
                        mm.setType("1");
                        mm.setTime(Ltimes);
                        mm.setTime(mID);
                        mm.setMsg("加密消息为："+result);

                        m.send(mm);
                    }


                    txtName.setText("");
                }else
                {
                    m.jta.append("聊天内容不能为空"+"\n");
                }

            }

        });

        frmMain.add(b1);
        frmMain.add(b2);
        frmMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmMain.setVisible(true);

    }
}
