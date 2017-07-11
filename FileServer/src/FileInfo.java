import java.io.*;
import java.security.NoSuchAlgorithmException;

public class FileInfo implements Serializable{
    String fileName;
    String uuid;
    long length;
    StNodeInfo mainNode;
    StNodeInfo backupNode;
    String md5;
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
/*        FileInfo tmp=new FileInfo();
        tmp.fileName="asda";
        tmp.uuid="123123-345345-345546-456-57578";
        tmp.length=654654;

        ObjectOutputStream osm=new ObjectOutputStream(new FileOutputStream("output.txt"));
        osm.writeObject(tmp);
        osm.close();

        FileInfo ooo;
        ObjectInputStream ism=new ObjectInputStream(new FileInputStream("output.txt"));
        ooo=(FileInfo) ism.readObject();
        System.out.println(ooo.fileName+" "+ooo.uuid);*/
    }
}
