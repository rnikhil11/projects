public class Params {
    int numNodes;
    int interRequestDelay;
    int csExecTime;
    int numRequests;

    Params(int a, int b, int c, int d) {
        this.numNodes = a;
        this.interRequestDelay = b;
        this.csExecTime = c;
        this.numRequests = d;
    }
}