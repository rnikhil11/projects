import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class LocalState implements Serializable {
    String hostname;
    int nodeIndex;
    String parent;
    boolean isActive;
    ArrayList<Integer> clock;
    HashMap<String, ArrayList<Message>> channelStates;

    LocalState(String h, int i) {
        this.hostname = h;
        this.nodeIndex = i;
        this.isActive = true;
        clock = new ArrayList<Integer>();
        channelStates = new HashMap<String, ArrayList<Message>>();

    }

    void setParent(String p) {
        this.parent = p;
    }

    void storeStateAndTimestamp(boolean active, ArrayList<Integer> cl) {
        this.isActive = active;
        this.clock = cl;

    }

    void stopRecording() {
        for (String key : this.channelStates.keySet()) {
            this.channelStates.put(key, new ArrayList<Message>());
        }
    }

    LocalState deepclone() {
        LocalState ls_copy = new LocalState(this.hostname, this.nodeIndex);
        ls_copy.parent = this.parent;
        ls_copy.isActive = this.isActive;
        ls_copy.clock.addAll(this.clock);
        ls_copy.channelStates.putAll(this.channelStates);
        return ls_copy;
    }

}