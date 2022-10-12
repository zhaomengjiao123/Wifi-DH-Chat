import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 26910;
    public String ip;//虚假ip
    public int type;//0公钥 1消息
    public int flag;//1第一个公钥 2第二个公钥
    public byte[] byt;
    public Message(){

    }



    public void setIp( String ip) {
        this.ip = ip;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public byte[] getByt() {
        return byt;
    }

    public void setByt(byte[] byt) {
        this.byt = byt;
    }
}
