import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

// Enumeration to store message types
enum MessageType {
	application, cs_request, cs_reply, cs_release, nodeId, app_end, csInfo
};

// Object to store message passing between nodes
// Message class can be modified to incoroporate all fields than need to be
// passed
// Message needs to be serializable
// Most base classes and arrays are serializable
public class Message implements Serializable {
	MessageType msgType;
	CSRequest req;
	public int nodeId;
	public String message;
	public int timestamp;
	long avgResponseTime;
	double throughput;
	CSInfo csInfo;

	// Constructor
	public Message(int _nodeId, MessageType t, int ts) {
		nodeId = _nodeId;
		msgType = t;
		timestamp = ts;
	}

	public Message(int _nodeId, MessageType t, double tp, long art, int ts) {
		nodeId = _nodeId;
		msgType = t;
		timestamp = ts;
		avgResponseTime = art;
		throughput = tp;
	}

	public Message(int _nodeId, String msg, MessageType t, int ts) {
		nodeId = _nodeId;

		msgType = t;
		message = msg;
		timestamp = ts;
	}

	public Message(int _nodeId, int msg, MessageType t, int ts) {
		nodeId = _nodeId;

		msgType = t;
		message = Integer.toString(msg);
		timestamp = ts;
	}

	public Message(int _nodeId, CSRequest _req, MessageType t, int ts) {
		nodeId = _nodeId;

		msgType = t;
		req = _req;
		timestamp = ts;

	}

	public Message(CSInfo _csInfo, int ts) {
		csInfo = _csInfo;
		msgType = MessageType.csInfo;
		timestamp = ts;
	}

	// Convert current instance of Message to ByteBuffer in order to send message
	// over SCTP
	public ByteBuffer toByteBuffer() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(this);
		oos.flush();

		ByteBuffer buf = ByteBuffer.allocateDirect(bos.size());
		buf.put(bos.toByteArray());

		oos.close();
		bos.close();

		// Buffer needs to be flipped after writing
		// Buffer flip should happen only once
		buf.flip();
		return buf;
	}

	// Retrieve Message from ByteBuffer received from SCTP
	public static Message fromByteBuffer(ByteBuffer buf) throws Exception {
		// Buffer needs to be flipped before reading
		// Buffer flip should happen only once
		buf.flip();
		byte[] data = new byte[buf.limit()];
		buf.get(data);
		buf.clear();

		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bis);
		Message msg = (Message) ois.readObject();

		bis.close();
		ois.close();

		return msg;
	}

}
