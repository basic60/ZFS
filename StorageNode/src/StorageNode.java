import javax.annotation.processing.Filer;
import javax.jnlp.FileOpenService;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageNode {
    String nodeName;
    private String nodeIP;
    int nodePort;
    private String rootDir;
    private Volume volume;
    String fileServerIP;
    int fileServerPort;
    int fileServerHeatBeatPort;
    private String uuid;
    private final List<Runnable> taskList=new ArrayList<>();

    public StorageNode(String configureFileName){
        try{
            InputStream in = new BufferedInputStream(new FileInputStream("storage1.properties"));
            Properties prop=new Properties();
            prop.load(in);
            nodeName=(String) prop.get("NodeName");
            nodeIP=(String) prop.get("NodeIP");
            nodePort=Integer.parseInt((String) prop.get("NodePort"));
            rootDir=(String) prop.get("RootFolder");
            volume=new Volume((String) prop.get("Volume"));
            fileServerIP=(String)prop.get("FileServerIP");
            fileServerPort=Integer.parseInt(((String)prop.get("FileServerPort")));
            fileServerHeatBeatPort=Integer.parseInt(((String)prop.get("FileServerHeartBeatPort")));
            in.close();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    private void registerToServer() throws InterruptedException {
        while (true){
            try{
                System.out.printf("trying to connect to the server %s:%d....\n",fileServerIP,fileServerPort);
                Socket soc=new Socket(fileServerIP,fileServerPort);

                PrintStream out=new PrintStream(soc.getOutputStream());
                printInfo(out);

                InputStream is=soc.getInputStream();
                BufferedReader bw=new BufferedReader(new InputStreamReader(is));
                uuid=bw.readLine();
                File tmp=new File(rootDir,uuid);
                if(!tmp.exists()){
                    if(tmp.mkdir())
                        System.out.printf("Creating directory %s successfully\n",uuid);
                }

                is.close();
                bw.close();
                out.close();
                soc.close();
                System.out.printf("Register to the File Server %s:%d successfully.\n",fileServerIP,fileServerPort);
                break;
            }
            catch (IOException e){
                System.out.println(e.getMessage());
                Thread.sleep(3000);
                System.out.println("retry.....");
            }
        }
    }

    public void printInfo(PrintStream out){
        out.println(nodeName);out.println(nodeIP);out.println(nodePort);
        out.println(volume.getTotalBytes());out.println(volume.getAvailalblebytes());
        out.flush();
    }

    public static void main(String[] args){
        try{
            if(args.length!=1){
                System.out.println("Error: Please input the configure file name name!!!");
                return;
            }
            StorageNode node=new StorageNode(args[0]);
            node.registerToServer();
            ExecutorService exec= Executors.newCachedThreadPool();
            node.taskList.add(new HeartBeatSender(node));
            node.taskList.add(new FileListener(node))
            for(Runnable i:node.taskList){
                exec.submit(i);
            }
            exec.shutdown();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}

class Volume{
    private long length;

    private long available;

    Volume(String len){
        String flag=len.substring(len.length()-2).toUpperCase();
        switch (flag) {
            case "GB":
                length = Integer.parseInt(len.substring(0, len.length() - 2)) * 1024 * 1024 * 1024;
                break;
            case "MB":
                length = Integer.parseInt(len.substring(0, len.length() - 2)) * 1024 * 1024;
                break;
            case "KB":
                length = Integer.parseInt(len.substring(0, len.length() - 2)) * 1024;
                break;
            default:
                length = Integer.parseInt(len.substring(0, len.length() - 1));
                break;
        }
        available=length;
    }

    public long getTotalBytes(){
        return length;
    }

    public long getAvailalblebytes(){
        return available;
    }

    public static String volumeToString(int byteLength){
        if(byteLength<1024)
            return byteLength+"bytes";
        else if(byteLength<1024*1024)
            return (double)byteLength/1024+"KB";
        else if(byteLength<1024*1024*1024)
            return (double)byteLength/1024/1024+"MB";
        else
            return (double)byteLength/1024/1024/1024+"GB";
    }
}

class HeartBeatSender implements Runnable{
    private StorageNode info;
    HeartBeatSender(StorageNode i){info=i;}
    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("sdfs");
                InetAddress add = InetAddress.getByName(info.fileServerIP);

                byte[] buf = (info.nodeName + " ok").getBytes();
                System.out.printf("sening heart beats of %s",info.nodeName);
                DatagramPacket dp = new DatagramPacket(buf, buf.length, add, info.fileServerHeatBeatPort);
                DatagramSocket soc = new DatagramSocket();
                soc.send(dp);
                soc.close();
                Thread.sleep(10000);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class FileListener implements Runnable {
    ServerSocket ss=new ServerSocket();
    private final int FilePort;
    public FileListener(StorageNode a) throws IOException {FilePort=a.nodePort;}
    @Override
    public void run() {
        while (true){
            try {
                Socket soc=ss.accept();
                soc.setSoTimeout(5000);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}