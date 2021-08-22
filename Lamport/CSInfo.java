import java.io.Serializable;
import java.util.HashMap;

public class CSInfo implements Comparable<CSInfo>, Serializable {
    HashMap<Integer, Integer> lastRecvdMesgTimestamps;
    CSRequest csReq;
    int clockBeforeCS;
    int clockAfterCS;

    CSInfo(HashMap<Integer, Integer> l, CSRequest csr, int clk1, int clk2) {
        lastRecvdMesgTimestamps = new HashMap<Integer, Integer>(l);
        csReq = csr;
        clockBeforeCS = clk1;
        clockAfterCS = clk2;
    }

    @Override
    public int compareTo(CSInfo c2) {

        return this.csReq.compareTo(c2.csReq);

    }

    public void setClockAfterCS(int clk) {
        this.clockAfterCS = clk;
    }

    public CSInfo deepclone() {
        CSInfo cpy = new CSInfo(this.lastRecvdMesgTimestamps, this.csReq, this.clockBeforeCS, this.clockAfterCS);
        return cpy;
    }

    public void print() {
        System.out.println(this.lastRecvdMesgTimestamps.toString() + " " + this.clockBeforeCS + " (" + csReq.timestamp
                + "," + csReq.nodeId + ") " + clockAfterCS);
    }

    public void setClockBeforeCS(int ts) {
        this.clockBeforeCS = ts;
    }
}
