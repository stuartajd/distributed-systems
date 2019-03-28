
import java.awt.*;
import javax.swing.*;
import java.util.concurrent.*;

public class LaplaceParallel extends Thread {

    final static int P = 8; // Threads
    final static int N = 256;
    final static int CELL_SIZE = 2;
    final static int NITER = 100000;
    final static int OUTPUT_FREQ = 1000;

    final static int DELAY = 0;
    static CyclicBarrier barrier = new CyclicBarrier(P);

    static float[][] phi = new float[N][N];
    static float[][] newPhi = new float[N][N];

    static Display display = new Display();

    public static void main(String args[]) throws Exception {
        // Main update loop.
        long startTime = System.currentTimeMillis();

        // Make voltage non-zero on left and right edges
        for (int j = 0; j < N; j++) {
            phi[0][j] = 1.0F;
            phi[N - 1][j] = 1.0F;
        }

        display.repaint();
        pause();

        LaplaceParallel[] threads = new LaplaceParallel[P];
        for (int me = 0; me < P; me++) {
            threads[me] = new LaplaceParallel(me);
            threads[me].start();
        }

        for (int me = 0; me < P; me++) {
            threads[me].join();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");
    }

    int me;

    LaplaceParallel(int me) {
        this.me = me;
    }

    final static int B = NITER / P;  // block size

    public void run() {
        int begin = me * B;
        int end = begin + B;

        for (int iter = begin; iter < end; iter++) {

            // Calculate new phi
            for (int i = 1; i < N - 1; i++) {
                for (int j = 1; j < N - 1; j++) {

                    newPhi[i][j]
                            = 0.25F * (phi[i][j - 1] + phi[i][j + 1]
                            + phi[i - 1][j] + phi[i + 1][j]);
                }
            }

            synch();

            // Update all phi values
            for (int i = 1; i < N - 1; i++) {
                for (int j = 1; j < N - 1; j++) {
                    phi[i][j] = newPhi[i][j];
                }
            }

            synch();

            if (iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter);
                display.repaint();
            }

            pause();
        }
    }

    static void synch() {
        try {
            barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void pause() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
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
                    float f = phi[i][j];
                    Color c = new Color(f, 0.0F, 1.0F - f);
                    g.setColor(c);
                    g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                            CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }
}
