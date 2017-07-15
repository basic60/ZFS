import sun.security.krb5.internal.KdcErrException;

import javax.annotation.processing.SupportedSourceVersion;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileClient {
    private static final String FILE_SERVER_IP="127.0.0.1";
    private static final int FILE_SERVER_PORT=30002;
    private static Socket socket=null;
    private static final String passwd="askjhKAHLKLakj329453=43564871cs2d234g*/*/6/'";
    private static final int ENCRYPT=1;
    private static final int DECRYPT=2;
    private static final String ROOT_DIR="d:\\zzh";

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
        try {
            File tmp = new File(fpath);
            String fname = tmp.getName();
            long flen = tmp.length();

            String md5Value = getMD5(fpath);
            System.out.printf("The MD5 of this file is: %s\n", md5Value);
            out = new PrintStream(socket.getOutputStream());

            out.println("upload");
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
            out.println("upload");
            out.println(nodeBack);
            out.println(uuid);
            out.flush();

            //Encrypt and send the file
            System.out.println("Encrypting and uploading.Please wait...............");
            InputStream is = new FileInputStream(tmp);
            BufferedOutputStream bout = new BufferedOutputStream(new GZIPOutputStream(socket.getOutputStream()));
            int cnt=0;int tot=0;int val;byte[] fdata=new byte[1024];byte[] res;
            while ((val=is.read())!=-1){
                tot++;
                fdata[cnt++]=(byte) val;
                if(cnt==1024)
                {
                    res = aes(fdata,ENCRYPT);
                    bout.write(res, 0, res.length);
                    bout.flush();
                    cnt=0;
                }
            }
            if(cnt>0)
            {
                res=new byte[cnt];
                for(int i=0;i!=cnt;i++) res[i]=fdata[i];
                res=aes(res,ENCRYPT);
              //  System.out.printf("The enc size is %d\n",res.length);
                bout.write(res,0,res.length);
            }
            bout.flush();
            System.out.printf("Uploading finished! The original file size is %d bytes\n",tot);

            bout.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Thread.sleep(3000);
        }
    }

    static void download(String uuid) {
        try {
            //Get where the file is stored on which storage node.
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.write("download\n");
            out.write(uuid+"\n");
            out.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String fileName=br.readLine();

            //Download from the storage node.
            if(fileName.equals("null")){
                System.out.println("Such file doesn't exist!");
                return;
            }
            else {
                int id=0;
                String mainNode=br.readLine();
                String backupNode=br.readLine();

                out.close();
                br.close();
                socket.close();
                System.out.printf(">>The main node of the file is %s\n",mainNode);
                System.out.printf(">>The backup node of the file is %s\n",backupNode);
                while (id++<2){
                    String ip=id==1?mainNode.split(" ")[0]:backupNode.split(" ")[0];
                    int port=id==1?Integer.parseInt(mainNode.split(" ")[1]):Integer.parseInt(backupNode.split(" ")[1]);
                    try{
                        socket=new Socket(ip,port);
                        System.out.printf("Connecting to the storage node %s:%d\n",ip,port);
                        socket.setSoTimeout(5000);
                        out=new PrintWriter(socket.getOutputStream());
                        out.println("download");
                        out.println(uuid);
                        out.flush();

                        //Start download the file.
                        OutputStream os = new FileOutputStream(new File(ROOT_DIR,fileName));
                        BufferedInputStream bin = new BufferedInputStream(new GZIPInputStream(socket.getInputStream()));
                        int cnt=0;int tot=0;int val;
                        byte[] fdata=new byte[1040];
                        while ((val=bin.read())!=-1){
                            tot++;
                            fdata[cnt++]=(byte) val;
                            if(cnt==1040){
                                byte[] res=aes(fdata,DECRYPT);
                                os.write(res,0,res.length);
                                os.flush();
                                cnt=0;
                            }
                        }
                        byte[] res;
                        if(cnt!=0)
                        {
                            System.out.printf("The cnt is %d \n",cnt);
                            byte[] mid=new byte[cnt];
                            for(int i=0;i!=cnt;i++) mid[i]=fdata[i];
                            res=aes(mid,DECRYPT);
                            os.write(res,0,res.length);
                        }
                        os.flush();
                        os.close();
                        out.close();
                        socket.close();
                        bin.close();
                        System.out.printf(">>Download file finished.Total %d bytes.\n",tot);
                        System.out.printf(">>Store at %s\\%s\n",ROOT_DIR,fileName);
                        break;
                    }
                    catch (IOException e){
                        System.out.println(e.getMessage());
                    }
                }
                if(id==3)
                    System.out.println("Download failed! Please retry after some periods.");
            }
        } catch (IOException e) {
           System.out.println(e.getMessage());
        }
    }

    static void delete(String uuid){
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
                socket.setSoTimeout(5000);
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

    static SecretKey key;

    static void generateKey(){
        KeyGenerator keygen= null;
        try {
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getCause());
        }
        keygen.init(128,new SecureRandom(passwd.getBytes()));
        SecretKey seckey=keygen.generateKey();
        byte[] encode=seckey.getEncoded();
        key=new SecretKeySpec(encode,"AES");
    }

    static byte[] aes(byte[] content,int mode){
        try {
            Cipher cipher=Cipher.getInstance("AES");
            if(mode==1)
                cipher.init(Cipher.ENCRYPT_MODE,key);
            else
                cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] result=cipher.doFinal(content);
            return result;
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        try{
            connect();
            if(args.length==2&&args[0].equals("upload")){
                generateKey();
                upload(args[1]);
            }
            else if(args.length==2&&args[0].equals("download")){
                generateKey();
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

