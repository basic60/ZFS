public class NodeAliveChecker implements Runnable{
    int last=-1;
    @Override
    public void run() {
        while (true) {
            long cutime = System.currentTimeMillis();
            boolean flag;
            do {
                flag = false;
                if(FileServer.stNodeList.size()!=last){
                    System.out.printf("Active Storage Node number : %d\n", FileServer.stNodeList.size());
                    last= FileServer.stNodeList.size();
                }
                for (int i = 0; i != FileServer.stNodeList.size(); i++) {
                    if (cutime - FileServer.stNodeList.get(i).lastVis > 15000) {
                        System.out.printf("'%s' %s:%s is not active! It's removed From the file server!\n",
                                FileServer.stNodeList.get(i).nodeName, FileServer.stNodeList.get(i).nodeIP, FileServer.stNodeList.get(i).nodePort);
                        FileServer.stNodeList.remove(i);
                        flag = true;
                        break;
                    }
                }
            } while (flag);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                Thread.yield();
            }
        }
    }
}