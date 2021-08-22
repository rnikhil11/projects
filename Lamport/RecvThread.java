import com.sun.nio.sctp.*;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RecvThread extends Thread {
    static int MAX_MSG_SIZE = 4096;
    SctpChannel rc;
    Params params;
    Setup parent;
    int senderId;

    RecvThread(SctpChannel rc, Params p, Setup parentThread) {
        this.rc = rc;
        this.params = p;
        this.parent = parentThread;

    }

    public void run() {
        try {

            while (rc.isOpen() && !parent.endFlag) {
                ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
                rc.receive(buf, null, null);

                try {
                    Message recvd = Message.fromByteBuffer(buf);
                    synchronized (parent.clockLock) {
                        parent.incrementClockAfterMesgRecv(recvd.timestamp);
                    }

                    if (recvd.msgType.equals(MessageType.nodeId)) {
                        parent.setNodeId(rc, recvd.nodeId);
                        senderId = recvd.nodeId;

                    } else {

                        synchronized (parent.lastRecvdMesgTimeStamps) {
                            parent.lastRecvdMesgTimeStamps.put(senderId, recvd.timestamp);
                            synchronized (parent.mutex.q) {
                                if (!parent.mutex.q.isEmpty()) {
                                    if (parent.lastRecvdMesgTimeStamps.size() == parent.socketAddrs.size()
                                            && parent.lastRecvdMesgTimeStamps.values().stream()
                                                    .allMatch((ts) -> ts > parent.mutex.q.peek().timestamp)) {// L1
                                        parent.lastRecvdMesgTimeStamps.notify();

                                    }
                                }
                            }
                        }

                        if (recvd.msgType.equals(MessageType.application)) {
                            parent.receive_start_mesg();

                        } else if (recvd.msgType.equals(MessageType.cs_request)) {
                            try {
                                parent.app.receive_cs_request(senderId, recvd.req.timestamp);
                            } catch (Exception e) {
                                System.out.println(Integer.toString(parent.nodeIndex) + " \n");
                                e.printStackTrace();
                            }

                        } else if (recvd.msgType.equals(MessageType.cs_reply)) {
                            parent.app.receive_cs_reply(senderId);

                        } else if (recvd.msgType.equals(MessageType.cs_release)) {
                            parent.output.println(
                                    "RECEIVED release MESSAGE " + recvd.req.timestamp + " from " + recvd.req.nodeId);

                            parent.receive_cs_release(senderId, recvd.req);
                        } else if (recvd.msgType.equals(MessageType.app_end)) {
                            parent.receive_endmesg(recvd);
                        } else if (recvd.msgType.equals(MessageType.csInfo)) {

                            parent.addCSInfo(recvd.csInfo);
                        }
                    }

                } catch (IOException e1) {
                    System.out.println("Exiting connection to " + senderId);
                    return;
                }
            }
            System.out.println("!!! CONNECTION to " + senderId + " CLOSED !!!");
            return;
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}