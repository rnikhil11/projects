import com.sun.nio.sctp.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Setup {
    int PORT;

    // Size of ByteBuffer to accept incoming messages
    boolean isZeroNode = false;
    HashMap<Integer, InetSocketAddress> socketAddrs;
    String hostname;
    Params params;
    int nodeIndex;
    int clock;
    PrintStream output;

    HashMap<Integer, SctpChannel> sendChannelMap;
    HashMap<Integer, SctpChannel> recvChannelMap;
    App app;
    HashMap<Integer, Integer> lastRecvdMesgTimeStamps;
    public final Object clockLock = new Object();
    boolean canStart;
    public final Object startLock = new Object();
    Mutex mutex;
    int startCount;
    public final Object endLock = new Object();
    int endMesgCount = 0;
    boolean endFlag = false;
    PriorityQueue<CSInfo> csList;
    int mesgCount = 0;
    public final Object mesgCountLock = new Object();
    long avgResponseTime;
    double avgThroughput;

    Setup(HashMap<Integer, InetSocketAddress> _nbrs, String _hostname, int _myPort, int _myIndex, Params _p,
            PrintStream _out) {
        socketAddrs = _nbrs;
        hostname = _hostname;
        PORT = _myPort;
        params = _p;
        nodeIndex = _myIndex;
        output = _out;
        isZeroNode = nodeIndex == 0;
        sendChannelMap = new HashMap<Integer, SctpChannel>();
        recvChannelMap = new HashMap<Integer, SctpChannel>();
        mutex = new Mutex(nodeIndex, params, this);

        clock = 0;
        lastRecvdMesgTimeStamps = new HashMap<Integer, Integer>();
        for (int nodeId : socketAddrs.keySet()) {
            lastRecvdMesgTimeStamps.put(nodeId, -1);
        }
        canStart = false;
        startCount = 0;
        if (isZeroNode) {
            csList = new PriorityQueue<CSInfo>();
        }
        avgResponseTime = 0;
        avgThroughput = 0;
    }

    public void incrementMesgCount() {
        synchronized (mesgCountLock) {
            mesgCount++;
        }
    }

    public void incrementMesgCountBy(int x) {
        synchronized (mesgCountLock) {
            mesgCount += x;
        }
    }

    public int getMesgCount() {
        synchronized (mesgCountLock) {
            return mesgCount;
        }
    }

    /**
     * Clock methods
     */
    public int getTimeStamp() {
        synchronized (clockLock) {
            return this.clock;
        }
    }

    public void incrementClock() {
        synchronized (clockLock) {
            clock++;
        }
    }

    public int getIncrementedClock() {
        synchronized (clockLock) {
            clock++;
            return clock;
        }
    }

    public void incrementClockAfterMesgRecv(int mesg_piggyback) {
        synchronized (clockLock) {
            clock = Math.max(clock, mesg_piggyback) + 1;
        }
    }

    public HashMap<Integer, Integer> getLastRecvdMesgTimeStamps() {
        synchronized (lastRecvdMesgTimeStamps) {
            return lastRecvdMesgTimeStamps;
        }
    }

    public void broadcast_req(CSRequest req) throws Exception {
        Message mesg = new Message(nodeIndex, req, MessageType.cs_request, this.getIncrementedClock());
        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer

        for (int nodeId : sendChannelMap.keySet()) {

            SctpChannel sc = sendChannelMap.get(nodeId);
            try {

                sc.send(mesg.toByteBuffer(), messageInfo);
                incrementMesgCount();

            } catch (Exception e) {
                System.out.println("SENDING APP MESSAGE TO " + nodeId + " FAILED");
                e.printStackTrace();
            }
        }

    }

    public void broadcast_release_mesg(CSRequest finishedCR) throws Exception {

        Message mesg = new Message(nodeIndex, finishedCR, MessageType.cs_release, this.getIncrementedClock());

        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer

        for (int nodeId : sendChannelMap.keySet()) {

            SctpChannel sc = sendChannelMap.get(nodeId);
            try {

                sc.send(mesg.toByteBuffer(), messageInfo);
                incrementMesgCount();

            } catch (Exception e) {
                System.out.println("SENDING RELEASE MESSAGE TO " + nodeId + " FAILED");
                e.printStackTrace();
            }
        }

    }

    public void receive_cs_release(int senderId, CSRequest a) throws InterruptedException {

        synchronized (mutex.q) {
            synchronized (mutex.bufferQ) {
                if (!mutex.q.isEmpty()) {
                    if (mutex.q.peek().equalTo(a)) {
                        mutex.q.poll();
                    } else {
                        mutex.bufferQ.add(a);
                    }
                    while (!mutex.bufferQ.isEmpty() && !mutex.q.isEmpty()
                            && mutex.bufferQ.peek().equalTo(mutex.q.peek())) {
                        mutex.q.poll();
                        mutex.bufferQ.poll();
                    }

                }
                mutex.printQueue();

            }

            if (!mutex.q.isEmpty()) {
                if (mutex.q.peek().nodeId == nodeIndex) {
                    output.println("notified");
                    mutex.q.notify();
                }
            }

        }

    }

    public void send_cs_reply(int recvrId) throws Exception {
        Message mesg = new Message(nodeIndex, "REPLY", MessageType.cs_reply, this.getIncrementedClock());
        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
        SctpChannel sc = sendChannelMap.get(recvrId);

        try {
            sc.send(mesg.toByteBuffer(), messageInfo);
            incrementMesgCount();

        } catch (Exception e) {
            System.out.println("SENDING REPLY MESSAGE TO " + recvrId + " FAILED");
            e.printStackTrace();
        }
    }

    public void receive_start_mesg() {
        if (isZeroNode) {
            synchronized (startLock) {
                startCount += 1;
                if (startCount == params.numNodes - 1) {
                    canStart = true;
                    startLock.notify();
                }
            }

        } else {
            synchronized (startLock) {
                if (!canStart) {
                    canStart = true;
                    startLock.notify();
                }
            }
        }
    }

    public void start() {

        try {

            System.out.println("[" + this.nodeIndex + "]: Starting node..");
            SctpServerChannel ssc;
            try {
                InetSocketAddress addr = new InetSocketAddress(PORT); // Get address from port number
                ssc = SctpServerChannel.open();// Open server channel
                ssc.bind(addr);// Bind server channel to address
            } catch (Exception ee) {
                System.out.println("Server creation failed. Exiting.");
                return;
            }
            Thread.sleep(60000); // wait for all machines to start

            // Send connection requests to nbrs
            for (Integer id : socketAddrs.keySet()) {

                // output.println("[" + this.nodeIndex + "]: Trying connection to server " +
                // h);
                // Connect to server using the socket address
                try {
                    // Store send channels in hashmap
                    sendChannelMap.put(id, SctpChannel.open(socketAddrs.get(id), 0, 0));

                } catch (Exception e) {
                    System.out.println(nodeIndex + ": Connection to node " + id + " failed.");
                    // e.printStackTrace();
                    continue;
                }
                // output.println("[" + this.nodeIndex + "]: Connected to Server " + h);

            }

            // Accept connections from nbrs on listen port

            ArrayList<SctpChannel> recvChannels = new ArrayList<SctpChannel>();

            while (recvChannels.size() < params.numNodes - 1) {
                SctpChannel sc = ssc.accept(); // Wait for incoming connection from client

                // Store channel in arraylist
                recvChannels.add(sc);

            }

            // Thread to receive any incoming message and operate on it
            Thread rt = new Thread(new MesgReceiveThread(recvChannels, params, this));
            rt.setDaemon(true);
            rt.start();
            for (int nbrId : sendChannelMap.keySet()) {
                SctpChannel sc = sendChannelMap.get(nbrId);
                Message mesg = new Message(nodeIndex, MessageType.nodeId, this.getIncrementedClock());
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                try {
                    sc.send(mesg.toByteBuffer(), messageInfo);
                    incrementMesgCount();

                } catch (Exception e) {
                    System.out.println("SENDING nodeID MESSAGE TO " + nbrId + " FAILED");
                    e.printStackTrace();
                }
            }
            synchronized (recvChannelMap) {
                while (recvChannelMap.size() != params.numNodes - 1) {
                    recvChannelMap.wait();
                }
            }
            System.out.println("[" + this.nodeIndex + "]: Finished setting up connections");

            app = new App(sendChannelMap, params, this, mutex);

            if (!isZeroNode) {
                Message mesg = new Message(nodeIndex, "START", MessageType.application, this.getIncrementedClock());
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                SctpChannel sc = sendChannelMap.get(0);
                try {
                    sc.send(mesg.toByteBuffer(), messageInfo);
                    incrementMesgCount();

                } catch (Exception e) {
                    System.out.println("SENDING START MESSAGE TO " + 0 + " FAILED");
                    e.printStackTrace();
                }
                synchronized (startLock) {
                    while (!canStart) {
                        startLock.wait();

                    }
                }
            } else {
                if (params.numNodes > 1) {
                    synchronized (startLock) {
                        while (!canStart) {
                            startLock.wait();
                        }
                    }
                    Message mesg = new Message(nodeIndex, "START", MessageType.application, this.getIncrementedClock());
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                    for (int nbrId : sendChannelMap.keySet()) {
                        SctpChannel sc = sendChannelMap.get(nbrId);
                        new Thread(new Runnable() {

                            public void run() {
                                try {
                                    sc.send(mesg.toByteBuffer(), messageInfo);
                                    incrementMesgCount();

                                } catch (Exception e) {
                                    System.out.println("SENDING START MESSAGE TO " + nbrId + " FAILED");
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }
                }

            }
            System.out.println(nodeIndex + ":done");

            // Application starts
            System.out.println("[" + nodeIndex + "]: App started");
            app.start();

            // end
            if (isZeroNode) {
                if (params.numNodes > 1) {
                    synchronized (endLock) {
                        while (!endFlag) {
                            endLock.wait();
                        }

                    }
                    Message mesg = new Message(nodeIndex, "END", MessageType.app_end, this.getIncrementedClock());
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                    for (int nbrId : sendChannelMap.keySet()) {
                        SctpChannel sc = sendChannelMap.get(nbrId);
                        new Thread(new Runnable() {

                            public void run() {
                                try {
                                    sc.send(mesg.toByteBuffer(), messageInfo);

                                } catch (Exception e) {
                                    System.out.println("SENDING end MESSAGE TO " + nbrId + " FAILED");
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }
                }
                synchronized (csList) {
                    verify(csList);
                }

                FileWriter fw = new FileWriter("recordings.out", true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(params.numNodes + " " + params.interRequestDelay + " " + params.csExecTime + " "
                        + avgResponseTime / params.numNodes + " " + avgThroughput / params.numNodes + " "
                        + mesgCount / params.numRequests);
                bw.newLine();
                bw.close();

            } else {
                synchronized (endLock) {
                    while (!endFlag) {
                        endLock.wait();
                    }
                    endLock.notify();
                }
            }
            output.close();

            rt.join();

            return;
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

    public void setNodeId(SctpChannel rc, int _nodeId) {

        synchronized (recvChannelMap) {
            recvChannelMap.put(_nodeId, rc);

            if (recvChannelMap.size() == params.numNodes - 1) {
                recvChannelMap.notify();
            }
        }
    }

    public void receive_endmesg(Message msg) {
        if (isZeroNode) {
            synchronized (endLock) {
                endMesgCount++;
                avgResponseTime += msg.avgResponseTime;
                avgThroughput += msg.throughput;
                if (endMesgCount == params.numNodes) {
                    endFlag = true;
                    endLock.notify();
                }

            }
        } else {
            synchronized (endLock) {
                endFlag = true;
                endLock.notify();
            }
        }
    }

    public void sendCSInfoMesg(CSInfo csInfo) {

        Message mesg = new Message(csInfo, getIncrementedClock());
        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
        SctpChannel sc = sendChannelMap.get(0);
        try {
            sc.send(mesg.toByteBuffer(), messageInfo);
            // incrementMesgCount();

        } catch (Exception e) {
            System.out.println("SENDING csinfo MESSAGE TO " + 0 + " FAILED");
            e.printStackTrace();
        }
    }

    public void addCSInfo(CSInfo csInfo) {

        System.out.println("Adding " + csList.size());
        synchronized (csList) {
            csList.add(csInfo.deepclone());
        }
    }

    public void verify(PriorityQueue<CSInfo> csl) {
        int prevNodeId = -1;
        int ts_end_prev_cs = 0;
        int ts_start_cs;
        for (int i = 0; i < csl.size(); i++) {
            CSInfo csInfo = csl.poll();
            // csInfo.print();
            ts_start_cs = csInfo.clockBeforeCS;
            if (prevNodeId != -1) {
                if (csInfo.csReq.nodeId == prevNodeId) {
                    boolean b1 = ts_start_cs > ts_end_prev_cs;
                    if (!b1) {
                        System.out.println("INCORRECT");
                        return;
                    }
                } else {
                    System.out.println(
                            "Checking ts of process" + prevNodeId + " in " + csInfo.lastRecvdMesgTimestamps.toString());
                    int tmp = csInfo.lastRecvdMesgTimestamps.get(prevNodeId);
                    boolean b1 = ts_end_prev_cs < tmp;
                    boolean b2 = tmp < ts_start_cs;
                    if (!(b1 && b2)) {
                        if (!b1) {
                            System.out.println("prev process cs ended at " + ts_end_prev_cs);
                            System.out.println("and sent mesg with ts " + tmp);
                        }
                        if (!b2) {
                            System.out.println("received mesg from prev process with ts " + tmp);
                            System.out.println("Started CS at " + ts_start_cs);
                        }
                        System.out.println("INCORRECT");
                        return;
                    }
                }
            }
            ts_end_prev_cs = csInfo.clockAfterCS;
            prevNodeId = csInfo.csReq.nodeId;

        }
        System.out.println("VERIFIED");
    }

}
