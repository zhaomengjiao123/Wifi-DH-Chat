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


public class dhclient3 {
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
    String ownIp="1.1.1.3";
    Map<String, SecretKey> map= new HashMap<String,SecretKey>();


    public dhclient3() {
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
    public void sendPublic() throws NoSuchAlgorithmException {

        Message ms=new Message();
        ms.setIp(ownIp);
        ms.setType(0);
        ms.flag=2;
        ms.setByt(receiverPublicKeyEnc);
        this.send(ms);//通过DatagramPacket发送对象
    }
    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    // 排除loopback回环类型地址（不管是IPv4还是IPv6 只要是回环地址都会返回true）
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了 就是我们要找的
                            // ~~~~~~~~~~~~~绝大部分情况下都会在此处返回你的ip地址值~~~~~~~~~~~~~
                            return inetAddr;
                        }

                        // 若不是site-local地址 那就记录下该地址当作候选
                        if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }

                    }
                }
            }

            // 如果出去loopback回环地之外无其它地址了，那就回退到原始方案吧
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
                        Message message;
                        ByteArrayInputStream bint=new ByteArrayInputStream(buf);
                        ObjectInputStream oint=new ObjectInputStream(bint);
                        message=(Message) oint.readObject();

                        System.out.println(message.type);
                        System.out.println(message.getByt());
                        String ip = message.ip;//接受消息发送方的ip地址

                        InetAddress localHost = getLocalHostExactAddress();



                        System.out.println(ip);
                        System.out.println(ownIp);
                        if(message.type==0){
                            System.out.println("ooo");
                            System.out.println(message.byt);
                            if(message.flag==2){
                                KeyFactory receiverKeyFactory = KeyFactory.getInstance("DH");
                                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(message.byt);
                                PublicKey receiverPublicKey = receiverKeyFactory.generatePublic(x509EncodedKeySpec);

//                            DHParameterSpec dhParameterSpec = ((DHPublicKey)receiverPublicKey).getParams();
//                            KeyPairGenerator receiverKeyPairGenerator = KeyPairGenerator.getInstance("DH");
//                            receiverKeyPairGenerator.initialize(dhParameterSpec);
//                            KeyPair receiverKeypair = receiverKeyPairGenerator.generateKeyPair();
//                            PrivateKey receiverPrivateKey = receiverKeypair.getPrivate();
//                            receiverPublicKeyEnc = receiverKeypair.getPublic().getEncoded();
                                KeyAgreement receiverKeyAgreement = KeyAgreement.getInstance("DH");
                                receiverKeyAgreement.init(receiverKeyPair.getPrivate());
                                receiverKeyAgreement.doPhase(receiverPublicKey, true);
                                receiverDesKey = receiverKeyAgreement.generateSecret("DES");
                                cipher = Cipher.getInstance("DES");
                                map.put(ip,receiverDesKey);
                                System.out.println(ip+": "+receiverDesKey);
                            }else{
                                KeyFactory receiverKeyFactory = KeyFactory.getInstance("DH");
                                System.out.println("message.byt: "+message.byt);
                                //System.out.println("string : "+message.byt.toString());
                                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(message.byt);
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
                                map.put(ip,receiverDesKey);
                                System.out.println(ip+": "+receiverDesKey);
                                //cipher.init(Cipher.ENCRYPT_MODE, receiverDesKey);
                                sendPublic();
                            }




                        }else if(!(ownIp).equals(ip)&&map.get(ip)!=null&&message.type==1){
                            System.out.println("client收到消息");
                            cipher.init(Cipher.DECRYPT_MODE, map.get(ip));
                            buf = cipher.doFinal(message.byt);
                            String a=new String(buf);
                            System.out.println("解密后 : " + a);
                            System.out.println("收到的解密消息为："+a);
                            String info = ip+": " + a + "\r\n";
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

    public void send(Message message){
        try{
            // byte[] buf = message.getBytes("UTF-8"); //发送信息
            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(message);        //序列化对象
            oout.flush();
            //oout.close();
            byte[] buf=bout.toByteArray();
            InetAddress inetAddress = InetAddress.getByName(groupHost); //组播地址
            DatagramPacket datagramPacket= new DatagramPacket(buf, buf.length, inetAddress, port); //发送数据报
            datagramSocket.send(datagramPacket); //发送数据
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
        dhclient3 m = new dhclient3();

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
//        b1.setBounds(250, 100, 100, 50);
//        b2.setBounds(450, 300, 100, 50);
//        lblName.setBounds(100, 300, 50, 50);
//        txtName.setBounds(200, 300, 200, 50);
        b1.setBounds(250, 30, 100, 50);
        b2.setBounds(450, 450, 100, 50);
        lblName.setBounds(100, 450, 50, 50);
        txtName.setBounds(150, 450, 270, 50);

        // 给按钮增加监听
        b1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                m.init();
                m.start();
                Message ms=new Message();
                ms.setIp(m.ownIp);
                ms.setType(0);
                ms.flag=1;
                KeyPairGenerator senderKeyPairGenerator = null;
                try {
                    senderKeyPairGenerator = KeyPairGenerator.getInstance("DH");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                senderKeyPairGenerator.initialize(512);
                m.receiverKeyPair=senderKeyPairGenerator.generateKeyPair();
                receiverPublicKeyEnc =  m.receiverKeyPair.getPublic().getEncoded();//发送方公钥，发送给接收方（网络、文件。。。）
                System.out.println(senderKeyPairGenerator);
                publicb=String.valueOf(receiverPublicKeyEnc);
                ms.setByt(receiverPublicKeyEnc);
                m.send(ms);//通过DatagramPacket发送对象


            }

        });
        b2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String content=txtName.getText();
                System.out.println("发送的消息为："+content);
                if(content != null && !content.trim().equals("")){
                    m.jta.append("我:"+content+"\n");
                    //获取Map中的所有key
                    Set<String> keySet = m.map.keySet();
                    //遍历存放所有key的Set集合
                    Iterator<String> it =keySet.iterator();
                    while(it.hasNext()){                         //利用了Iterator迭代器**
                        //得到每一个key
                        String key = it.next();
                        //通过key获取对应的value
                        SecretKey  value = m.map.get(key);
                        System.out.println(value);
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
                        Message ms=new Message();
                        ms.setIp(m.ownIp);
                        ms.setType(1);
                        ms.setByt(result);
                        m.send(ms);
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
