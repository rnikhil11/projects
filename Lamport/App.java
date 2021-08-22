import java.util.HashMap;
import java.util.Random;

import com.sun.nio.sctp.*;

public class App {
    HashMap<Integer, SctpChannel> sendChannelMap;
    Params params;
    Setup parentThread;
    Mutex mutex;
    double u;

    public App(HashMap<Integer, SctpChannel> _sendChannelMap, Params _params, Setup _parent, Mutex _mutex) {
        sendChannelMap = _sendChannelMap;
        params = _params;
        parentThread = _parent;
        mutex = _mutex;
    }

    public double getNext(double m) {
        Random r = new Random();

        u = r.nextDouble();
        return -m * Math.log(1 - u);
    }

    public int getRandom(int mean) {

        int x = (int) getNext(mean);
        return x == 0 ? 1 : x;

    }

    public CSRequest generate_cs_request() {

        CSRequest req = new CSRequest(parentThread.nodeIndex, parentThread.getTimeStamp());

        synchronized (mutex.q) {
            mutex.q.add(req);
            if (mutex.q.peek().nodeId == parentThread.nodeIndex) {
                mutex.q.notify();
            }
            try {
                parentThread.broadcast_req(req);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return req;

    }

    public void receive_cs_request(int cr_nodeId, int cr_ts) {
        CSRequest req = new CSRequest(cr_nodeId, cr_ts);
        synchronized (mutex.q) {

            mutex.q.add(req);

            try {
                parentThread.send_cs_reply(cr_nodeId);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void receive_cs_reply(int senderId) {

    }

    public void execute_cs(int execTime) throws InterruptedException {

        Thread.sleep(execTime);

    }

    public void start() throws InterruptedException {
        int i = 0;
        long sumOfExecTimes = 0;
        double thruput = 0;
        long beginTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis(), startTime;
        while (i < params.numRequests) {
            int delay = getRandom(params.interRequestDelay);
            Thread.sleep(delay);
            int execTime = getRandom(params.csExecTime);
            CSRequest cr = generate_cs_request();
            startTime = System.currentTimeMillis();

            mutex.cs_enter(cr);
            execute_cs(execTime);
            endTime = System.currentTimeMillis();

            mutex.cs_leave();

            i++;
            sumOfExecTimes += (endTime - startTime);

        }
        long avgResponseTime = sumOfExecTimes / params.numRequests;
        thruput = (double) params.numRequests / (endTime - beginTime);
        System.out.println(parentThread.nodeIndex + " FINISHED ALL REQUESTS");
        if (!parentThread.isZeroNode) {
            Message mesg = new Message(parentThread.nodeIndex, MessageType.app_end, thruput, avgResponseTime,
                    parentThread.getIncrementedClock());
            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
            SctpChannel sc = sendChannelMap.get(0);
            try {
                sc.send(mesg.toByteBuffer(), messageInfo);
                // parentThread.incrementMesgCount();
            } catch (Exception e) {
                System.out.println("SENDING END MESSAGE TO " + 0 + " FAILED");
                e.printStackTrace();
            }
        } else {

            synchronized (parentThread.endLock) {
                parentThread.avgResponseTime += avgResponseTime;
                parentThread.avgThroughput += thruput;
                parentThread.endMesgCount += 1;
                if (parentThread.endMesgCount == params.numNodes) {
                    parentThread.endFlag = true;
                    parentThread.endLock.notify();
                }
            }

        }
        return;
    }
}
