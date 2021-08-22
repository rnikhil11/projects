import com.sun.nio.sctp.*;

import java.util.HashMap;
import java.util.concurrent.*;

public class FloodProtocol {
    HashMap<String, SctpChannel> sendChannelMap;
    Params params;
    Setup parentThread;
    boolean activeState;
    boolean isZeroNode;
    boolean receivedFlood;
    static int MAX_MSG_SIZE = 4096;

    FloodProtocol(HashMap<String, SctpChannel> sc, Params p, Setup pt, boolean isZero) {
        this.sendChannelMap = sc;
        this.params = p;
        this.parentThread = pt;
        this.isZeroNode = isZero;
        this.receivedFlood = false;
        this.activeState = true;

    }

    public void run() {

        // flooding
        if (isZeroNode) {
            try {
                Message msg = new Message("PARENT-FLOOD", MessageType.flood);
                for (String recvrHostname : sendChannelMap.keySet()) {
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                    SctpChannel sc = sendChannelMap.get(recvrHostname);

                    try {
                        sc.send(msg.toByteBuffer(), messageInfo);
                    } catch (Exception e) {
                        System.out.println("[" + parentThread.hostname + "] :SENDING PARENT-FLOOD MESSAGE TO "
                                + recvrHostname + " FAILED");
                        e.printStackTrace();
                    }
                    // System.out.println("[" + parentThread.hostname + "] :PARENT-FLOOD MESSAGE
                    // SUCCESFULLY SENT to "
                    // + recvrHostname);
                }

            } catch (Exception e) {
                e.printStackTrace();

            }

        } else {

            synchronized (parentThread.parentNodeLock) {
                while (parentThread.parentNode.equals("")) {
                    try {
                        parentThread.parentNodeLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            // System.out.println("[" + parentThread.hostname + "] : Received flood from
            // parent node.");
            // System.out.println("[" + parentThread.hostname + "] : Forwarding floods and
            // sending acks..");

            // send flood/ack to nbrs
            ExecutorService fwdFloodExecutor = Executors.newFixedThreadPool(sendChannelMap.size());
            for (String recvrHostname : sendChannelMap.keySet()) {
                SctpChannel sc = sendChannelMap.get(recvrHostname);
                fwdFloodExecutor.submit(new Runnable() {
                    public void run() {
                        try {
                            // When flood message is from parent, send an ack to parent
                            synchronized (parentThread.parentNodeLock) {
                                if (recvrHostname.equals(parentThread.parentNode)) {
                                    Message parentAck = new Message("PARENT-ACK", MessageType.flood_ack);
                                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for
                                                                                                   // SCTP
                                                                                                   // layer
                                    try {
                                        sc.send(parentAck.toByteBuffer(), messageInfo);
                                    } catch (Exception e) {
                                        System.out.println("[" + parentThread.hostname
                                                + "] :SENDING parent-ack MESSAGE TO " + recvrHostname + " FAILED");
                                        e.printStackTrace();
                                    }
                                    // System.out.println("[" + parentThread.hostname
                                    // + "] :parent ack MESSAGE SUCCESFULLY SENT to " + recvrHostname);

                                    // only send flood message to neighbours who have not sent me
                                } else {

                                    synchronized (parentThread.floodSendersLock) {
                                        if (!parentThread.floodSenders.contains(recvrHostname)) {
                                            Message msg = new Message("PARENT-FLOOD", MessageType.flood);
                                            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo
                                                                                                           // for SCTP
                                                                                                           // layer
                                            try {
                                                sc.send(msg.toByteBuffer(), messageInfo);
                                            } catch (Exception e) {
                                                System.out.println("[" + parentThread.hostname
                                                        + "] :SENDING PARENT-FLOOD MESSAGE TO " + recvrHostname
                                                        + " FAILED");
                                                e.printStackTrace();
                                            }
                                            // System.out.println("[" + parentThread.hostname
                                            // + "] :PARENT-FLOOD MESSAGE SUCCESFULLY SENT to " + recvrHostname);
                                        }
                                    }

                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
            fwdFloodExecutor.shutdown();

            try {
                fwdFloodExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            parentThread.forwardMessageToNodeZero(new Message("END_FLOOD_PROTOCOL", MessageType.converge_cast));
        }
        System.out.println("[" + parentThread.hostname + "] : exiting FLOOD PROTOCOL");
        if (!isZeroNode) {
            System.out.println("[" + parentThread.hostname + "]: PARENT:" + parentThread.parentNode);
        }
        return;
    }
}
