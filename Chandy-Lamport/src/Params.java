public class Params {
    int numNodes;
    int minPerActive;
    int maxPerActive;
    int minSendDelay;
    int snapshotDelay;
    int maxNumber;

    Params(int a, int b, int c, int d, int e, int f) {
        this.minPerActive = a;
        this.maxPerActive = b;
        this.minSendDelay = c;
        this.snapshotDelay = d;
        this.maxNumber = e;
        this.numNodes = f;
    }
}