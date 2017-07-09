import java.io.IOException;

public class StNodeInfo {
    String nodeName;
    String nodeIP;
    int nodePort;
    Volume volume;
    String uuid;
    long lastVis;
    public StNodeInfo(){}
    public StNodeInfo(String name,String ip,int port,long tot,long avi){
        volume=new Volume(avi,tot);nodeIP=ip;
        nodeName=name;
        nodePort=port;
    }

    public static void main(String[] args) throws IOException {
    }
}


class Volume{
    private long length;

    private long available;

    Volume(long avi,long tot){
        length=tot;
        available=avi;
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