import java.io.IOException;
import java.net.*;

public class GUIStorageNodeNotifier implements Runnable {
    private final int GUI_ST_PORT=35000;
    @Override
    public void run() {
        while (true){
            try
            {
                InetAddress add = InetAddress.getByName("127.0.0.1");
                StringBuilder sb=new StringBuilder();
                for (StNodeInfo i : FileServer.stNodeList) {
                    sb.append(i.nodeName);sb.append("\n");
                    sb.append(i.nodeIP).append(" ").append(i.nodePort);sb.append("\n");
                    sb.append(i.volume.getAvailableBytes());sb.append("\n");
                    sb.append(i.volume.getTotalBytes());sb.append("\n");
                }

                byte[] buf=sb.toString().getBytes();
                DatagramPacket dp=new DatagramPacket(buf,buf.length,add,GUI_ST_PORT);
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
