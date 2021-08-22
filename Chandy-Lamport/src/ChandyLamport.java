import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class ChandyLamport {
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        Scanner sc = new Scanner(file);
        String line;
        String myHostname = InetAddress.getLocalHost().getHostName();

        int i = 0;

        int numNodes = 0, minPerActive = 0, maxPerActive = 0, minSendDelay = 0, snapshotDelay = 0, maxNumber = 0;
        // snapshotDelay is the amount of time to wait between initiating snapshots in
        // the Chandy and Lamport algorithm
        ArrayList<ArrayList<Integer>> nbrMap = new ArrayList<ArrayList<Integer>>();
        ArrayList<String> nodes = new ArrayList<String>();
        ArrayList<Integer> listenPorts = new ArrayList<Integer>();
        int myIndex = -1;
        while (sc.hasNextLine())
            try {
                line = sc.nextLine().trim();
                try {
                    Integer.parseInt(line.substring(0, 1));
                } catch (NumberFormatException nfe) {
                    continue;
                }
                if (i == 0) {
                    String[] tokens = line.split("#")[0].trim().split(" ");
                    if (tokens.length != 6) {
                        System.out.println("ERROR IN READING FIRST LINE");
                    }
                    numNodes = Integer.parseInt(tokens[0]);
                    minPerActive = Integer.parseInt(tokens[1]);
                    maxPerActive = Integer.parseInt(tokens[2]);
                    minSendDelay = Integer.parseInt(tokens[3]);
                    snapshotDelay = Integer.parseInt(tokens[4]);
                    maxNumber = Integer.parseInt(tokens[5]);

                } else {
                    if (i < numNodes + 1) {
                        String[] tokens = line.split("#")[0].trim().split(" ");
                        if (tokens.length != 3) {
                            System.out.println("ERROR IN READING LATER LINE");
                            return;
                        }
                        int nodeId = Integer.parseInt(tokens[0]);
                        String hostname = tokens[1] + ".utdallas.edu";
                        int listenPort = Integer.parseInt(tokens[2]);
                        if (nodeId != nodes.size()) {
                            System.out.println("ERROR IN READING NBRS");
                            return;
                        }
                        nodes.add(hostname);
                        listenPorts.add(listenPort);
                        if (myHostname.equals(hostname)) {
                            myIndex = nodes.size() - 1;
                        }
                    } else {
                        String[] tokens = line.split("#")[0].trim().split(" ");
                        ArrayList<Integer> nbrs = new ArrayList<Integer>();
                        for (String t : tokens) {
                            nbrs.add(Integer.parseInt(t));
                        }
                        nbrMap.add(nbrs);
                    }
                }
                i++;
            } catch (IllegalArgumentException e) {
                System.out.println("Unsuccessful CONFIG READ");
                e.printStackTrace();
            }
        sc.close();

        if (myIndex != -1) {
            String writeFile = args[0] + "-" + Integer.toString(myIndex) + ".out";
            FileOutputStream outputStream = new FileOutputStream(writeFile);
            PrintStream output = new PrintStream(outputStream);

            ArrayList<Integer> myNbrs = nbrMap.get(myIndex);
            ArrayList<InetSocketAddress> nbrs = new ArrayList<InetSocketAddress>();
            for (Integer k : myNbrs) {
                nbrs.add(new InetSocketAddress(nodes.get(k), listenPorts.get(k)));
            }
            Params p = new Params(minPerActive, maxPerActive, minSendDelay, snapshotDelay, maxNumber, numNodes);
            new Setup(nbrs, nodes.get(myIndex), listenPorts.get(myIndex), p, myIndex, output).start();
            output.close();
        }
        return;

    }

}
