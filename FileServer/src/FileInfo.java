import java.io.*;
import java.security.NoSuchAlgorithmException;

public class FileInfo implements Serializable{
    String fileName;
    String uuid;
    long length;
    String  mainNode;
    String  backupNode;
    String md5;
    public FileInfo(String fname,String id,long len,String mNode,String bNode,String md5val){
        fileName=fname;
        uuid=id;
        length=len;
        mainNode=mNode;
        backupNode=bNode;
        md5=md5val;
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
/*        FileInfo tmp=new FileInfo();
        tmp.fileName="asda";
        tmp.uuid="123123-345345-345546-456-57578";
        tmp.length=654654;

        ObjectOutputStream osm=new ObjectOutputStream(new FileOutputStream("7output.txt"));
        osm.writeObject(tmp);
        osm.close();

        FileInfo ooo;
        ObjectInputStream ism=new ObjectInputStream(new FileInputStream("7output.txt"));
        ooo=(FileInfo) ism.readObject();
        System.out.println(ooo.fileName+" "+ooo.uuid);*/
    }
}
