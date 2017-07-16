import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class GUIFileNotifier implements Runnable{
    private final int GUI_FILE_PORT=35001;
    @Override
    public void run() {
        while (true){
            try
            {
                InetAddress add = InetAddress.getByName("127.0.0.1");
                StringBuilder sb=new StringBuilder();
                for (FileInfo i : FileServer.fileList) {
                    sb.append(i.fileName);sb.append("\n");
                    sb.append(i.uuid);sb.append("\n");
                    sb.append(i.length);sb.append("\n");
                    sb.append(i.actualLength);sb.append("\n");
                    sb.append(i.mainNode);sb.append("\n");
                    sb.append(i.backupNode);sb.append("\n");
                }

                byte[] buf=sb.toString().getBytes();
                DatagramPacket dp=new DatagramPacket(buf,buf.length,add,GUI_FILE_PORT);
                DatagramSocket dSocket=new DatagramSocket();
                dSocket.send(dp);
                dSocket.close();
                System.out.println("[Notify-GUI] Send the storage node info to the GUI.");
                Thread.sleep(1000);
            } catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
