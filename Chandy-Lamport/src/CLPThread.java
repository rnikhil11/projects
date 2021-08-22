import com.sun.nio.sctp.*;

import java.util.ArrayList;
import java.util.HashMap;

public class CLPThread extends Thread {
    final int MAX_MSG_SIZE = 4096;
    HashMap<String, SctpChannel> sendChannelMap;
    HashMap<String, SctpChannel> recvChannelMap;
    Params params;
    Setup parentThread;
    boolean isZeroNode;
    boolean blueNode;
    ArrayList<Boolean> recvdMarkers;
    boolean terminalSnapshot;

    CLPThread(HashMap<String, SctpChannel> sc, HashMap<String, SctpChannel> rc, Params p, Setup pt, boolean isZero) {
        this.sendChannelMap = sc;
        this.recvChannelMap = rc;
        this.params = p;
        this.parentThread = pt;
        this.isZeroNode = isZero;
        this.blueNode = true;
        this.terminalSnapshot = false;

    }

    public void resetState() {
        synchronized (parentThread) {
            parentThread.isBlue = true;
        }
        parentThread.clearRecords();
    }

    public boolean verify(String hostname, int nodeIndex, int timestamp, HashMap<String, LocalState> snaps) {
        int maxValue = -1;
        for (String h : snaps.keySet()) {
            maxValue = Math.max(maxValue, snaps.get(h).clock.get(nodeIndex));
        }

        return maxValue == snaps.get(hostname).clock.get(nodeIndex) ? true : false;

    }

    public void verifyGlobalSnapshot(HashMap<String, LocalState> snaps) {

        System.out.println("[" + parentThread.hostname + "]: Verifying global snapshot ");
        boolean result = true;

        for (String h : snaps.keySet()) {
            System.out.println(h + "-------" + snaps.get(h).clock);
        }

        for (String h : snaps.keySet()) {
            LocalState ls = snaps.get(h);

            int nodeIndex = ls.nodeIndex;
            int timestamp = ls.clock.get(nodeIndex);
            // System.out.println(Integer.toString(nodeIndex) + " : " + verify(h, nodeIndex,
            // timestamp, snaps));
            result = result && verify(h, nodeIndex, timestamp, snaps);
        }
        System.out.println("*************" + result + "**************");
    }

    public void run() {
        if (isZeroNode) {
            System.out.println("[" + parentThread.hostname + "]: Zero node starting CLP");
            parentThread.incrementSnapshotSize();
            while (!this.terminalSnapshot) {
                // stop taking snapshot if its a terminal snapshot
                boolean breakFlag = false;

                parentThread.changeColor();
                Long startTime = System.currentTimeMillis();
                // initiate CL protocol
                HashMap<String, LocalState> snaps;
                synchronized (parentThread.snapshots) {
                    while (parentThread.getSnapshots().size() != this.params.numNodes) {

                        try {
                            parentThread.snapshots.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    snaps = parentThread.getSnapshots();
                    parentThread.snapshots.notifyAll();

                }
                // wait till zero node gets snapshots from all nodes

                for (LocalState ls : snaps.values()) {
                    if (ls.isActive) {
                        breakFlag = true;
                        break;
                    } else {
                        for (ArrayList<Message> mesgs : ls.channelStates.values()) {
                            if (mesgs.size() != 0) {
                                breakFlag = true;
                                break;
                            }
                        }

                        if (breakFlag) {
                            break;
                        }
                    }
                }
                // checking for termination condition: each snapshot must have passive state and
                // empty channel states
                if (!breakFlag) {
                    terminalSnapshot = true;
                }
                verifyGlobalSnapshot(snaps);
                resetState();
                try {
                    Thread.sleep(Math.max(System.currentTimeMillis() - startTime, params.snapshotDelay));
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
                parentThread.incrementSnapshotSize();

            }

            System.out.println("[" + parentThread.hostname + "]: END SNAPSHOT");

            parentThread.sendEndMesg();
            return;

        } else {
            // System.out.println("[" + parentThread.hostname + "]:>>>>STARTING CLP");

            // finished sending marker messages to all
            synchronized (parentThread.endCLPSendLock) {
                while (!parentThread.endCLPSendFlag && parentThread.isBlue()) {
                    try {
                        parentThread.endCLPSendLock.wait();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                parentThread.endCLPSendLock.notifyAll();
            }
            // System.out.println("[" + parentThread.hostname + "]:>>finish sending CLP");

            // finished receiving marker messages from neighbours
            synchronized (parentThread.markerLock) {
                while (parentThread.markerReceipts != recvChannelMap.size()) {
                    try {
                        parentThread.markerLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                parentThread.markerLock.notifyAll();
            }
            // System.out.println("[" + parentThread.hostname + "]:>>finish receving CLP");

            this.resetState();

        }

        // System.out.println("[" + parentThread.hostname + "]:CLP termination");
        return;
    }
}
