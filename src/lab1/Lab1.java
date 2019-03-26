package lab1;

public class Lab1 extends Thread{

    public static void main(String[] args) throws Exception {
        Lab1 thread1 = new Lab1();
        thread1.begin = 0 ;
        thread1.end = numSteps / 2 ;

        Lab1 thread2 = new Lab1();
        thread2.begin = numSteps / 2 ;
        thread2.end = numSteps ;

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        double pi = step * (thread1.sum + thread2.sum) ;

        System.out.println("Value of pi: " + pi);
    }
    
    
    static int numSteps = 10000000;
    
    static double step = 1.0 / (double) numSteps;

    double sum ;  
    int begin, end ;

    public void run() {

        sum = 0.0 ;

        for(int i = begin ; i < end ; i++){
            double x = (i + 0.5) * step ;
            sum += 4.0 / (1.0 + x * x);
        }
    }
    
}
