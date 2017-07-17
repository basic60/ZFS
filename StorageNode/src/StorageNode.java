import java.io.*;
import java.net.*;
import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageNode {
    String nodeName;
    String nodeIP;
    int nodePort;
    String rootDir;
    Volume volume;
    String fileServerIP;
    int fileServerPort;
    int fileServerHeatBeatPort;
    private String uuid;
    private final List<Runnable> taskList=new ArrayList<>();
    Properties prop;

    StorageNode(String configureFileName){
        try{
            InputStream in = new BufferedInputStream(new FileInputStream(configureFileName));
            prop=new Properties();
            prop.load(in);
            nodeName=(String) prop.get("NodeName");
            nodeIP=(String) prop.get("NodeIP");
            nodePort=Integer.parseInt((String) prop.get("NodePort"));
            rootDir=(String) prop.get("RootFolder");
            File tmp=new File(rootDir);
            if(!tmp.exists())
                tmp.mkdir();
            volume=new Volume((String) prop.get("Volume"));
            fileServerIP=(String)prop.get("FileServerIP");
            fileServerPort=Integer.parseInt(((String)prop.get("FileServerPort")));
            fileServerHeatBeatPort=Integer.parseInt(((String)prop.get("FileServerHeartBeatPort")));
            in.close();
            refreshVolume();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    private void refreshVolume(){
        Queue<File> queue=new LinkedList<>();
        queue.offer(new File(rootDir));
        while (!queue.isEmpty()){
            File[] flist=queue.poll().listFiles();
            for(File i:flist){
                if(i.isDirectory())
                    queue.offer(i);
                else
                    volume.minus(i.length());
            }
        }
        System.out.printf("[%s-volumeCheck] Available volume:%d bytes\n",nodeName,volume.getAvailableBytes());
    }

    /*
    *<========Use StTest.java to run multiple threads of Storage Node at once=================>
    */

    public static Map<String,String> mp=new HashMap<>();
    public static void main(String[] args){
        try{

            mp.put("1","storage1.properties");
            mp.put("2","storage2.properties");
            mp.put("3","storage3.properties");
            mp.put("4","storage4.properties");
            mp.put("5","storage5.properties");
            Scanner input = new Scanner(System.in);
            String arg=input.next();
            if(!mp.containsKey(arg)){
                System.out.println("Please input the correct command!");
                return;
            }
            else
                arg=mp.get(arg);

            StorageNode node=new StorageNode(arg);
            ExecutorService exec= Executors.newCachedThreadPool();
            node.taskList.add(new RegisterAndUpdate(node));
            node.taskList.add(new FileListener(node));
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
                length = Long.parseLong(len.substring(0, len.length() - 2)) * 1024 * 1024 * 1024;
                break;
            case "MB":
                length = Long.parseLong(len.substring(0, len.length() - 2)) * 1024 * 1024;
                break;
            case "KB":
                length = Long.parseLong(len.substring(0, len.length() - 2)) * 1024;
                break;
            default:
                length = Long.parseLong(len.substring(0, len.length() - 1));
                break;
        }
        available=length;
    }

    void minus(long val){
        available-=val;
    }

    void setAvailableBytes(long val){available=val;}

    long getTotalBytes(){
        return length;
    }

    long getAvailableBytes(){
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

//To notify the server that this node is available and update the volume information.
class RegisterAndUpdate implements Runnable{
    private StorageNode node;
    public RegisterAndUpdate(StorageNode stNode){
        node=stNode;
    }
    @Override
    public void run() {
        while (true){
            try {
                InetAddress add = InetAddress.getByName(node.fileServerIP);
                byte[] buf=String.format("%s\n%s\n%d\n%d\n%d",
                        node.nodeName,node.nodeIP,node.nodePort,node.volume.getTotalBytes(),
                        node.volume.getAvailableBytes()).getBytes();
                DatagramPacket dp=new DatagramPacket(buf,buf.length,add,node.fileServerHeatBeatPort);
                DatagramSocket dc=new DatagramSocket();
                dc.send(dp);
                dc.close();
              //  System.out.printf("[%s-Update] Update volume and send heart beat packet.\n",node.nodeName);
                Thread.sleep(2000);
            } catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}