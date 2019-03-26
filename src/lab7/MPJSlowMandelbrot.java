/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab7;

/**
 *
 * @author up772629
 */
import mpi.* ;
  
  import java.awt.* ;
  import javax.swing.* ;

  public class MPJSlowMandelbrot {

      final static int N = 1024 ;
      final static int CUTOFF = 100000 ; 

      final static int BLOCK_SIZE = 4 ;  // rows in block of work

      final static int NUM_BLOCKS  = N / BLOCK_SIZE ;
      final static int BUFFER_SIZE = 1 + BLOCK_SIZE * N ;
      
      // tag values
      final static int TAG_HELLO   = 0 ;
      final static int TAG_RESULT  = 1 ;
      final static int TAG_TASK    = 2 ;
      final static int TAG_GOODBYE = 3 ;
      
      static int [] [] set ;

      public static void main(String [] args) throws Exception {
      
          MPI.Init(args) ;
		  
          int me = MPI.COMM_WORLD.Rank() ;
          int P = MPI.COMM_WORLD.Size() ;

          int numWorkers = P - 1 ;
          
          int [] buffer = new int [BUFFER_SIZE] ;
              
          if(me == 0) {  // master process - sends out work and displays results
          
              set = new int [N] [N] ;
              Display display = new Display() ;
		 
              for(int i = 0 ; i < N ; i++) {
                  for(int j = 0 ; j < N ; j++) {
                      set [i] [j] = -1 ;
                  }
              }
              display.repaint() ;

              // Calculate set

              long startTime = System.currentTimeMillis();

              int nextBlockStart = 0 ;
              int numHellos = 0 ;
              int numBlocksReceived = 0 ;
              
              while(numBlocksReceived < NUM_BLOCKS || numHellos < numWorkers) {

                  // Receive hello or results from any worker

                  Status status =
                          MPI.COMM_WORLD.Recv(buffer, 0, BUFFER_SIZE, MPI.INT,
                                              MPI.ANY_SOURCE, MPI.ANY_TAG) ;

                  if(status.tag == TAG_RESULT) {  

                      // Save returned results to `set' and display

                      int resultBlockStart = buffer [0] ;
                      for(int i = 0 ; i < BLOCK_SIZE ; i++) {
                          for(int j = 0 ; j < N ; j++) {
                              set [resultBlockStart + i] [j] = buffer [1 + N * i + j] ;
                          }
                      }
                      numBlocksReceived++ ;
                      display.repaint() ; 
                  }
                  else {  // tag is TAG_HELLO
                      numHellos++ ;
                  }
                  
                  // Send next block of work or finish tag to same worker                  
                  if(nextBlockStart < N) {
                      buffer [0] = nextBlockStart ;
                      MPI.COMM_WORLD.Send(buffer, 0, 1, MPI.INT, status.source, TAG_TASK) ;
                      nextBlockStart += BLOCK_SIZE ;
                      System.out.println("Sending work to " + status.source) ;
                  }
                  else {
                      MPI.COMM_WORLD.Send(buffer, 0, 0, MPI.INT, status.source, TAG_GOODBYE) ;  
                      System.out.println("Shutting down " + status.source) ;
                  }                  
              }
              
              long endTime = System.currentTimeMillis();

              System.out.println("Calculation completed in " +
                                 (endTime - startTime) + " milliseconds");                    
          }
          else {  // worker process
          
              // Send request to master for a first block of work

              MPI.COMM_WORLD.Send(buffer, 0, 0, MPI.INT, 0, TAG_HELLO) ;
              
              boolean done = false ;
              while(!done) {
                  Status status = MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, 0, MPI.ANY_TAG) ;
                  
                  if(status.tag == TAG_TASK) {
                      int blockStart = buffer [0] ;

                      for(int i = 0 ; i < BLOCK_SIZE ; i++) {
                          for(int j = 0 ; j < N ; j++) {

                               double cr = (4.0 * (blockStart + i) - 2 * N) / N ;
                               double ci = (4.0 * j - 2 * N) / N ;

                               double zr = cr, zi = ci ;

                               int k = 0 ;
                               while (k < CUTOFF && zr * zr + zi * zi < 4.0) {

                                   // z = c + z * z

                                   double newr = cr + zr * zr - zi * zi ;
                                   double newi = ci + 2 * zr * zi ;

                                   zr = newr ;
                                   zi = newi ;

                                   k++ ;
                              }

                              buffer [1 + N * i + j] = k ;
                          }
                      }
                      buffer [0] = blockStart ;
                      MPI.COMM_WORLD.Send(buffer, 0, BUFFER_SIZE, MPI.INT, 0, TAG_RESULT) ;
                  }
                  else {  // tag is TAG_GOODBYE
                      done = true ;
                  }
              }       
          }

          MPI.Finalize() ;
      }

      static class Display extends JPanel {

          Display() {

              setPreferredSize(new Dimension(N, N)) ;

              JFrame frame = new JFrame("Mandelbrot");
              frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              frame.setContentPane(this);
              frame.pack();
              frame.setVisible(true);
          }

          public void paintComponent(Graphics g) {
              for(int i = 0 ; i < N ; i++) {
                  for(int j = 0 ; j < N ; j++) {
                      int k = set [i] [j] ;
                      Color c ;

                      if(k == -1) {  // uninitialized
                          c = Color.WHITE ;
                      }
                      else {
                          float level ;
                          if(k < CUTOFF) {
                              level = k < 50 ? (float) k / 50 : 1.0F ;
                          }
                          else {
                              level = 0 ;
                          }
                          c = new Color(level/2, level/2, level) ;  // Blueish
                      }

                      g.setColor(c) ;
                      g.fillRect(i, j, 1, 1) ;
                  }
              }
          }
      }
  }