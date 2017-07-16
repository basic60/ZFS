import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class FileDeleter implements Runnable{
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