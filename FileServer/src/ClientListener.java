import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.UUID;

public class ClientListener implements Runnable {
    private ServerSocket ss = new ServerSocket(FileServer.SERVER_CLIENT_PORT);
    private Socket s;
    private InputStream is;
    private OutputStream os;
    BufferedReader br;
    ClientListener() throws IOException {
    }

    private void upload(){
        try {
            String md5 = br.readLine();
            String fname = br.readLine();
            String flen = br.readLine();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            if (FileServer.fileList != null) {
                for (FileInfo i : FileServer.fileList) {
                    if (i.md5.equals(md5)) {
                        bw.write("exist\n"+i.uuid);
                        bw.close();
                        br.close();
                        is.close();
                        os.close();
                        s.close();
                        return;
                    }
                }
            }

            Collections.sort(FileServer.stNodeList);
            if (FileServer.stNodeList.size() >= 2) {
                String mNode = FileServer.stNodeList.get(0).nodeIP + " " + FileServer.stNodeList.get(0).nodePort;
                String bNode = FileServer.stNodeList.get(1).nodeIP + " " + FileServer.stNodeList.get(1).nodePort;
                bw.write(mNode + "\n");
                bw.write(bNode + "\n");

                String uuid = UUID.randomUUID().toString();

                System.out.println("Receiving file uploading request.");
                System.out.printf(">>File Name: %s\n", fname);
                System.out.printf(">>File length: %s\n", flen);
                System.out.printf(">>MD5 of this file: %s\n", md5);
                System.out.printf(">>UUID of this file: %s\n", uuid);
                System.out.printf(">>Allocating main Storage Node %s to the client.\n", mNode);
                System.out.printf(">>Allocating backup Storage Node %s to the client.\n", bNode);
                FileServer.fileList.add(new FileInfo(fname, uuid, Long.parseLong(flen), mNode, bNode, md5));

                bw.write(uuid + "\n");

                bw.close();
            } else {
                throw new FileServerException("Not enough Storage node!");
            }
        }
        catch (IOException | FileServerException e){
            System.out.println(e.getMessage());
        }
    }

    private void download(){
        try {
            String uuid = br.readLine();
            StringBuilder res=new StringBuilder();
            for(FileInfo i: FileServer.fileList){
                if(i.uuid.equals(uuid)&&i.actualLength>0){
                    res.append(i.fileName);res.append("\n");
                    res.append(i.mainNode);res.append("\n");res.append(i.backupNode);
                    break;
                }
                else if(i.uuid.equals(uuid)&&i.actualLength==0){
                    FileServer.deleteFile.add(uuid+"\n"+i.mainNode);
                    FileServer.deleteFile.add(uuid+"\n"+i.backupNode);
                }
            }
            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(os));
            bw.write(res.length()>0?res.toString():"null");
            bw.flush();
            bw.close();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    //Add the file to the delete trying list.

    private void  delete(){
        try{
            String uuid=br.readLine();
            int index=-1;
            synchronized (FileServer.fileList){
                for(int i = 0; i!= FileServer.fileList.size(); i++){
                    if(FileServer.fileList.get(i).uuid.equals(uuid)){
                        index=i;
                        System.out.printf("%s is added to the delete trying list.",uuid);
                        FileServer.deleteFile.add(uuid+"\n"+ FileServer.fileList.get(i).mainNode);
                        FileServer.deleteFile.add(uuid+"\n"+ FileServer.fileList.get(i).backupNode);
                        break;
                    }
                }
                FileServer.writeTryingDeletingFile();
            }

            BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(os));
            if(index==-1)
                bw.write("null");
            else
            {
                bw.write("suc");
            }
            bw.flush();
            bw.close();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                s = ss.accept();
                s.setSoTimeout(5000);
                is = s.getInputStream();
                os = s.getOutputStream();
                br = new BufferedReader(new InputStreamReader(is));

                String command = br.readLine();
                if (command.equals("upload"))
                    upload();
                else if(command.equals("download"))
                    download();
                else if(command.equals("delete")){
                    delete();
                }

                br.close();
                is.close();
                os.close();
                s.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}