import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileOperationListener implements Runnable{
    @Override
    public void run() {
        while (true){
            byte[] buffer=new byte[1024];
            DatagramPacket datapacket=new DatagramPacket(buffer,buffer.length);
            try {
                InetAddress add = InetAddress.getByName(FileServer.SERVER_IP);
                DatagramSocket soc = new DatagramSocket(FileServer.SERVER_FILEOP_PORT, add);
                soc.receive(datapacket);
                soc.close();
                String msg=new String(buffer,0,datapacket.getLength());
                String[] arr=msg.split("\n");

                if(arr[0].equals("upload")){
                    for(FileInfo i: FileServer.fileList)
                        if(arr[1].equals(i.uuid))
                        {
                            i.actualLength=Long.parseLong(arr[2]);
                            System.out.printf("[Upload] The actual length of the file %s is %d bytes.\n",i.uuid,i.actualLength);
                        }
                }
                else if(arr[0].equals("delete")){
                    String uuid=arr[1];
                    String host=arr[2];
                    int id=-1;
                    synchronized (FileServer.fileList){
                        for(int i = 0; i!= FileServer.fileList.size(); i++){
                            if(FileServer.fileList.get(i).uuid.equals(uuid)){
                                id=i;
                                System.out.printf("[Delete] Remove the file info of %s.\n",uuid);
                                break;
                            }
                        }
                        if(id!=-1)
                            FileServer.fileList.remove(id);
                    }

                    synchronized (FileServer.deleteFile)
                    {
                        id=-1;
                        for(int i=0;i<FileServer.deleteFile.size();i++){
                            if(FileServer.deleteFile.get(i).split("\n")[0].equals(uuid)&&
                                    FileServer.deleteFile.get(i).split("\n")[1].equals(host)){
                                id=i;
                                System.out.printf("[Delete] Remove the file info %s on server %s in delete trying table.\n",uuid,host);
                                break;
                            }
                        }
                        if(id!=-1)
                            FileServer.deleteFile.remove(id);
                        FileServer.writeTryingDeletingFile();
                    }
                }
                writeFileInfo();
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    private void writeFileInfo() throws IOException {
        ObjectOutputStream osm =new ObjectOutputStream(new FileOutputStream("FileInfo.dat"));
        osm.writeObject(FileServer.fileList);
        osm.flush();osm.close();
    }
}