/**
 * Time taken to execute:
 * 40252ms, 38613ms, 38901ms
 */

package lab7;

/**
 *
 * @author up772629
 */
import java.awt.*;
import javax.swing.*;

public class SlowMandelbrot {

    final static int N = 1024;
    final static int CUTOFF = 100000;

    static int[][] set = new int[N][N];

    public static void main(String[] args) throws Exception {

        Display display = new Display();

        // Special value for uninitialized pixels (rendered white below)
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                set[i][j] = -1;
            }
        }
        display.repaint();

        // Calculate set
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < N; i++) {
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
            display.repaint();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("Calculation completed in "
                + (endTime - startTime) + " milliseconds");
    }

    static class Display extends JPanel {

        Display() {

            setPreferredSize(new Dimension(N, N));

            JFrame frame = new JFrame("MandelBrot");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    int k = set[i][j];
                    Color c;

                    if (k == -1) {  // uninitialized
                        c = Color.WHITE;
                    } else {
                        float level;
                        if (k < CUTOFF) {
                            level = k < 50 ? (float) k / 50 : 1.0F;
                        } else {
                            level = 0;
                        }
                        c = new Color(level / 2, level / 2, level);  // Blueish
                    }

                    g.setColor(c);
                    g.fillRect(i, j, 1, 1);
                }
            }
        }
    }
}
