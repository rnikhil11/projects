import com.sun.nio.sctp.*;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class RecvThread extends Thread {
    static int MAX_MSG_SIZE = 4096;

    SctpChannel rc;
    Params params;
    Setup parent;

    RecvThread(SctpChannel rc, Params p, Setup parentThread) {
        this.rc = rc;
        this.params = p;
        this.parent = parentThread;

    }

    public void run() {
        try {
            String senderHostname = InetAddress
                    .getByName(rc.getRemoteAddresses().toArray()[0].toString().substring(1).split(":")[0])
                    .getHostName();
            while (rc.isOpen() && !parent.endFlag) {
                ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
                rc.receive(buf, null, null);

                try {
                    Message recvd = Message.fromByteBuffer(buf);

                    if (recvd.msgType.equals(MessageType.flood)) {
                        parent.recvFloodMesg(rc, recvd, senderHostname);

                    } else if (recvd.msgType.equals(MessageType.flood_ack)) {
                        parent.recvFloodAckMesg(rc, recvd, senderHostname);

                    } else if (recvd.msgType.equals(MessageType.converge_cast)) {
                        parent.recvConvergeCastMesg(rc, recvd, senderHostname);

                    } else if (recvd.msgType.equals(MessageType.marker)) {
                        parent.recvMarkerMesg(rc, recvd, senderHostname);

                    } else if (recvd.msgType.equals(MessageType.forward)) {
                        parent.recvForwardMesg(rc, recvd, senderHostname);

                    } else if (recvd.msgType.equals(MessageType.map)) {
                        parent.recvMapMesg(rc, recvd, senderHostname);

                    } else if (recvd.msgType.equals(MessageType.end)) {
                        parent.sendEndMesg();
                        break;
                    }
                } catch (IOException e1) {
                    System.out.println("Exiting connection to " + senderHostname);
                    return;
                }
            }
            System.out.println("!!! CONNECTION to " + senderHostname + " CLOSED !!!");
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }
}