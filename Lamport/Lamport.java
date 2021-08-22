import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Scanner;

public class Lamport {
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        int myIndex = Integer.parseInt(args[1].trim());

        Scanner sc = new Scanner(file);
        String line;
        String myHostname = InetAddress.getLocalHost().getHostName();
        int i = 0;

        int numNodes = 0, interRequestDelay = 0, csExecTime = 0, numRequests = 0;

        HashMap<Integer, InetSocketAddress> nbrs = new HashMap<Integer, InetSocketAddress>();// map from process ID to
                                                                                             // channel
        int myPort = -1;
        while (sc.hasNextLine())
            try {
                line = sc.nextLine().trim().split("#")[0].trim();
                if (line.equals("")) {
                    continue;
                }
                try {
                    Integer.parseInt(line.substring(0, 1));
                } catch (NumberFormatException nfe) {
                    continue;
                }
                if (i == 0) {
                    String[] tokens = line.split("#")[0].trim().split(" ");
                    if (tokens.length != 4) {
                        System.out.println("ERROR IN READING FIRST LINE");
                    }

                    numNodes = Integer.parseInt(tokens[0]);
                    interRequestDelay = Integer.parseInt(tokens[1]);
                    csExecTime = Integer.parseInt(tokens[2]);
                    numRequests = Integer.parseInt(tokens[3]);

                } else {

                    String[] tokens = line.split(" ");
                    if (tokens.length != 3) {
                        System.out.println("ERROR IN READING LATER LINE");
                        return;
                    }
                    int nodeId = Integer.parseInt(tokens[0]);
                    String hostname = tokens[1] + ".utdallas.edu";
                    int listenPort = Integer.parseInt(tokens[2]);
                    if (nodeId >= numNodes) {
                        System.out.println("ERROR IN READING NBRS");
                        return;
                    }
                    if (nodeId == myIndex) {

                        myPort = listenPort;
                    } else {

                        nbrs.put(nodeId, new InetSocketAddress(hostname, listenPort));

                    }

                }
                i++;
            } catch (IllegalArgumentException e) {
                System.out.println("Unsuccessful CONFIG READ");
                e.printStackTrace();
            }
        sc.close();
        if (myPort != -1) {
            String writeFile = args[0] + "-" + Integer.toString(myIndex) + ".out";
            FileOutputStream outputStream = new FileOutputStream(writeFile);
            PrintStream output = new PrintStream(outputStream);
            Params p = new Params(numNodes, interRequestDelay, csExecTime, numRequests);
            new Setup(nbrs, myHostname, myPort, myIndex, p, output).start();
            output.close();
        }
        System.out.println(myIndex + ": Exiting");
        return;

    }

}
