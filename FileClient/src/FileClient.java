import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public class FileClient {
    private static final String FILE_SERVER_IP="127.0.0.1";
    private static final int FILE_SERVER_PORT=30002;
    private static Socket socket=null;
    private static final String passwd="askjhKAHLKLakj329453=43564871cs2d234g*/*/6/'";

    //Calculate the md5 of the file.
    static String getMD5(String fpath) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            BufferedInputStream br=new BufferedInputStream(new FileInputStream(fpath));

            int tmp;int cnt=0;
            ArrayList<Byte> arr=new ArrayList<>();
            while ((tmp=br.read())!=-1){
                cnt++;
                arr.add((byte) tmp);
                if(cnt==1024){
                    byte[] res=new byte[cnt];
                    for(int i=0;i!=cnt;i++) res[i]=arr.get(i);
                    md5.update(res);
                    arr.clear();
                    cnt=0;
                }
            }
            if(cnt>0){
                byte[] res=new byte[cnt];
                for(int i=0;i!=cnt;i++) res[i]=arr.get(i);
                md5.update(res);
            }

            br.close();
            BigInteger i = new BigInteger(1, md5.digest());
            return i.toString(16);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void upload(String fpath) throws InterruptedException {
        String nodeBack;
        String nodeMain;
        String uuid;
        PrintStream out;
        while (true) try {
            File tmp = new File(fpath);
            String fname = tmp.getName();
            long flen = tmp.length();

            String md5Value = getMD5(fpath);
            System.out.printf("The MD5 of this file is: %s\n", md5Value);
            out = new PrintStream(socket.getOutputStream());

            out.println(md5Value);
            out.println(fname);
            out.println(flen);

            //Judge if the file has been uploaded to the storage node yet.If so,don't upload it.
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            nodeMain = br.readLine();
            if (nodeMain.equals("exist")) {
                System.out.println("This file has been sent to the server yet!");
                br.close();
                out.close();
                socket.close();
                return;
            }
            nodeBack = br.readLine();
            uuid = br.readLine();

            out.close();
            br.close();
            socket.close();

            System.out.printf("The uuid of this file is %s. Start uploading......\n", uuid);
            String[] arr = nodeMain.split(" ");

            //Connect to the storage node.Send file uuid and the forward table to it.
            System.out.printf("Connecting to the Storage Node %s:%s\n", arr[0], arr[1]);
            socket = new Socket(arr[0], Integer.valueOf(arr[1]));
            out = new PrintStream(socket.getOutputStream());
            out.println("forward " + nodeBack);
            out.println(uuid);
            out.flush();

            //Encrypt and send the file
            System.out.println("Encrypting and uploading.Please wait...............");
            InputStream is = new FileInputStream(tmp);
            BufferedOutputStream bout = new BufferedOutputStream(new GZIPOutputStream(socket.getOutputStream()));
            int cnt=0;int tot=0;int val;byte[] fdata=new byte[1024];
            while ((val=is.read())!=-1){
                tot++;
                fdata[cnt++]=(byte) val;
                if(cnt==1024)
                {
                    fdata = aesEncrypt(fdata);
                    bout.write(fdata, 0, cnt);
                    bout.flush();
                    cnt=0;
                }
            }
            if(cnt>0)
                bout.write(fdata,0,cnt);
            bout.flush();
            System.out.printf("Uploading finished! The original file size is %d bytes\n",tot);

            bout.close();
            out.close();
            socket.close();
            break;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Thread.sleep(3000);
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
            KeyGenerator keygen=KeyGenerator.getInstance("AES");
            keygen.init(128,new SecureRandom(passwd.getBytes()));
            SecretKey seckey=keygen.generateKey();
            byte[] encode=seckey.getEncoded();
            SecretKey key=new SecretKeySpec(encode,"AES");

            Cipher cipher=Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE,key);
            byte[] result=cipher.doFinal(content);
            return result;
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        try{
            connect();
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
            }
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}

