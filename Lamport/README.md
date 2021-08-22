FILES:
Lamport.java:           Has the main method. Reads the config file name from command line and calls the start() method of Setup class.
Setup.java :            Sets up connections to send and receive messages from the node's neighbours and then    initiates application.
MesgRecvThread.java:    Create a RecvThread for each incoming channel
RecvThread.java:        For a given incoming channel, calls methods for operating on messages received based on message type.
Params.java:            Structure for passing configuration parameters.
Message.java:           Message class for different types of messages and to convert to/from byte buffers.
App.java:               Application class which generates CS requests
Mutex.java:             Mutex class which maintains priority queue and provides cs_enter() and cs_leave()
CSRequest.java          Class which has nodeId and timestamp
CSInfo.java             Class which is used to send information for verification          
recordings.out          Output file which stores all the recorded values in format: 
                        n d c avgResponsetime Throughput messageComplexity
Graph.py                To plot graphs
COMPILE:
javac *.java

EXECUTE: 
java Lamport config nodeId

OUTPUTS:
Outputs debugging information in standard output.
Each node enters its local snapshots in config-<i>.out where i is the node number.

SCRIPTS:
After compiling, program can be executed with following scripts.
launcher.sh > output : to run the program
cleanup.sh : Kills all processes by user.
