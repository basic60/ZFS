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
    String rootDir;
    private Volume volume;
    String fileServerIP;
    int fileServerPort;
    int fileServerHeatBeatPort;
    private String uuid;
    private final List<Runnable> taskList=new ArrayList<>();

    public StorageNode(String configureFileName){
        try{
            InputStream in = new BufferedInputStream(new FileInputStream(configureFileName));
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
                System.out.printf("[%s] trying to connect to the server %s:%d....\n",nodeName,fileServerIP,fileServerPort);
                Socket soc=new Socket(fileServerIP,fileServerPort);

                PrintStream out=new PrintStream(soc.getOutputStream());
                printInfo(out);

                InputStream is=soc.getInputStream();
                BufferedReader bw=new BufferedReader(new InputStreamReader(is));
                uuid=bw.readLine();
                File tmp=new File(rootDir,uuid);
                if(!tmp.exists()){
                    if(tmp.mkdir())
                        System.out.printf("[%s] Creating directory %s successfully\n",nodeName,uuid);
                }
                rootDir=".\\"+uuid;
                is.close();
                bw.close();
                out.close();
                soc.close();
                System.out.printf("[%s] Register to the File Server %s:%d successfully.\n",nodeName,fileServerIP,fileServerPort);
                System.out.printf("[%s] The root directory is %s\n",nodeName,rootDir);
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
            node.registerToServer();
            ExecutorService exec= Executors.newCachedThreadPool();
            node.taskList.add(new HeartBeatSender(node));
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
                InetAddress add = InetAddress.getByName(info.fileServerIP);

                byte[] buf = (info.nodeName + " ok").getBytes();
          //      System.out.printf("[%s] sending heart beats.\n",info.nodeName);
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
    private final int FilePort;
    private final String rootDir;
    private final String nodeName;
    private ServerSocket ss;
    FileListener(StorageNode a) throws IOException
    {
        FilePort=a.nodePort;rootDir=a.rootDir;nodeName=a.nodeName;
        ss=new ServerSocket(FilePort);
    }
    @Override
    public void run() {
        while (true) {
            try {
                System.out.printf("[%s] Listen at port %d\n", nodeName, FilePort);
                Socket soc = ss.accept();
                soc.setSoTimeout(5000);

                //Read the uuid and the forward table.
                BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
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
                int cnt = 0;
                while ((tmp = bs.read()) != -1) {
                    fout.write(tmp);
                    cnt++;
                }
                fout.flush();
                System.out.printf("[%s-file] Save file finished. Total %d bytes.\n",nodeName,cnt);

                bs.close();
                fout.close();
                soc.close();
                br.close();

                //Forward the file to the backup storage node.
                if (!forwardTable.equals("null"))
                {
                    forward(uuid,forwardTable);
                    System.out.printf("[%s-file] Forward file  finished.\n",nodeName);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    void forward(String uuid,String forwardTable){
        Socket soc;
        String[] arr=forwardTable.split(" ");
        try{
            while (true) {
                System.out.printf("[%s-forward] Start forwarding to %s\n", nodeName, forwardTable);
                System.out.printf("[%s-forward] Try connecting to the server at %s\n",nodeName,forwardTable);
                soc = new Socket(arr[0], Integer.valueOf(arr[1]));
                System.out.printf("[%s-forward] Connect to the server successfully\n",nodeName);
                PrintStream out = new PrintStream(soc.getOutputStream());
                out.println("null");
                out.println(uuid);

                BufferedInputStream bin = new BufferedInputStream(new FileInputStream(new File(rootDir,uuid)));
                BufferedOutputStream bout=new BufferedOutputStream(soc.getOutputStream());
                int tmp;
                while ((tmp = bin.read()) != -1) {
                    bout.write(tmp);
                }
                bout.flush();

                out.close();
                bout.close();
                soc.close();
                break;
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }

    }
}