package org.keyspring.poc.spring.vault.utility;

import java.io.*;

/**
 * Created by Subhankar on 8/17/2017.
 */
public class KeySpringUtilily {

    public static byte[] convertToBytes(Object object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public static Object convertFromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInput in = new ObjectInputStream(bis);
            return in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }  catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
