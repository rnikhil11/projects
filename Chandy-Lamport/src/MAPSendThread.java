import com.sun.nio.sctp.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MAPSendThread extends Thread {
    HashMap<String, SctpChannel> sendChannelMap;
    ArrayList<String> receivers;
    Params params;
    Setup parent;

    MAPSendThread(HashMap<String, SctpChannel> sc, Params p, Setup parentThread) {
        this.sendChannelMap = sc;
        this.receivers = new ArrayList<String>(sc.keySet());
        this.params = p;
        this.parent = parentThread;

    }

    public void run() {
        try {
            while (!parent.endFlag) {
                if (parent.isActive()) {
                    int numMessages = (int) Math.floor(
                            Math.random() * (params.maxPerActive - params.minPerActive + 1) + params.minPerActive);
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer

                    // System.out.println("SENDING " + Integer.toString(numMessages) + " NO. OF MAP
                    // MESSAGES ");
                    for (int i = 0; i < numMessages; i++) {

                        int randomIndex = (int) Math.floor(Math.random() * (receivers.size()));
                        String recvrHostname = receivers.get(randomIndex);

                        SctpChannel sc = sendChannelMap.get(recvrHostname);

                        parent.sendMapMesg(sc, messageInfo, recvrHostname);

                        Thread.sleep(params.minSendDelay);

                    }
                    parent.setActive(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
