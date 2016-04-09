package com.breadwallet.tools;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by Mihail on 4/7/16.
 */
public class TypesConverter {

    private TypesConverter(){};

    public static byte[] intToBytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(x);
        return buffer.array();
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    public static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
//        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
//        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static char[] lowerCaseCharArray(char[] arr){
        char[] lowerPhrase = new char[arr.length];
        for(int i = 0; i < arr.length; i++){
            lowerPhrase[i] = Character.toLowerCase(arr[i]);
        }
        return lowerPhrase;
    }

    public static char[] toChars(byte[] arr) {
        char[] charArray = new char[arr.length];
        for(int i = 0; i < arr.length; i++)
            charArray[i] = (char) arr[i];
        return charArray;
    }
}
