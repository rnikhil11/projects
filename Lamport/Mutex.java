import java.util.HashMap;
import java.util.PriorityQueue;

public class Mutex {
    int nodeId;
    Params p;
    PriorityQueue<CSRequest> q;
    PriorityQueue<CSRequest> bufferQ;

    Setup setup;
    CSInfo csInfo;

    Mutex(int _nodeId, Params _p, Setup _setup) {
        nodeId = _nodeId;
        p = _p;
        q = new PriorityQueue<CSRequest>();
        bufferQ = new PriorityQueue<CSRequest>();

        setup = _setup;

    }

    public void printQueue() {
        synchronized (q) {
            for (CSRequest cr : q) {
                setup.output.println("(" + cr.timestamp + "," + cr.nodeId + ")");
            }
            setup.output.println(">>>>>> end of queue");
        }
    }

    public void cs_leave() {

        synchronized (q) {
            setup.output.println("CS LEAVE");
            if (!q.isEmpty()) {
                CSRequest cr = q.poll();
                csInfo.setClockAfterCS(setup.getTimeStamp());

                if (!setup.isZeroNode) {
                    setup.sendCSInfoMesg(csInfo);
                } else {
                    setup.addCSInfo(csInfo);
                }

                setup.output.println("CS_FINISH");

                try {
                    setup.broadcast_release_mesg(cr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("ERROR");
            }
        }

    }

    public void cs_enter(CSRequest csreq) throws InterruptedException {
        int req_ts = csreq.timestamp;
        synchronized (setup.lastRecvdMesgTimeStamps) {
            setup.output.println("CS ENTER " + csreq.timestamp);
            while (!(setup.lastRecvdMesgTimeStamps.size() == setup.socketAddrs.size()
                    && setup.lastRecvdMesgTimeStamps.values().stream().allMatch((ts) -> ts > req_ts))) {// L1
                setup.output.println("Waiting for L1");
                setup.output.println(req_ts);
                setup.lastRecvdMesgTimeStamps.wait();

            }
        }
        synchronized (this.q) {
            while (q.isEmpty() || (!q.isEmpty() && q.peek().nodeId != this.nodeId)) {// L2
                setup.output.println("Waiting for L2");
                printQueue();
                this.q.wait();
            }

        }
        HashMap<Integer, Integer> lrmt;
        synchronized (setup.lastRecvdMesgTimeStamps) {
            lrmt = setup.lastRecvdMesgTimeStamps;
        }
        synchronized (setup.clockLock) {
            int start_ts = setup.getTimeStamp();

            csInfo = new CSInfo(lrmt, csreq, start_ts, 0);
        }

        setup.output.println("Met L1 and L2 at :" + setup.getTimeStamp());

    }

}
