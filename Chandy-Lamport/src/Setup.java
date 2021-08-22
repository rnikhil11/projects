import com.sun.nio.sctp.*;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Setup {
	// Port number to open server for clients to connect
	// Client should connect to same port number that server opens
	int PORT = 1234;
	boolean active = Math.random() < 0.5;
	int messagesSent = 0;

	// Size of ByteBuffer to accept incoming messages
	static int MAX_MSG_SIZE = 4096;
	int NUM_NBRS;
	boolean isZeroNode = false;
	ArrayList<InetSocketAddress> nbrs;
	String hostname;
	Params params;
	int nodeIndex;
	ArrayList<Integer> clock;
	PrintStream output;
	String parentNode;
	HashSet<String> childrenNodes;
	HashMap<String, SctpChannel> sendChannelMap;
	HashMap<String, SctpChannel> recvChannelMap;
	HashMap<String, ArrayList<Message>> channelStates;
	boolean isBlue;
	LocalState ls;
	ArrayList<HashMap<String, LocalState>> snapshots;
	int markerReceipts;
	public final Object mapLock = new Object();
	public final Object parentNodeLock = new Object();
	public final Object floodSendersLock = new Object();
	public final Object markerLock = new Object();
	public final Object endFPLock = new Object();
	public final Object endLock = new Object();
	public final Object endCLPSendLock = new Object();
	public int endFPCount;
	public boolean endCLPSendFlag;
	public boolean endFlag;
	ArrayList<String> floodSenders;

	Setup(ArrayList<InetSocketAddress> nbrs, String myHostname, int myPort, Params p, int myIdx, PrintStream op) {
		this.nbrs = nbrs;
		this.NUM_NBRS = nbrs.size();
		this.hostname = myHostname;
		this.PORT = myPort;
		this.isZeroNode = myIdx == 0;
		this.params = p;
		this.nodeIndex = myIdx;
		clock = new ArrayList<Integer>();
		for (int i = 0; i < p.numNodes; i++) {
			clock.add(0);
		}
		output = op;
		sendChannelMap = new HashMap<String, SctpChannel>();
		recvChannelMap = new HashMap<String, SctpChannel>();
		channelStates = new HashMap<String, ArrayList<Message>>();
		isBlue = true;
		ls = new LocalState(myHostname, nodeIndex);
		snapshots = new ArrayList<HashMap<String, LocalState>>();
		markerReceipts = 0;
		endCLPSendFlag = false;
		childrenNodes = new HashSet<String>();
		floodSenders = new ArrayList<String>();
		parentNode = "";
		endFPCount = 0;
		endFlag = false;

	}

	public void incrementSnapshotSize() {
		synchronized (snapshots) {
			this.snapshots.add(new HashMap<String, LocalState>());
		}
	}

	public int getSnapshotNumber() {
		synchronized (snapshots) {
			return snapshots.size();
		}
	}

	public void addSnapshot(String h, LocalState l) {
		synchronized (snapshots) {
			int s = snapshots.size();
			this.snapshots.get(s - 1).put(h, l);
		}
	}

	/**
	 * State methods
	 */
	public void setActive(boolean s) {
		// System.out.println("STATE changed to");
		// System.out.println(s);
		synchronized (mapLock) {
			this.active = s;
			return;
		}

	}

	/**
	 * State methods
	 */
	public boolean isActive() {
		synchronized (mapLock) {
			return this.active;
		}
	}

	/**
	 * MAP Message counts methods
	 */
	public void incrementMessagesSent() {
		synchronized (mapLock) {
			this.messagesSent += 1;
		}
	}

	/**
	 * MAP Message counts methods
	 */
	public int getMessagesSent() {
		synchronized (mapLock) {
			return this.messagesSent;
		}
	}

	/**
	 * Clock methods
	 */
	public ArrayList<Integer> getTimeStamp() {
		synchronized (mapLock) {
			return this.clock;
		}
	}

	/**
	 * Clock methods
	 */
	public ArrayList<Integer> setClockAfterInternalEvent(int inc) {
		synchronized (mapLock) {
			this.clock.set(nodeIndex, this.clock.get(nodeIndex) + inc);
			return this.clock;
		}
	}

	/**
	 * Clock methods
	 */
	public ArrayList<Integer> setClockBeforeMesgSend() {
		synchronized (mapLock) {
			this.clock.set(this.nodeIndex, this.clock.get(this.nodeIndex) + 1);
			return this.clock;

		}
	}

	/**
	 * Clock methods
	 */
	public ArrayList<Integer> getClockAfterMesgRecv(ArrayList<Integer> piggyback) {
		synchronized (mapLock) {
			for (int i = 0; i < this.clock.size(); i++) {
				this.clock.set(i, Math.max(this.clock.get(i), piggyback.get(i)));
			}
			this.clock.set(this.nodeIndex, this.clock.get(this.nodeIndex) + 1);
			return this.clock;
		}
	}

	/**
	 * Send message methods
	 */
	public void sendMapMesg(SctpChannel sc, MessageInfo mInfo, String recvr) throws Exception {

		synchronized (mapLock) {
			setClockBeforeMesgSend();

			Message msg = new Message((int) Math.floor(Math.random() * (1001)), MessageType.map, this.getTimeStamp());

			try {
				sc.send(msg.toByteBuffer(), mInfo);

			} catch (Exception e) {
				System.out.println("SENDING MAP MESSAGE TO " + recvr + " FAILED");
				setClockAfterInternalEvent(-1);
				e.printStackTrace();
			}

			this.incrementMessagesSent();

			// System.out.println("[" + this.hostname + "] : MAP MESSAGE " + messagesSent +
			// " SUCCESFULLY SENT to " + recvr);
			return;
		}

	}

	/**
	 * Send methods
	 */
	public void forwardLocalStateToNodeZero(Message ls_mesg) {

		MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
		SctpChannel sc = sendChannelMap.get(this.parentNode);
		try {
			sc.send(ls_mesg.toByteBuffer(), messageInfo);
		} catch (Exception e) {
			System.out.println("[" + hostname + "]:SENDING FORWARD MESSAGE TO " + parentNode + " FAILED");
			// e.printStackTrace();
		}
		// System.out.println("[" + this.hostname + "]: SENT forward mesg " +
		// ls_mesg.ls.clock + " to " + parentNode);

	}

	public void forwardMessageToNodeZero(Message mesg) {
		if (mesg.msgType != MessageType.converge_cast) {
			System.out.println("[" + this.hostname + "] : Invalid message passed as arg");
		}

		MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
		SctpChannel sc = sendChannelMap.get(this.parentNode);
		try {
			sc.send(mesg.toByteBuffer(), messageInfo);
		} catch (Exception e) {
			System.out.println("[" + this.hostname + "] : SENDING coverge cast MESSAGE TO " + parentNode + " FAILED");
			// e.printStackTrace();
		}
		// System.out.println("[" + this.hostname + "] : SENT coverge cast MESSAGE to "
		// + parentNode);

	}

	public void recvConvergeCastMesg(SctpChannel rc, Message recvd, String senderHostname) throws Exception {
		if (this.isZeroNode) {

			if (recvd.message.equals("END_FLOOD_PROTOCOL")) {

				synchronized (endFPLock) {
					this.endFPCount++;
					if (this.endFPCount == params.numNodes - 1) {
						endFPLock.notify();
					}
				}
			}

		} else {

			this.forwardMessageToNodeZero(recvd);
		}
	}

	/**
	 * Send methods
	 */
	public void sendEndMesg() {
		if (childrenNodes.size() != 0) {

			// end protocol by sending end messages to all children
			System.out.println("7" + "[" + hostname + "]:SENDING END message to " + childrenNodes.toString());
			ExecutorService sendEndMesgExecutor = Executors.newFixedThreadPool(childrenNodes.size());
			for (String childHost : childrenNodes) {
				Message endMessage = new Message("END", MessageType.end);
				MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
				sendEndMesgExecutor.submit(new Runnable() {
					public void run() {
						try {
							sendChannelMap.get(childHost).send(endMessage.toByteBuffer(), messageInfo);
						} catch (Exception e) {
							System.out.println("[" + hostname + "]:SENDING END message to " + childHost + " failed ");
							e.printStackTrace();
						}
					};
				});

			}
			sendEndMesgExecutor.shutdown();
			try {
				sendEndMesgExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				System.out.println("[" + hostname + "]:SENDING end messages not terminated");
			}
		}
		synchronized (endLock) {
			endFlag = true;
			endLock.notify();
		}
	}

	/**
	 * Receive methods
	 */
	public void recvFloodMesg(SctpChannel rc, Message recvd, String senderHostname) throws Exception {
		synchronized (this.floodSendersLock) {
			this.floodSenders.add(senderHostname);
		}
		synchronized (this.parentNodeLock) {
			if (this.parentNode.equals("")) {
				this.parentNode = senderHostname;
				this.ls.setParent(senderHostname);
			}
			this.parentNodeLock.notify();
		}
	}

	/**
	 * Receive methods
	 */
	public void recvFloodAckMesg(SctpChannel rc, Message recvd, String senderHostname) throws Exception {
		synchronized (this.childrenNodes) {
			this.childrenNodes.add(senderHostname);
		}
	}

	/**
	 * Receive methods
	 */
	public void recvMapMesg(SctpChannel rc, Message recvd, String senderHostname) throws Exception {
		synchronized (mapLock) {

			// String msg = recvd.message;
			this.channelStates.get(senderHostname).add(recvd);// CL

			this.getClockAfterMesgRecv(recvd.piggyback);

			if (!this.isActive()) {
				int k = this.getMessagesSent();
				if (k < params.maxNumber) {
					// System.out.println("# OF MAP MESSAGES SENT = " + Integer.toString(k) + "< MAX
					// NUMBER ("
					// + Integer.toString(params.maxNumber) + ")");
					this.setActive(true);
				} else {
					// System.out.println("# OF MAP MESSAGES SENT = " + Integer.toString(k) + ">=
					// MAX NUMBER ("
					// + Integer.toString(params.maxNumber) + ")");
					// System.out.println("NO CHANGE IN STATE");
				}
			}

			// System.out.println("[" + this.hostname + "]: MAP Message received from
			// client: " + senderHostname);

			return;
		}
	}

	/**
	 * Receive methods
	 */
	public void recvMarkerMesg(SctpChannel rc, Message recvd, String senderHostname) throws Exception {
		// System.out.println("Received marker from " + senderHostname);
		this.recvMarkerRule(senderHostname);
		this.addMarkerReceipt(senderHostname);
		if (recvd.ls.nodeIndex != 0) {// sender is not zero node
			if (recvd.ls.parent.equals(this.hostname)) {
				this.childrenNodes.add(senderHostname);
			}
		}

		return;
	}

	/**
	 * Receive methods
	 */
	public void recvForwardMesg(SctpChannel rc, Message recvd, String senderHostname) throws Exception {
		// if node zero, message is meant for me, so capture snapshot
		if (this.isZeroNode) {
			this.captureSnapshot(recvd);
			// System.out.println("Received forward from " + senderHostname);
			// System.out.println(recvd.ls.hostname + ":" + recvd.ls.clock);

		} else {
			// else, forward it to node zero
			this.forwardLocalStateToNodeZero(recvd);
		}
	}

	/**
	 * CL methods: called by node zero initially; called by other nodes on receiving
	 * marker
	 */

	// change color if blue

	public boolean ifBlueChange() {
		boolean x = false;
		synchronized (this) {
			if (isBlue) {
				x = true;
				isBlue = false;
			}
			return x;
		}
	}

	public boolean isBlue() {
		synchronized (this) {
			return isBlue;
		}
	}

	void printClock(ArrayList<Integer> c) {
		String s = "";
		for (Integer i : c) {
			s = s + " " + Integer.toString(i);
		}
		output.println(s);
		return;
	}

	public void changeColor() {
		if (this.ifBlueChange()) {

			if (!isZeroNode) {

				Thread clp = new Thread(new CLPThread(sendChannelMap, recvChannelMap, params, this, isZeroNode));
				clp.setDaemon(true);
				clp.start();
			}

			this.recordLocalState();
			// record local state

			if (!isZeroNode) {

				Message ls_mesg = new Message(this.ls, MessageType.forward);
				this.forwardLocalStateToNodeZero(ls_mesg);
				printClock(ls_mesg.ls.clock);
				// if not node zero, then send recorded local state to node zero
			} else {

				synchronized (this.snapshots) {
					this.addSnapshot(hostname, ls.deepclone());
					this.snapshots.notifyAll();
				}

				printClock(this.ls.clock);
				// System.out.println( ls.hostname + ":" + ls.clock);
			}

			this.markerSendingRule();
			// send markers to all outgoing channels
		}

	}

	/**
	 * Marker count methods
	 */
	public void addMarkerReceipt(String sender) {
		synchronized (markerLock) {
			markerReceipts++;
			markerLock.notifyAll();
		}
	}

	/**
	 * marker methods
	 */
	public void markerSendingRule() {

		for (String recvrNode : sendChannelMap.keySet()) {
			SctpChannel sc = sendChannelMap.get(recvrNode);
			Message msg = new Message(this.ls, MessageType.marker);
			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
			try {
				sc.send(msg.toByteBuffer(), messageInfo);
			} catch (Exception e) {
				System.out.println("[" + this.hostname + "]: SENDING MARKER MESSAGE TO " + recvrNode + " FAILED");
				// e.printStackTrace();
			}
			// System.out.println("[" + this.hostname + "]: SENT MARKER mesg to " +
			// recvrNode);
		}
		synchronized (endCLPSendLock) {
			this.endCLPSendFlag = true;
			// set flag to signal all markers sent
			endCLPSendLock.notifyAll();
		}

	}

	/**
	 * marker methods
	 */
	public void recvMarkerRule(String sender) {
		if (!isZeroNode) {
			this.changeColor();
		}
		this.recordChannelState(sender);

	}

	/**
	 * LocalState methods
	 */
	public void recordLocalState() {
		synchronized (mapLock) {
			this.ls.storeStateAndTimestamp(this.isActive(), this.getTimeStamp());
		}
		// store current state : active/passive and current clock value
	}

	/**
	 * LocalState methods
	 */
	public void clearRecords() {

		for (String key : this.channelStates.keySet()) {
			this.channelStates.put(key, new ArrayList<Message>());
		}
		this.ls.stopRecording();
		synchronized (this.markerLock) {
			this.markerReceipts = 0;
			markerLock.notifyAll();
		}
		synchronized (endCLPSendLock) {
			this.endCLPSendFlag = false;
			endCLPSendLock.notifyAll();
		}

	}

	/**
	 * Channel states methods
	 */
	public void recordChannelState(String senderHost) {
		this.ls.channelStates.put(senderHost, new ArrayList<Message>());
		for (Message m : this.channelStates.get(senderHost)) {
			this.ls.channelStates.get(senderHost).add(m);
		}
		this.channelStates.get(senderHost).clear();
		return;

	}

	/**
	 * Snapshot methods
	 */
	public void captureSnapshot(Message ls_mesg) {
		if (ls_mesg.msgType.equals(MessageType.forward)) {
			// System.out.println(ls_mesg.ls.hostname + " : " + ls_mesg.ls.clock);
			// System.out.println(ls_mesg.ls.channelStates.toString());

			synchronized (this.snapshots) {
				this.addSnapshot(ls_mesg.ls.hostname, ls_mesg.ls);
				this.snapshots.notifyAll();
			}

		}

	}

	/**
	 * Snapshot methods
	 */
	public HashMap<String, LocalState> getSnapshots() {
		synchronized (this.snapshots) {
			int s = snapshots.size();
			return this.snapshots.get(s - 1);
		}

	}

	public void start() {

		try {
			if (isZeroNode) {
				setActive(true);
			}
			InetSocketAddress addr = new InetSocketAddress(PORT); // Get address from port number

			System.out.println("[" + this.hostname + "]: Starting node..");
			SctpServerChannel ssc;
			try {
				ssc = SctpServerChannel.open();// Open server channel
				ssc.bind(addr);// Bind server channel to address
			} catch (Exception ee) {
				System.out.println("Server creation failed. Exiting.");
				return;
			}
			// Server creation successful

			Thread.sleep(2000); // wait for all machines to start

			// Send connection requests to nbrs
			for (int i = 0; i < nbrs.size(); i++) {
				System.out.println("[" + this.hostname + "]: Trying connection to server " + nbrs.get(i).getHostName());
				// Connect to server using the socket address
				try {
					// Store send channels in hashmap
					sendChannelMap.put(nbrs.get(i).getHostName(), SctpChannel.open(nbrs.get(i), 0, 0));

				} catch (Exception e) {
					System.out.println("Connection to " + nbrs.get(i).getHostName() + " failed.");
					// e.printStackTrace();
					continue;
				}
				System.out.println("[" + this.hostname + "]: Connected to Server " + nbrs.get(i).getHostName());

			}

			// Accept connections from nbrs on listen port

			while (recvChannelMap.size() < NUM_NBRS) {
				SctpChannel sc = ssc.accept(); // Wait for incoming connection from client
				String senderAddr = sc.getRemoteAddresses().toArray()[0].toString();
				output.println(">>>>>>>>" + senderAddr);
				output.println(">>>>>>>" + sc);
				String senderHost = InetAddress
						.getByName(sc.getRemoteAddresses().toArray()[0].toString().substring(1).split(":")[0])
						.getHostName();

				// Store channel in hashmap
				recvChannelMap.put(senderHost, sc);

				System.out.println("[" + this.hostname + "]: Client " + senderHost + " connected");

				// Initialize channel states for each incoming connection
				// synchronized (mapLock) {
				channelStates.put(senderHost, new ArrayList<Message>());
				// }

			}

			// Thread to receive any incoming message and operate on it
			Thread rt = new Thread(new MesgReceiveThread(recvChannelMap, params, this));
			rt.setDaemon(true);
			rt.start();

			// Start Flooding to find parent node
			new FloodProtocol(sendChannelMap, params, this, isZeroNode).run();

			if (isZeroNode) {
				synchronized (endFPLock) {
					while (endFPCount != params.numNodes - 1) {
						endFPLock.wait();
					}
				}
				System.out.println("[" + this.hostname + "]: Node zero finished flood");
			}

			// Start MAP
			Thread map = new Thread(new MAPSendThread(sendChannelMap, params, this));
			map.setDaemon(true);
			map.start();
			Thread clp = new Thread(new CLPThread(sendChannelMap, recvChannelMap, params, this, isZeroNode));

			if (isZeroNode) {
				clp.setDaemon(true);
				clp.start();
			}

			synchronized (endLock) {
				while (!endFlag) {
					endLock.wait();
				}
				rt.join();
				System.out.println("[" + this.hostname + "]: Exiting RT succesfully");

				map.join();
				System.out.println("[" + this.hostname + "]: Exiting MAP succesfully");

				if (isZeroNode) {
					clp.join();
					System.out.println("[" + this.hostname + "]: Exiting CLP succesfully");

				}
				for (SctpChannel sc : sendChannelMap.values()) {
					sc.close();
				}
				for (SctpChannel rc : recvChannelMap.values()) {
					rc.close();
				}
				System.out.println("[" + this.hostname + "]: Exiting program succesfully");

				return;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
