import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {
    static final int SERVER_HEART_PORT =30001;
    static final int SERVER_CLIENT_PORT=30002;
    static final int SERVER_FILEOP_PORT=30003;
    static final String SERVER_IP="127.0.0.1";

    static void writeTryingDeletingFile() {
        try {
            FileWriter fw = new FileWriter("TryDelete.dat");
            for (String i : FileServer.deleteFile) {
                fw.write(i + "\n");
            }
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static final List<StNodeInfo> stNodeList=new ArrayList<>();
    static List<FileInfo> fileList;

    //Format: uuid'\n'ip port
    static final List<String> deleteFile=new ArrayList<>();

    private List<Runnable> taskList;

    private FileServer(){
        taskList =new LinkedList<>();
    }

    private void init(){
        try{
            ObjectInputStream ism=new ObjectInputStream(new FileInputStream("FileInfo.dat"));
            fileList=(List<FileInfo>)ism.readObject();
            if(fileList!=null){
                System.out.println("[init] Load file info completely");
            }
            ism.close();

            //Read the try deleting list.
            BufferedReader fin=new BufferedReader(new FileReader("TryDelete.dat"));
            String uuid;StringBuilder sb=new StringBuilder();
            while ((uuid=fin.readLine())!=null){
                sb.append(uuid);sb.append('\n');sb.append(fin.readLine());
                deleteFile.add(sb.toString());
                System.out.printf("[init] file %s at node %s is added to the deleted trying list.\n",
                        sb.toString().split("\n")[0],sb.toString().split("\n")[1]);
                sb=new StringBuilder();
            }
        }
        catch (IOException | ClassNotFoundException e){
            ;
        } finally {
            if(fileList==null){
                fileList= new ArrayList<>();
            }
        }
    }

    public static void main(String[] args){
        try {
            FileServer ser=new FileServer();
            ser.init();
            ser.taskList.add(new HeartbeatListener());
            ser.taskList.add(new NodeAliveChecker());
            ser.taskList.add(new ClientListener());
            ser.taskList.add(new FileOperationListener());
            ser.taskList.add(new FileDeleter());
            ExecutorService exec= Executors.newCachedThreadPool();
            for(Object i:ser.taskList){
                exec.execute((Runnable) i);
            }
            exec.shutdown();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }

    }
}

class HeartbeatListener implements Runnable{
    @Override
    public void run() {
        while (true){
            byte[] buffer=new byte[1024];
            DatagramPacket datapacket=new DatagramPacket(buffer,buffer.length);
            try {
                InetAddress add=InetAddress.getByName(FileServer.SERVER_IP);
                DatagramSocket soc=new DatagramSocket(FileServer.SERVER_HEART_PORT,add);
                soc.receive(datapacket);
                soc.close();
                String msg=new String(buffer,0,datapacket.getLength());
                //System.out.println("Receive message:\n"+msg);

                boolean flag=false;
                String[] arr=msg.split("\n");
                StNodeInfo tmp=new StNodeInfo(arr[0],arr[1],Integer.parseInt(arr[2]),Long.parseLong(arr[3]), Long.parseLong(arr[4]));
                tmp.lastVis=System.currentTimeMillis();
                for(StNodeInfo i: FileServer.stNodeList){
                    if(i.nodeName.equals(arr[0])){
                        i.lastVis=tmp.lastVis;
                        i.volume.setAvailableBytes(tmp.volume.getAvailableBytes());
                        flag=true;
                    }
                }
                if(!flag)
                    FileServer.stNodeList.add(tmp);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

class NodeAliveChecker implements Runnable{
    int last=-1;
    @Override
    public void run() {
        while (true) {
            long cutime = System.currentTimeMillis();
            boolean flag;
            do {
                flag = false;
                if(FileServer.stNodeList.size()!=last){
                    System.out.printf("Active Storage Node number : %d\n", FileServer.stNodeList.size());
                    last= FileServer.stNodeList.size();
                }
                for (int i = 0; i != FileServer.stNodeList.size(); i++) {
                    if (cutime - FileServer.stNodeList.get(i).lastVis > 15000) {
                        System.out.printf("'%s' %s:%s is not active! It's removed From the file server!\n",
                                FileServer.stNodeList.get(i).nodeName, FileServer.stNodeList.get(i).nodeIP, FileServer.stNodeList.get(i).nodePort);
                        FileServer.stNodeList.remove(i);
                        flag = true;
                        break;
                    }
                }
            } while (flag);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                Thread.yield();
            }
        }
    }
}

class ClientListener implements Runnable {
    private ServerSocket ss = new ServerSocket(FileServer.SERVER_CLIENT_PORT);
    private Socket s;
    private InputStream is;
    private OutputStream os;
    BufferedReader br;
    ClientListener() throws IOException {
    }

    private void upload(){
        try {
            String md5 = br.readLine();
            String fname = br.readLine();
            String flen = br.readLine();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            if (FileServer.fileList != null) {
                for (FileInfo i : FileServer.fileList) {
                    if (i.md5.equals(md5) && i.actualLength != 0) {
                        bw.write("exist\n");
                        bw.close();
                        br.close();
                        is.close();
                        os.close();
                        s.close();
                        return;
                    }
                }
            }

            Collections.sort(FileServer.stNodeList);
            if (FileServer.stNodeList.size() >= 2) {
                String mNode = FileServer.stNodeList.get(0).nodeIP + " " + FileServer.stNodeList.get(0).nodePort;
                String bNode = FileServer.stNodeList.get(1).nodeIP + " " + FileServer.stNodeList.get(1).nodePort;
                bw.write(mNode + "\n");
                bw.write(bNode + "\n");

                String uuid = UUID.randomUUID().toString();

                System.out.println("Receiving file uploading request.");
                System.out.printf(">>File Name: %s\n", fname);
                System.out.printf(">>File length: %s\n", flen);
                System.out.printf(">>MD5 of this file: %s\n", md5);
                System.out.printf(">>UUID of this file: %s\n", uuid);
                System.out.printf(">>Allocating main Storage Node %s to the client.\n", mNode);
                System.out.printf(">>Allocating backup Storage Node %s to the client.\n", bNode);
                FileServer.fileList.add(new FileInfo(fname, uuid, Long.parseLong(flen), mNode, bNode, md5));

                bw.write(uuid + "\n");

                bw.close();
            } else {
                throw new FileServerException("Not enough Storage node!");
            }
        }
        catch (IOException | FileServerException e){
            System.out.println(e.getMessage());
        }
    }

    private void download(){
        try {
            String uuid = br.readLine();
            StringBuilder res=new StringBuilder();
            for(FileInfo i: FileServer.fileList){
                if(i.uuid.equals(uuid)){
                    res.append(i.fileName);res.append("\n");
                    res.append(i.mainNode);res.append("\n");res.append(i.backupNode);
                    break;
                }
            }
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(os));
            bw.write(res.length()>0?res.toString():"null");
            bw.flush();
            bw.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    //Add the file to the delete trying list.

    private void  delete(){
        try{
            String uuid=br.readLine();
            int index=-1;
            synchronized (FileServer.fileList){
                for(int i = 0; i!= FileServer.fileList.size(); i++){
                    if(FileServer.fileList.get(i).uuid.equals(uuid)){
                        index=i;
                        System.out.printf("%s is added to the delete trying list.",uuid);
                        FileServer.deleteFile.add(uuid+"\n"+ FileServer.fileList.get(i).mainNode);
                        FileServer.deleteFile.add(uuid+"\n"+ FileServer.fileList.get(i).backupNode);
                        break;
                    }
                }
                FileServer.writeTryingDeletingFile();
            }

            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(os));
            if(index==-1)
                bw.write("null");
            else
            {
                bw.write("suc");
            }
            bw.flush();
            bw.close();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                s = ss.accept();
                s.setSoTimeout(5000);
                is = s.getInputStream();
                os = s.getOutputStream();
                br = new BufferedReader(new InputStreamReader(is));

                String command = br.readLine();
                if (command.equals("upload"))
                    upload();
                else if(command.equals("download"))
                    download();
                else if(command.equals("delete")){
                    delete();
                }

                br.close();
                is.close();
                os.close();
                s.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

class FileServerException extends Exception {
    public FileServerException(){}
    public FileServerException(String msg){
        super(msg);
    }
}

class FileOperationListener implements Runnable{
    @Override
    public void run() {
        while (true){
            byte[] buffer=new byte[1024];
            DatagramPacket datapacket=new DatagramPacket(buffer,buffer.length);
            try {
                InetAddress add = InetAddress.getByName(FileServer.SERVER_IP);
                DatagramSocket soc = new DatagramSocket(FileServer.SERVER_FILEOP_PORT, add);
                soc.receive(datapacket);
                soc.close();
                String msg=new String(buffer,0,datapacket.getLength());
                String[] arr=msg.split("\n");

                if(arr[0].equals("upload")){
                    for(FileInfo i: FileServer.fileList)
                        if(arr[1].equals(i.uuid))
                        {
                            i.actualLength=Long.parseLong(arr[2]);
                            System.out.printf("[Upload] The actual length of the file %s is %d bytes.\n",i.uuid,i.actualLength);
                        }
                }
                else if(arr[0].equals("delete")){
                    String uuid=arr[1];
                    String host=arr[2];
                    int id=-1;
                    synchronized (FileServer.fileList){
                        for(int i = 0; i!= FileServer.fileList.size(); i++){
                            if(FileServer.fileList.get(i).uuid.equals(uuid)){
                                id=i;
                                System.out.printf("[Delete] Remove the file info of %s.\n",uuid);
                                break;
                            }
                        }
                        if(id!=-1)
                            FileServer.fileList.remove(id);
                    }

                    synchronized (FileServer.deleteFile)
                    {
                        id=-1;
                        for(int i=0;i<FileServer.deleteFile.size();i++){
                            if(FileServer.deleteFile.get(i).split("\n")[0].equals(uuid)&&
                                    FileServer.deleteFile.get(i).split("\n")[1].equals(host)){
                                id=i;
                                System.out.printf("[Delete] Remove the file info %s on server %s in delete trying table.\n",uuid,host);
                                break;
                            }
                        }
                        if(id!=-1)
                            FileServer.deleteFile.remove(id);
                        FileServer.writeTryingDeletingFile();
                    }
                }
                writeFileInfo();
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    private void writeFileInfo() throws IOException {
        ObjectOutputStream osm =new ObjectOutputStream(new FileOutputStream("FileInfo.dat"));
        osm.writeObject(FileServer.fileList);
        osm.flush();osm.close();
    }
}

class FileDeleter implements Runnable{
    @Override
    public void run() {
        while (true) {
            synchronized (FileServer.deleteFile){
                for (int i = 0; i < FileServer.deleteFile.size(); i++) {
                    String[] arr = FileServer.deleteFile.get(i).split("\n");
                    String uuid = arr[0];
                    String info = arr[1];
                    sendDeleteInfo(uuid, info.split(" ")[0], Integer.parseInt(info.split(" ")[1]));
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void sendDeleteInfo(String uuid,String ip,int port){
        try {
            Socket soc=new Socket(ip,port);
            soc.setSoTimeout(3000);
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
            bw.write("delete\n");
            bw.write(uuid+"\n");
            bw.flush();
            bw.close();
            soc.close();
            System.out.printf("Sending file deletion info of %s to %s:%d completely.\n",uuid,ip,port);
        } catch (IOException e) {
            System.out.println("Send delete info error: "+e.getMessage());
        }

    }
}