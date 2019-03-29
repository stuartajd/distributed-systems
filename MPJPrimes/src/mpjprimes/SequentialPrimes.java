package mpjprimes;

import java.util.ArrayList;

public class SequentialPrimes {
    /**
     * Write a parallel version of a simple prime number sieve. To test a number n for primeness, 
     * just try dividing it by all numbers from 2 to sqrt(n). A fairly naive sieve tests all numbers 
     * from 2 to N in this way and returns a list of all prime numbers in this range.
     * 
     * This problem is suitable for implementing as an MPJ task farm. As a single task, the master sends 
     * a subrange of numbers to a worker (just a start value and end value for the range will do). 
     * 
     * The worker tests them for primality, and returns a list (array) of prime numbers. The master 
     * should concatenate all the lists together and output them as one long list, in the right order, 
     * at the end of the run (you don't need include the final output stage in your timing).
     */
    
    final static int S = 1; // Start of the sieve
    final static int N = 10000; // End point should be N

    
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        ArrayList<Integer> primes = new ArrayList<>(); // Captured prime list

        
        for(int i = S; i < N; i++){
            // Goes through all numbers between start and end
            if(isNumberPrime(i)){
                primes.add(i);
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("Calculated "+ N +" primes in " + (endTime - startTime) + "ms (Found: "+ primes.size() + ")");
    }
    
    public static boolean isNumberPrime(int number) {  
        if(number <= 1) return false;
        for(int i = 2; i <= Math.sqrt(number); ++i)
        {
            if(number % i == 0) return false;
        }
        
        return true;  
   }  
}
