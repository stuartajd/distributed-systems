/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab1;

/**
 *
 * @author up772629
 */
public class QuadPi extends Thread {
    public static void main(String[] args) throws Exception {

        double startTime = System.nanoTime();

        int steps = numSteps / 4;
        
        QuadPi thread1 = new QuadPi();
        thread1.begin = 0;
        thread1.end = steps;

        QuadPi thread2 = new QuadPi();
        thread2.begin = steps;
        thread2.end = steps * 2;
        
        QuadPi thread3 = new QuadPi();
        thread3.begin = steps * 2;
        thread3.end = steps * 3;
        
        QuadPi thread4 = new QuadPi();
        thread4.begin = steps * 3;
        thread4.end = steps * 4;

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        double endTime = System.nanoTime();

        double pi = step * (thread1.sum + thread2.sum + thread3.sum + thread4.sum);

        System.out.println("Value of pi: " + pi);

        System.out.println("Calculated in "
                + ((endTime - startTime) / 1000000) + " milliseconds");
    }

    static int numSteps = 10000000;

    static double step = 1.0 / (double) numSteps;

    double sum;
    int begin, end;

    public void run() {

        sum = 0.0;

        for (int i = begin; i < end; i++) {
            double x = (i + 0.5) * step;
            sum += 4.0 / (1.0 + x * x);
        }
    }
}
