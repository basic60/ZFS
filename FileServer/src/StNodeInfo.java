import java.io.IOException;

public class StNodeInfo implements Comparable{
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
        StNodeInfo t1=new StNodeInfo();
        t1.volume=new Volume(1000,100000000);
        StNodeInfo t2=new StNodeInfo();
        t2.volume=new Volume(546546879,100000000);
        System.out.println(t1.compareTo(t2));

    }

    @Override
    public int compareTo(Object o) {
        return volume.compareTo(((StNodeInfo)o).volume);
    }
}


class Volume implements Comparable{
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

    @Override
    public int compareTo(Object o) {
        return (int)(-available+((Volume)o).available);
    }
}