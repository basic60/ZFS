import java.io.*;
import java.net.*;
import java.util.Properties;

class FileListener implements Runnable {
    private final int FilePort;
    private final String rootDir;
    private final String nodeName;
    private final Volume volume;
    private final String fileServerIP;
    private final Properties prop;
    private final int NODE_PORT;
    private final int FILEOP_PORT=30003;
    private ServerSocket ss;
    Socket soc;
    BufferedReader br;
    FileListener(StorageNode a) throws IOException
    {
        FilePort=a.nodePort;rootDir=a.rootDir;nodeName=a.nodeName;
        volume=a.volume;prop=a.prop;fileServerIP=a.fileServerIP;NODE_PORT=a.nodePort;
        ss=new ServerSocket(FilePort);
    }

    private void notifyUploadFileSize(String uuid,long size){
        InetAddress add;
        while (true){
            try {
                add = InetAddress.getByName(fileServerIP);
                byte[] buffer=String.format("upload\n%s\n%d",uuid,size).getBytes();
                DatagramPacket dp=new DatagramPacket(buffer,buffer.length,add,FILEOP_PORT);
                DatagramSocket dataSocket=new DatagramSocket();
                dataSocket.send(dp);
                dataSocket.close();
                break;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void notifyDeleteFiles(String uuid) {
        final String LOCAL_HOST="127.0.0.1 "+NODE_PORT;
        InetAddress add ;
        while (true){
            try {
                add = InetAddress.getByName(fileServerIP);
                byte[] buffer=String.format("delete\n%s\n%s\n",uuid,LOCAL_HOST).getBytes();
                DatagramPacket dp=new DatagramPacket(buffer,buffer.length,add,FILEOP_PORT);
                DatagramSocket dc=new DatagramSocket();
                dc.send(dp);
                dc.close();
                System.out.printf("[%s-notify-remove] Notify the server %s of the file %s's remove info completely.\n",nodeName,LOCAL_HOST,uuid);
                break;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    //Upload the file.
    void upload(){
        try{
            String forwardTable = br.readLine();
            String uuid = br.readLine();

            System.out.printf("[%s-file] Start receiving file\n", nodeName);
            System.out.printf("[%s-file] The uuid of this file is : %s\n", nodeName, uuid);
            System.out.printf("[%s-file] Backup Node : %s\n", nodeName, forwardTable);

            //Read binary data of the file.
            BufferedInputStream bs = new BufferedInputStream(soc.getInputStream());

            //Save file to the disk.
            FileOutputStream fout = new FileOutputStream(new File(rootDir, uuid));
            int tmp;
            long cnt = 0;
            while ((tmp = bs.read()) != -1) {
                fout.write(tmp);
                cnt++;
            }
            fout.flush();
            volume.minus(cnt);
            System.out.printf("[%s-file] Save file finished. Total %d bytes.\n",nodeName,cnt);

            notifyUploadFileSize(uuid,cnt);

            bs.close();
            fout.close();
            br.close();
            soc.close();

            //Forward the file to the backup storage node.
            if (!forwardTable.equals("null"))
            {
                forward(uuid,forwardTable);
                System.out.printf("[%s-file] Forward file finished.\n",nodeName);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    //Forward the file to the server.
    private void forward(String uuid,String forwardTable){
        Socket socket;
        String[] arr=forwardTable.split(" ");
        try{
            while (true) {
                System.out.printf("[%s-forward] Start forwarding to %s\n", nodeName, forwardTable);
                System.out.printf("[%s-forward] Try connecting to the server at %s\n",nodeName,forwardTable);
                socket = new Socket(arr[0], Integer.valueOf(arr[1]));
                System.out.printf("[%s-forward] Connect to the server successfully\n",nodeName);
                PrintStream out = new PrintStream(socket.getOutputStream());
                out.println("upload");
                out.println("null");
                out.println(uuid);

                BufferedInputStream bin = new BufferedInputStream(new FileInputStream(new File(rootDir,uuid)));
                BufferedOutputStream bout=new BufferedOutputStream(socket.getOutputStream());
                int tmp;
                while ((tmp = bin.read()) != -1) {
                    bout.write(tmp);
                }
                bout.flush();

                bin.close();
                out.close();
                bout.close();
                socket.close();
                break;
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    //Send file to the client.
    private void download(String uuid){
        try {

            FileInputStream fin=new FileInputStream(new File(rootDir,uuid));
            BufferedOutputStream out=new BufferedOutputStream(soc.getOutputStream());
            int tmp;long cnt=0;
            System.out.printf("[%s-download] Start send file %s\n",nodeName,uuid);
            while ((tmp=fin.read())!=-1){
                cnt++;
                out.write(tmp);
                if(cnt%2048==0)
                    out.flush();
            }
            out.flush();
            out.close();
            fin.close();
            System.out.printf("[%s-download] Send file finished. Total %d bytes.\n",nodeName,cnt);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    //Delete the file on the server.
    private void delete(String uuid){
        File dir=new File(rootDir,uuid);
        if(dir.exists()){
            long len=dir.length();
            volume.setAvailableBytes(volume.getAvailableBytes()+len);
            if(dir.delete())
            {
                System.out.printf("[%s-delete] Delete file %s successfully! %d bytes of space are freed!\n",nodeName,uuid,len);
                notifyDeleteFiles(uuid);
            }
            else {
                System.out.printf("[%s-delete] Delete file %s Failed!\n",nodeName,uuid);
            }
        }
        else {
            //Notify the file server again if the file has been deleted yet but receive the delete command again.
            notifyDeleteFiles(uuid);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.printf("[%s] Listen at port %d\n", nodeName, FilePort);
                soc = ss.accept();
                soc.setSoTimeout(5000);

                //Read the uuid and the forward table.
                br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                String commmand = br.readLine();
                if (commmand.equals("upload"))
                    upload();
                else if(commmand.equals("download"))
                    download(br.readLine());
                else if(commmand.equals("delete")){
                    delete(br.readLine());
                }
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
    }
}

