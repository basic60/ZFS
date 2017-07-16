import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class HeartbeatListener implements Runnable{
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
                // System.out.println("Receive message:\n"+msg);

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