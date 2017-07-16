import java.io.*;
import java.security.NoSuchAlgorithmException;

public class FileInfo implements Serializable{
    String fileName;
    String uuid;
    long length;
    long actualLength=0;
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
}
