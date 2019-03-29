import mpi.*;

import java.awt.*;
import javax.swing.*;

public class MPJLaplace {

    final static int N = 256;
    final static int CELL_SIZE = 2;
    final static int NITER = 100000;
    final static int OUTPUT_FREQ = 1000;

    static int P, me, B;

    static float[][] phi;
    static float[][] newPhi;

    static float[][] allPhi;  // temporary array for display

    static Display display;

    public static void main(String args[]) throws Exception {

        MPI.Init(args);

        me = MPI.COMM_WORLD.Rank();
        P = MPI.COMM_WORLD.Size();

        if (me == 0) {
            allPhi = new float[N][N];  // used for display only
            display = new Display();
        }

        B = N / P;

        phi = new float[B + 2][N];
        newPhi = new float[B + 2][N];

        // Make voltage non-zero on left and right edges
        if (me == 0) {
            for (int j = 0; j < N; j++) {
                phi[1][j] = 1.0F;
            }
        }

        if (me == P - 1) {
            for (int j = 0; j < N; j++) {
                phi[B][j] = 1.0F;
            }
        }

        displayPhi();

        // Don't change ghost regions in update loops
        int begin = 1;
        int end = B + 1;

        // Don't update fixed boundary values
        if (me == 0) {
            begin = 2;
        }

        if (me == P - 1) {
            end = B;
        }

        // Main update loop.
        long startTime = System.currentTimeMillis();

        for (int iter = 0; iter < NITER; iter++) {

            // Edge swap
            int next = (me + 1) % P;
            int prev = (me - 1 + P) % P;
            MPI.COMM_WORLD.Sendrecv(phi[B], 0, N, MPI.FLOAT, next, 0,
                    phi[0], 0, N, MPI.FLOAT, prev, 0);
            MPI.COMM_WORLD.Sendrecv(phi[1], 0, N, MPI.FLOAT, prev, 0,
                    phi[B + 1], 0, N, MPI.FLOAT, next, 0);

            // Calculate new phi
            for (int i = begin; i < end; i++) {
                for (int j = 1; j < N - 1; j++) {

                    newPhi[i][j]
                            = 0.25F * (phi[i][j - 1] + phi[i][j + 1]
                            + phi[i - 1][j] + phi[i + 1][j]);
                }
            }

            // Update all phi values
            for (int i = begin; i < end; i++) {
                for (int j = 1; j < N - 1; j++) {
                    phi[i][j] = newPhi[i][j];
                }
            }

            if (iter % OUTPUT_FREQ == 0) {
                if (me == 0) {
                    System.out.println("iter = " + iter);
                }
                displayPhi();
            }
        }

        long endTime = System.currentTimeMillis();

        if (me == 0) {
            System.out.println("Calculation completed in "
                    + (endTime - startTime) + " milliseconds");
        }

        displayPhi();

        MPI.Finalize();
    }

    public static void displayPhi() {

        if (me > 0) {
            MPI.COMM_WORLD.Send(phi, 1, B, MPI.OBJECT, 0, 0);
        } else {  // me == 0
            for (int i = 1; i <= B; i++) {
                for (int j = 0; j < N; j++) {
                    allPhi[i - 1][j] = phi[i][j];
                }
            }
            for (int src = 1; src < P; src++) {
                MPI.COMM_WORLD.Recv(allPhi, src * B, B, MPI.OBJECT, src, 0);
            }

            display.repaint();
        }
    }

    static class Display extends JPanel {

        final static int WINDOW_SIZE = N * CELL_SIZE;

        Display() {
            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

            JFrame frame = new JFrame("Laplace");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    float f = allPhi[i][j];
                    Color c = new Color(f, 0.0F, 1.0F - f);
                    g.setColor(c);
                    g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                            CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }
}
