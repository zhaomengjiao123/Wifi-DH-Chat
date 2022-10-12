public class Utils {

    public static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    //bytes 转 Hex 字符 方便打印
    public static String bytesToHex(byte[] paramArrayOfByte)
    {
        int i = 0;
        char[] arrayOfChar = new char[2 * paramArrayOfByte.length];
        for (int j = 0; j < paramArrayOfByte.length; j++)
        {
            int k = 0xFF & paramArrayOfByte[j];
            arrayOfChar[(j * 2)] = HEX_ARRAY[(k >>> 4)];
            arrayOfChar[(1 + j * 2)] = HEX_ARRAY[(k & 0xF)];
        }
        StringBuilder localStringBuilder = new StringBuilder();
        while (i < arrayOfChar.length)
        {
            localStringBuilder.append("0x").append(arrayOfChar[i]).append(arrayOfChar[(i + 1)]).append(" ");
            i += 2;
        }
        return localStringBuilder.toString();
    }



}
