/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab1;

/**
 *
 * Value of pi: 3.141592653589731
 * Calculated in 86 milliseconds
 * 
 * Value of pi: 3.141592653589731
 * Calculated in 108 milliseconds
 * 
 * Value of pi: 3.141592653589731
 * 1Calculated in 94 milliseconds
 */
  public class SequentialPi {

      public static void main(String[] args) {

          long startTime = System.currentTimeMillis();

          int numSteps = 100000000;

          double step = 1.0 / (double) numSteps;

          double sum = 0.0;

          for(int i = 0 ; i < numSteps ; i++){
              double x = (i + 0.5) * step ;
              sum += 4.0 / (1.0 + x * x);
          }

          double pi = step * sum ;

          long endTime = System.currentTimeMillis();

          System.out.println("Value of pi: " + pi);

          System.out.println("Calculated in " +
                             (endTime - startTime) + " milliseconds");
      }
  }}
