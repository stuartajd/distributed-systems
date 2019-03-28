package lab2;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.File;

/**
 * Average from Sequential: 983ms / Lowest: 780ms
 * 
 * Loop on i
 * 2 Threads: 469ms / 648ms / 427ms / 394ms / 406ms (Average: 469ms - Speedup: 2.096) - (Lowest: 394ms - Speedup: 1.980)
 * 4 Threads: 328ms / 373ms / 393ms / 345ms / 282ms (Average: 344ms - Speedup: 2.858) - (Lowest: 282ms - Speedup: 2.766)
 * 8 Threads: 266ms / 266ms / 303ms / 373ms / 297ms (Average: 301ms - Speedup: 3.267) - (Lowest: 266ms - Speedup: 2.932)
 * 
 * Loop on j
 * 2 Threads: 391ms / 394ms / 394ms / 403ms / 647ms (Average: 455ms - Speedup: 2.160) - (Lowest: 391ms - Speedup: 1.995)
 * 4 Threads: 344ms / 332ms / 378ms / 262ms / 414ms (Average: 346ms - Speedup: 2.841) - (Lowest: 332ms - Speedup: 2.349)
 * 8 Threads: 313ms / 313ms / 278ms / 266ms / 350ms (Average: 304ms - Speedup: 3.234) - (Lowest: 266ms - Speedup: 2.932)
 */
public class ParallelMandelbrot extends Thread {

    final static int P = 8;
    final static int N = 4096;
    final static int CUTOFF = 100;

    static int[][] set = new int[N][N];

    public static void main(String[] args) throws Exception {

        // Calculate set
        long startTime = System.currentTimeMillis();

        ParallelMandelbrot[] threads = new ParallelMandelbrot[P];
        for (int me = 0; me < P; me++) {
            threads[me] = new ParallelMandelbrot(me);
            threads[me].start();
        }

        for (int me = 0; me < P; me++) {
            threads[me].join();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");

        // Plot image
        BufferedImage img = new BufferedImage(N, N,
                BufferedImage.TYPE_INT_ARGB);

        // Draw pixels
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                int k = set[i][j];

                float level;
                if (k < CUTOFF) {
                    level = (float) k / CUTOFF;
                } else {
                    level = 0;
                }
                Color c = new Color(0, level, 0);  // Green
                img.setRGB(i, j, c.getRGB());
            }
        }

        // Print file
        ImageIO.write(img, "PNG", new File("MandelbrotParallel.png"));
    }

    int me;

    public ParallelMandelbrot(int me) {
        this.me = me;
    }

    public void run() {

        int begin, end;

        int b = N / P;  // block size
        begin = me * b;
        end = begin + b;
        
        for (int i = me; i < N; i += P) {
//        for (int i = 0; i < N; i++) {
//            for (int j = me; j < N; j += P) {
            for (int j = 0; j < N; j++) {

                double cr = (4.0 * i - 2 * N) / N;
                double ci = (4.0 * j - 2 * N) / N;

                double zr = cr, zi = ci;

                int k = 0;
                while (k < CUTOFF && zr * zr + zi * zi < 4.0) {

                    // z = c + z * z
                    double newr = cr + zr * zr - zi * zi;
                    double newi = ci + 2 * zr * zi;

                    zr = newr;
                    zi = newi;

                    k++;
                }

                set[i][j] = k;
            }
        }
    }
}
