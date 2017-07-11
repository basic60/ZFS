import javax.annotation.processing.SupportedSourceVersion;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class FileClient {
    private static final String FILE_SERVER_IP="127.0.0.1";
    private static final int FILE_SERVER_PORT=30002;
    private static Socket socket=null;
    private static final String passwd="askjhKAHLKLakj329453=43564871cs2d234g*/*/6/'";

    static String getMD5(String fpath) {
        try {
            FileInputStream fs = new FileInputStream(fpath);
            int len = fs.available();
            System.out.println(len);
            byte[] tmp = new byte[len];
            fs.read(tmp, 0, len);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(tmp);
            BigInteger i = new BigInteger(1, md5.digest());
            return i.toString(16);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void upload(String fpath) throws IOException {
        String md5Value = getMD5(fpath);
        System.out.printf("The MD5 of this file is: %s\n",md5Value);
        PrintStream out=new PrintStream(socket.getOutputStream());
        out.println(md5Value);
        BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String nodeMain=br.readLine();
        String nodeBack=br.readLine();
        String uuid=br.readLine();
        out.close();
        br.close();
        socket.close();

        String[] arr=nodeMain.split(" ");
        while (true){
            System.out.printf("Connecting to the Storage Node %s:%s\n",arr[0],arr[1]);
            socket=new Socket(arr[0],Integer.valueOf(arr[1]));
            out=new PrintStream(socket.getOutputStream());
            out.println("forward "+nodeBack);
            out.println(uuid);

        }

    }

    static void download(String uuid){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void delete(String uuid){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void show(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void connect(){
        while (true){
            System.out.printf("Trying connecting to the server %s:%d\n",FILE_SERVER_IP,FILE_SERVER_PORT);
            try {
                socket=new Socket(FILE_SERVER_IP,FILE_SERVER_PORT);
                System.out.println("Connecting to the server successfully!");
                break;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.out.println("retrying.....");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    static byte[] aesEncrypt(byte[] content){
        try {
            System.out.println("Start encrypting by using AES-128.............");
            KeyGenerator keygen=KeyGenerator.getInstance("AES");
            keygen.init(128,new SecureRandom(passwd.getBytes()));
            SecretKey seckey=keygen.generateKey();
            byte[] encode=seckey.getEncoded();
            SecretKey key=new SecretKeySpec(encode,"AES");

            Cipher cipher=Cipher.getInstance("AES");
            System.out.println();
            cipher.init(Cipher.ENCRYPT_MODE,key);
            byte[] result=cipher.doFinal(content);
            for(byte i:result){
                System.out.print(i+" ");
            }
            System.out.println("Encrypting finished!") ;
            return result;
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
/*        connect();
        if(args.length==1&&args[0].equals("show")){
            show();
        }
        else if(args.length==2&&args[0].equals("upload")){
            upload(args[1]);
        }
        else if(args.length==2&&args[0].equals("download")){
            download(args[1]);
        }
        else if(args.length==2&&args[0].equals("delete")){
            delete(args[1]);
        }
        else {
            System.out.println("Please input the correct number of parameters!");
        }*/
    }
}

