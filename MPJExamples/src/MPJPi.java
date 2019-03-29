
import mpi.*;

public class MPJPi {

    final static int N = 100000000;

    public static void main(String args[]) throws Exception {
        MPI.Init(args);

        long startTime = System.currentTimeMillis();

        int me = MPI.COMM_WORLD.Rank();
        int P = MPI.COMM_WORLD.Size();

        int b = N / P;
        int begin = me * b;
        int end = begin + b;

        double step = 1.0 / (double) N;

        double sum = 0.0;

        for (int i = begin; i < end; i++) {
            double x = (i + 0.5) * step;
            sum += 4.0 / (1.0 + x * x);
        }

        if (me > 0) {
            double[] sendBuf = new double[]{sum};  // 1-element array containing sum
            MPI.COMM_WORLD.Send(sendBuf, 0, 1, MPI.DOUBLE, 0, 0);
        } else {
            double[] recvBuf = new double[1];
            for (int src = 1; src < P; src++) {
                MPI.COMM_WORLD.Recv(recvBuf, 0, 1, MPI.DOUBLE, src, 0);
                sum += recvBuf[0];
            }
        }

        double pi = step * sum;

        long endTime = System.currentTimeMillis();

        if (me == 0) {
            System.out.println("Value of pi: " + pi);
            System.out.println("Calculated in "
                    + (endTime - startTime) + " milliseconds");
        }

        MPI.Finalize();
    }
}
