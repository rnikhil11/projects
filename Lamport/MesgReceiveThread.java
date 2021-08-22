import com.sun.nio.sctp.*;
import java.util.ArrayList;

public class MesgReceiveThread extends Thread {

    ArrayList<SctpChannel> recvChannels;
    Params params;
    Setup parentThread;

    MesgReceiveThread(ArrayList<SctpChannel> rc, Params p, Setup pt) {
        this.recvChannels = rc;
        this.params = p;
        this.parentThread = pt;
    }

    public void run() {
        ArrayList<Thread> recvThreads = new ArrayList<Thread>();
        for (SctpChannel rcChannel : recvChannels) {
            Thread rt = new Thread(new RecvThread(rcChannel, params, parentThread));
            recvThreads.add(rt);
            rt.setDaemon(true);
            rt.start();

        }
        return;

    }
}
