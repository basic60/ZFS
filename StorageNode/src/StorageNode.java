import javax.jnlp.FileOpenService;
import java.io.*;
import java.net.*;
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
    public static void main(String[] args){
        try{
            if(args.length!=1){
                System.out.println("Error: Please input the configure file name name!!!");
                return;
            }
            StorageNode node=new StorageNode(args[0]);
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

class FileListener implements Runnable {
    private final int FilePort;
    private final String rootDir;
    private final String nodeName;
    private final Volume volume;
    private final String fileServerIP;
    private final Properties prop;
    private final int FILEOP_PORT=30003;
    private ServerSocket ss;
    Socket soc;
    BufferedReader br;
    FileListener(StorageNode a) throws IOException
    {
        FilePort=a.nodePort;rootDir=a.rootDir;nodeName=a.nodeName;
        volume=a.volume;prop=a.prop;fileServerIP=a.fileServerIP;
        ss=new ServerSocket(FilePort);
    }

    private void notifyUploadFileSize(String uuid,long size){
        InetAddress add = null;
        while (true){
            try {
                add = InetAddress.getByName(fileServerIP);
                byte[] buffer=String.format("upload\n%s\n%d",uuid,size).getBytes();
                DatagramPacket dp=new DatagramPacket(buffer,buffer.length,add,FILEOP_PORT);
                DatagramSocket dc=new DatagramSocket();
                dc.send(dp);
                dc.close();
                break;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    //Upload the file.
    void upload(){
        try{
            String forwardTable = br.readLine();
            String uuid = br.readLine();

            System.out.printf("[%s-file] Start receiving file\n", nodeName);
            System.out.printf("[%s-file] The uuid of this file is : %s\n", nodeName, uuid);
            System.out.printf("[%s-file] Backup Node : %s\n", nodeName, forwardTable);

            //Read binary data of the file.
            BufferedInputStream bs = new BufferedInputStream(soc.getInputStream());

            //Save file to the disk.
            FileOutputStream fout = new FileOutputStream(new File(rootDir, uuid));
            int tmp;
            long cnt = 0;
            while ((tmp = bs.read()) != -1) {
                fout.write(tmp);
                cnt++;
            }
            fout.flush();
            volume.minus(cnt);
            System.out.printf("[%s-file] Save file finished. Total %d bytes.\n",nodeName,cnt);

            bs.close();
            fout.close();
            soc.close();
            br.close();

            //Forward the file to the backup storage node.
            if (!forwardTable.equals("null"))
            {
                notifyUploadFileSize(uuid,cnt);
                forward(uuid,forwardTable);
                System.out.printf("[%s-file] Forward file finished.\n",nodeName);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    //Forward the file to the server.
    void forward(String uuid,String forwardTable){
        Socket socket;
        String[] arr=forwardTable.split(" ");
        try{
            while (true) {
                System.out.printf("[%s-forward] Start forwarding to %s\n", nodeName, forwardTable);
                System.out.printf("[%s-forward] Try connecting to the server at %s\n",nodeName,forwardTable);
                socket = new Socket(arr[0], Integer.valueOf(arr[1]));
                System.out.printf("[%s-forward] Connect to the server successfully\n",nodeName);
                PrintStream out = new PrintStream(socket.getOutputStream());
                out.println("upload");
                out.println("null");
                out.println(uuid);

                BufferedInputStream bin = new BufferedInputStream(new FileInputStream(new File(rootDir,uuid)));
                BufferedOutputStream bout=new BufferedOutputStream(socket.getOutputStream());
                int tmp;
                while ((tmp = bin.read()) != -1) {
                    bout.write(tmp);
                }
                bout.flush();

                out.close();
                bout.close();
                socket.close();
                break;
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    void download(String uuid){
        try {
            FileInputStream fin=new FileInputStream(new File(rootDir,uuid));
            Socket soc=
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.printf("[%s] Listen at port %d\n", nodeName, FilePort);
                soc = ss.accept();
                soc.setSoTimeout(5000);

                //Read the uuid and the forward table.
                br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                String commmand = br.readLine();
                if (commmand.equals("upload"))
                    upload();
                else if(commmand.equals("download"))
                    download(br.readLine());
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }

        }
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
               // System.out.printf("[%s-Update] Update volume and send heart beat packet.\n",node.nodeName);
                Thread.sleep(5000);
            } catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}