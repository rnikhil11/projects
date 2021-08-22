import java.io.Serializable;

public class CSRequest implements Comparable<CSRequest>, Serializable {
    int nodeId;
    int timestamp;

    CSRequest(int _nodeId, int _ts) {
        nodeId = _nodeId;
        timestamp = _ts;
    }

    @Override
    public int compareTo(CSRequest o) {
        if (o.timestamp == this.timestamp) {
            if (this.nodeId == o.nodeId) {
                return 0;
            } else {
                return this.nodeId < o.nodeId ? -1 : 1;
            }
        } else {
            return this.timestamp < o.timestamp ? -1 : 1;
        }
    }

    public boolean equalTo(CSRequest o) {
        return this.nodeId == o.nodeId && this.timestamp == o.timestamp;
    }

}
