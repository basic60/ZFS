import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//To start four threads of Storage Node at once.
public class StTest {
    private static final List<Runnable> taskList=new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        taskList.add(new StartMain(new String[]{"storage1.properties"}));
        taskList.add(new StartMain(new String[]{"storage2.properties"}));
        taskList.add(new StartMain(new String[]{"storage3.properties"}));
        taskList.add(new StartMain(new String[]{"storage4.properties"}));

        ExecutorService exec= Executors.newCachedThreadPool();
        for(Runnable i:taskList){
            exec.submit(i);
            Thread.sleep(100);
        }
        //Add a new endpoint after one minute.
        Thread.sleep(60000);
        exec.submit(new StartMain(new String[]{"storage5.properties"}));
        exec.shutdown();
    }
}

class StartMain implements Runnable{
    private String[] args;
    public StartMain(String[] var){args=var;}
    @Override
    public void run() {
        StorageNode.main(args);
    }
}