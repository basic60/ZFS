import javax.annotation.processing.Filer;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FIleServer {
    public static final int SERVER_PORT_NUM=30000;
    public static final int SERVER_HEART_PORT =30001;
    public static final String SERVER_IP="127.0.0.1";
    public static final Map <String,String> registeredStNode=new HashMap<>();
    public static final List<StNodeInfo> stNodeList=new ArrayList<>();

    public static void updateStNodeInfo(StNodeInfo oldn,StNodeInfo newb){
        oldn.nodeIP=newb.nodeIP;
        oldn.nodePort=newb.nodePort;
        oldn.volume=newb.volume;
        oldn.uuid=newb.uuid;
    }

    private List<Runnable> taskList;

    FIleServer(){
        taskList =new LinkedList();
    }

    void LoadStNode(){
        try{
            BufferedReader br=new BufferedReader(new FileReader("RegisteredNode.txt"));
            String line;
            while ((line=br.readLine())!=null){
                String[] arr=line.split(" ");
                registeredStNode.put(arr[0],arr[1]);
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        FIleServer ser=new FIleServer();
        ser.LoadStNode();
        ser.taskList.add(new NodeRegisterListener());
        ser.taskList.add(new HeartbeatListener());
        ser.taskList.add(new NodeAliveChecker());
        ExecutorService exec= Executors.newCachedThreadPool();
        for(Object i:ser.taskList){
            exec.execute((Runnable) i);
        }
        exec.shutdown();
    }
}

class NodeRegisterListener implements Runnable {
    private ServerSocket ss = new ServerSocket(FIleServer.SERVER_PORT_NUM);

    NodeRegisterListener() throws IOException {
    }

    void writeBack() throws IOException {
        BufferedWriter bw=new BufferedWriter(new FileWriter("RegisteredNode.txt"));;
        for(Map.Entry<String, String> i:FIleServer.registeredStNode.entrySet()){
            System.out.println(i.getKey()+" "+i.getValue());
            bw.write(i.getKey()+" "+i.getValue()+"\n");
        }
        bw.flush();
        bw.close();
    }

    @Override
    public void run() {
        while (true){
            Socket s;
            InputStream is;OutputStream os;
            try{
                System.out.println("Listening...............");
                s=ss.accept();
                s.setSoTimeout(3000);
                is=s.getInputStream();
                os=s.getOutputStream();

                BufferedReader br=new BufferedReader(new InputStreamReader(is));
                StNodeInfo tmp=new StNodeInfo();
                tmp.nodeName=br.readLine();
                tmp.nodeIP=br.readLine();
                tmp.nodePort=Integer.parseInt(br.readLine());
                tmp.volume=new Volume(Long.parseLong(br.readLine()),Long.parseLong(br.readLine()));

                if(!FIleServer.registeredStNode.containsKey(tmp.nodeName)){
                    tmp.uuid=UUID.randomUUID().toString();
                    FIleServer.registeredStNode.put(tmp.nodeName,tmp.uuid);
                    writeBack();
                }
                else {
                    tmp.uuid=FIleServer.registeredStNode.get(tmp.nodeName);
                }
                tmp.lastVis=System.currentTimeMillis();
                FIleServer.stNodeList.add(tmp);


                PrintStream pw=new PrintStream(os);
                pw.println(tmp.uuid);
                pw.flush();

                pw.close();
                br.close();
                is.close();
                s.close();
                System.out.printf("Storage Node %s %s:%s added to the File Server.\n",tmp.nodeName,tmp.nodeIP,tmp.nodePort);
                Thread.yield();
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
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
                InetAddress add=InetAddress.getByName(FIleServer.SERVER_IP);
                DatagramSocket soc=new DatagramSocket(FIleServer.SERVER_HEART_PORT,add);
                soc.receive(datapacket);
                soc.close();
                String msg=new String(buffer,0,datapacket.getLength());
                System.out.println("Receiving message "+msg);
                String[] arr=msg.split(" ");
                if(arr[1].equals("ok")){
                    for(int i=0;i!=FIleServer.stNodeList.size();i++){
                        if(FIleServer.stNodeList.get(i).nodeName.equals(arr[0])){
                            FIleServer.stNodeList.get(i).lastVis=System.currentTimeMillis();
                        }
                    }
                }
                else if(arr[1].equals("end")){
                    int id=-1;
                    for(int i=0;i!=FIleServer.stNodeList.size();i++){
                        if(FIleServer.stNodeList.get(i).nodeName.equals(arr[0])){
                            id=i;
                            break;
                        }
                    }
                    if(id!=-1)
                        FIleServer.stNodeList.remove(id);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                Thread.yield();
            }
        }
    }
}

class NodeAliveChecker implements Runnable{
    @Override
    public void run() {
        while (true) {
            long cutime = System.currentTimeMillis();
            boolean flag;
            do {
                flag = false;
                System.out.printf("Active Storage Node number : %d\n", FIleServer.stNodeList.size());
                for (int i = 0; i != FIleServer.stNodeList.size(); i++) {
                    if (cutime - FIleServer.stNodeList.get(i).lastVis > 15000) {
                        System.out.printf("'%s' %s:%s is not active! It's removed From the file server!\n",
                                FIleServer.stNodeList.get(i).nodeName, FIleServer.stNodeList.get(i).nodeIP, FIleServer.stNodeList.get(i).nodePort);
                        FIleServer.stNodeList.remove(i);
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