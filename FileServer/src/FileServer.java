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
            ser.taskList.add(new GUIStorageNodeNotifier());
            ser.taskList.add(new GUIFileNotifier());
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

class FileServerException extends Exception {
    public FileServerException(){}
    public FileServerException(String msg){
        super(msg);
    }
}

