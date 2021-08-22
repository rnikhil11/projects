import com.sun.nio.sctp.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MesgReceiveThread extends Thread {

    HashMap<String, SctpChannel> recvChannelMap;
    Params params;
    Setup parentThread;

    MesgReceiveThread(HashMap<String, SctpChannel> rc, Params p, Setup pt) {
        this.recvChannelMap = rc;
        this.params = p;
        this.parentThread = pt;
    }

    public void run() {
        ArrayList<Thread> recvThreads = new ArrayList<Thread>();
        for (String recvrHost : recvChannelMap.keySet()) {
            Thread rt = new Thread(new RecvThread(recvChannelMap.get(recvrHost), params, parentThread));
            recvThreads.add(rt);
            rt.setDaemon(true);
            rt.start();

        }

    }
}
