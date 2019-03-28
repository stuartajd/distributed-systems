/*
 The MIT License

 Copyright (c) 2005 - 2008
   1. Distributed Systems Group, University of Portsmouth (2005)
   2. Aamir Shafi (2005 - 2008)
   3. Bryan Carpenter (2005 - 2008)
   4. Mark Baker (2005 - 2008)

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * File         : MPJDaemon.java 
 * Author       : Aamir Shafi, Bryan Carpenter, Deniz Unal
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.28 $
 * Updated      : $Date: 2006/10/20 17:24:47 $
 */

package runtime.daemon;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;

import org.apache.log4j.Logger ;
import org.apache.log4j.PropertyConfigurator ;
import org.apache.log4j.PatternLayout ;
import org.apache.log4j.FileAppender ;
import org.apache.log4j.Level ;
import org.apache.log4j.DailyRollingFileAppender ;
import org.apache.log4j.spi.LoggerRepository ;

import java.util.concurrent.Semaphore ; 

import runtime.MPJRuntimeException ;  
import runtime.OutputProtocol ;

public class MPJDaemon {

  private Socket peerSocket;
  private ServerSocket serverSocket;
  static Logger logger = null ; 
  private int D_SER_PORT = 10000;
  public int mpjPortBase = 20000;
  static final boolean DEBUG = true ;
  private String hostName = null;
  String mpjHomeDir = null ;    
    
  
  public MPJDaemon(String args[]) throws Exception {
    
    try {
      InetAddress localaddr = InetAddress.getLocalHost();
      hostName = localaddr.getHostName();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    Map<String,String> map = System.getenv() ;
    mpjHomeDir = map.get("MPJ_HOME");
          
    createLogger(mpjHomeDir); 

    if(DEBUG && logger.isDebugEnabled()) { 
      logger.debug("mpjHomeDir "+mpjHomeDir); 
    }
  
    if (args.length == 1) {
      
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug (" args[0] " + args[0]);
        logger.debug ("setting daemon port to" + args[0]);
      }

      D_SER_PORT = new Integer(args[0]).intValue();

    }
    else {
      
      throw new MPJRuntimeException("Usage: java MPJDaemon daemonServerPort");

    }
    serverSocketInit();
    serverAccept();
    
  } 
  
  private void serverAccept() throws Exception {

    HandlerThread handlerThread = null;

    while(true){
      try {
        peerSocket = serverSocket.accept();
        peerSocket.setTcpNoDelay(true);
        handlerThread = new HandlerThread(peerSocket,this);
        handlerThread.start();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void serverSocketInit() {
    
    try {
      serverSocket = new ServerSocket();
      serverSocket.bind(new InetSocketAddress(D_SER_PORT));
    }
    catch (Exception cce) {
      cce.printStackTrace();
      System.exit(0);
    }
  }

  static boolean matchMe(String line) throws Exception {

    if(!line.contains("@") || line.startsWith("#") ) {
      return false;
    }

    StringTokenizer token = new StringTokenizer(line, "@");    
    boolean found = false; 
    String hostName = token.nextToken() ;
    InetAddress host = null ;
    
    try {    
      host = InetAddress.getByName(hostName) ;
    }
    catch(Exception e) {
      return false;         
    }

    Enumeration<NetworkInterface> cards =
            NetworkInterface.getNetworkInterfaces() ;
    
    foundIt: 

    while(cards.hasMoreElements()) {

      NetworkInterface card = cards.nextElement() ;
      Enumeration<InetAddress> addresses = card.getInetAddresses();

      while(addresses.hasMoreElements()) {
        InetAddress address = addresses.nextElement() ;
        if(host.getHostName().equals(address.getHostName()) || 
           host.getHostAddress().equals(address.getHostAddress())) {
          found = true;
          break foundIt;
        }
      }
    }

    return found; 
  }
  
  
  private void createLogger(String homeDir) throws MPJRuntimeException {
  
    if(logger == null) {

      DailyRollingFileAppender fileAppender = null ;

      try {
        fileAppender = new DailyRollingFileAppender(
                            new PatternLayout(
                            " %-5p %c %x - %m\n" ),
                            homeDir+"/logs/daemon-"+hostName+".log",
                            "yyyy-MM-dd-a" );

        Logger rootLogger = Logger.getRootLogger() ;
        rootLogger.addAppender( fileAppender);
        LoggerRepository rep =  rootLogger.getLoggerRepository() ;
        rootLogger.setLevel ((Level) Level.ALL );
        logger = Logger.getLogger( "mpjdaemon" );
      }
      catch(Exception e) {
        throw new MPJRuntimeException(e) ;
      }
    }
  }
  

  public static void main(String args[]) {
    try {
      MPJDaemon dae = new MPJDaemon(args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}


class HandlerThread extends Thread {

  private String wdir = null ;
  private String URL = null; //http:server:portclient.jar
  private String mpjURL = null; 
  private String deviceName = null;
  private String className = null ;
  private ArrayList<String> jvmArgs = new ArrayList<String>();
  private ArrayList<String> appArgs = new ArrayList<String>();
  private String loader = null;  
  private MyLogger logger = null ; 
  private int processes = 0;  
  private Socket peerSocket;
  private static final boolean DEBUG = MPJDaemon.DEBUG ;  
  private String mpjHome = null ;
  private int D_SER_PORT = 10000;
  private String mpjHomeDir = null ;
  private Process p[] = null ;
  private MPJDaemon mpjDaemon;
  private int status = 0 ;
  
  public HandlerThread(Socket peerSocket,MPJDaemon mpjDaemon){
  
    this.peerSocket = peerSocket;
    this.logger = new MyLogger(mpjDaemon.logger);
    this.mpjHomeDir = mpjDaemon.mpjHomeDir;
    this.mpjDaemon=mpjDaemon;
  }

  private void sendPorts(int numProcs) throws Exception {
      
    DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());    
    synchronized(mpjDaemon){
      dos.writeInt(mpjDaemon.mpjPortBase);
      mpjDaemon.mpjPortBase += 2*numProcs;
    }
  }

  public void run(){
      
    byte [] stackTrace = null ;
    try {
      DataInputStream inputStream =
              new DataInputStream(peerSocket.getInputStream()) ;
      int intNumProcs = inputStream.readInt();
      sendPorts(intNumProcs);
    
      while(true){
      
        byte [] command = new byte [4] ;
        inputStream.readFully(command);

        String read = new String(command);
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("READ_EVENT (String)<" + read + ">");
        }

        if (read.equals("url-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("url-");
          }
          int length = inputStream.readInt() ;
          if(DEBUG && logger.isDebugEnabled()) { 
              logger.debug ("URL Length -->" + length);
          }
          byte[] byteArray = new byte[length];
          inputStream.readFully(byteArray);
          URL = new String(byteArray);
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("URL:<" + URL + ">");
          }

          if(URL.endsWith(".jar")) {
            className = null ;        
          }
        }

        if (read.equals("mul-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("mul-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("URL Length -->" + length);
          }
          byte[] byteArray = new byte[length];
          inputStream.readFully(byteArray);
          mpjURL = new String(byteArray);
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("mpjURL:<" + mpjURL + ">");
          }
        }

        if (read.equals("cls-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("cls-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("className length -->" + length);
          }
          byte[] byteArray = new byte[length];
          inputStream.readFully(byteArray);
          className = new String(byteArray);
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("className :<" + className + ">");
          }
        }

        if (read.equals("app-")) {

          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("app-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("Application args Length -->" + length);
          }

          for(int j=0 ; j<length ; j++) {       
            int argLen = inputStream.readInt();
            byte[] t = new byte[argLen];
            inputStream.readFully(t);
            appArgs.add(new String(t)); 
          }
        }

        else if (read.equals("num-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("num-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("should be 4, isit ? -->" + length);
          }
          processes = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("Num of processes ==>" + processes);
          }
        }

        else if (read.equals("arg-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("arg-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("argu len -->"+length);
          }

          for(int j=0 ; j<length ; j++) {      
            int argLen = inputStream.readInt();
            byte[] t = new byte[argLen];
            inputStream.readFully(t);
            jvmArgs.add(new String(t)); 
          }
        }

        else if (read.equals("dev-")) {
          if(DEBUG && logger.isDebugEnabled()) {
            logger.debug ("dev-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("dev-Length -->" + length);
          }

          byte[] byteArray = new byte[length];
          inputStream.readFully(byteArray);
          deviceName = new String(byteArray);
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("Device Name :<" + deviceName + ">");
          }
        }

        else if (read.equals("ldr-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("ldr-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("ldr-Length -->" + length);
          }

          byte[] byteArray = new byte[length];
          inputStream.readFully(byteArray);
          loader = new String(byteArray);
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("ldr:<"+loader+">");
          }
        }

        else if (read.equals("wdr-")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("wdr-");
          }
          int length = inputStream.readInt();
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("wdr-Length -->" + length);
          }
          byte[] byteArray = new byte[length];
          inputStream.readFully(byteArray);
          wdir = new String(byteArray);
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("wdir :<"+wdir+">");
          }
        }

        else if (read.equals("*GO*")) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("GO");
          }
          break;
        }
      }

      handleRequest();
    }
    catch(Exception ex) {
      ByteArrayOutputStream arrayStream = new ByteArrayOutputStream() ;
      PrintStream stream = new PrintStream(arrayStream) ;
      ex.printStackTrace(stream);
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug (arrayStream.toString());
      }
      stackTrace = arrayStream.toByteArray() ;
      status = 255 ;  // whatever
    }

    try {

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Stopping the output");
      }

      DataOutputStream os =
              new DataOutputStream(peerSocket.getOutputStream());

      if(stackTrace != null) {
        synchronized (peerSocket) {
          os.writeInt(OutputProtocol.TEXT) ;
          os.writeInt(stackTrace.length) ;
          os.write(stackTrace, 0, stackTrace.length) ;
        } 
      }

      synchronized (peerSocket) {
        os.writeInt(OutputProtocol.EXIT) ;
        os.writeInt(status) ;
      } 

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Checking whether peerChannel is closed or what ?" +
                      peerSocket);
      }

      if (!peerSocket.isClosed()) {
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("Closing it ..."+peerSocket );
        }
        peerSocket.close();
      } 

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("Was already closed, or i closed it");
      }

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("\n\n ** .. execution ends .. ** \n\n");
      }
    }
    catch(Exception ex) {
      if(DEBUG && logger.isDebugEnabled()) { 
        ByteArrayOutputStream arrayStream = new ByteArrayOutputStream() ;
        PrintStream stream = new PrintStream(arrayStream) ;
        ex.printStackTrace(stream);
        logger.debug (arrayStream.toString());
      }
    }
  }

  private void handleRequest() throws Exception {

    BufferedReader bufferedReader = null;
    InputStream in = null;
    URL aURL = null;
    String conf = URL.substring(0, (URL.lastIndexOf("/") + 1));
    String config = conf + "mpjdev.conf";
    String rank_ = null;

    aURL = new URL(new String(config));
    in = aURL.openStream();

    bufferedReader = new BufferedReader(new InputStreamReader(in));
    OutputHandler [] outputThreads = new OutputHandler[processes] ;
    p = new Process[processes] ;    
    int iter = 0;

    for (int j = 0; j < processes; j++) {
      String line = null;
      boolean loop = true;

      while (loop) {
        line = bufferedReader.readLine();

        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("line read <" + line + ">");
        }

        if ( (line != null) &&
            (MPJDaemon.matchMe(line))) {  

          StringTokenizer tokenizer = new StringTokenizer(line, "@");
          tokenizer.nextToken();
          tokenizer.nextToken();
          rank_ = tokenizer.nextToken();

          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("rank_ " + rank_);
          }
        
          loop = false;

        }
        else {
          iter++;
          if (iter > (processes + 100)) {
            if(DEBUG && logger.isDebugEnabled()) { 
              logger.debug (" read all entries from config file");
            }
            loop = false;
          }
          else {
            continue;
          }
        } //end else


      } //end while

      String[] jArgs = jvmArgs.toArray(new String[0]); 
      boolean now = false;
      boolean noSwitch = true ;

      for(int e=0 ; e<jArgs.length; e++) {

        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("jArgs["+e+"]="+jArgs[e]);
        }

        if(now) {
        
          String cp = jvmArgs.remove(e);
        
          if(loader.equals("useLocalLoader")) {
            cp = "."+File.pathSeparator+""+
                   mpjHomeDir+"/lib/loader1.jar"+
                   File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
                   File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                   File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"+
                   File.pathSeparator+cp;
          }
          else if(loader.equals("useRemoteLoader")) {
            cp = //"."+File.pathSeparator+""+
                  mpjHomeDir+"/lib/loader1.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"+
                  File.pathSeparator+cp;
          }
        
          jvmArgs.add(e,cp);
          now = false;
        }

        if(jArgs[e].equals("-cp")) {
          now = true;
          noSwitch = false;
        }

      }

      if(noSwitch) {
        jvmArgs.add("-cp");

        if(loader.equals("useLocalLoader")) {
          jvmArgs.add("."+File.pathSeparator+""
                  +mpjHomeDir+"/lib/loader1.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar" );
        }
        else if(loader.equals("useRemoteLoader")) {
          jvmArgs.add(//"."+File.pathSeparator+""+
                  mpjHomeDir+"/lib/loader1.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar" );
        }
      }

      jArgs = jvmArgs.toArray(new String[0]);

      for(int e=0 ; e<jArgs.length; e++) {
      
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("modified: jArgs["+e+"]="+jArgs[e]);
        }

      }

      String[] aArgs = appArgs.toArray(new String[0]); 

      String[] ex = new String[ (9+jArgs.length+aArgs.length) ]; 
      ex[0] = "java";

      //System.arraycopy ... can be used ..here ...
      for(int i=0 ; i<jArgs.length ; i++) { 
        ex[i+1] = jArgs[i];   
      }

      int indx = jArgs.length+1; 

      ex[indx] = "runtime.daemon.Wrapper" ; indx++ ;
      ex[indx] = URL; indx++ ; 
      ex[indx] = Integer.toString(processes); indx++ ; 
      ex[indx] = deviceName; indx++ ; 
      ex[indx] = rank_ ; indx++ ;
      ex[indx] = loader ; indx++ ;
      ex[indx] = mpjURL; indx++ ;

      if(className != null) {
        ex[indx] = className ; 
      }
      else {
        ex[indx] = "dummy" ; //this is JAR case ..this arg will never 
                             //be used ...
      }

      //System.arraycopy ... can be used ..here ...
      for(int i=0 ; i< aArgs.length ; i++) { 
        ex[i+9+jArgs.length] = aArgs[i];   
      }

      if(DEBUG && logger.isDebugEnabled()) { 
        for (int i = 0; i < ex.length; i++) {
          logger.debug(i+": "+ ex[i]);
        }  
      }

      /*... Making the command finishes here ...*/

      if(DEBUG && logger.isDebugEnabled()) { 
        for (int i = 0; i < ex.length; i++) {
          logger.debug(i+": "+ ex[i]);
        }  
      }

      ProcessBuilder pb = new ProcessBuilder(ex);
      pb.directory(new File(wdir)) ;
      pb.redirectErrorStream(true); 

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("starting the process ");
      }

      try{
        p[j] = pb.start();
      
      }
      catch(Exception exc){

        pb.directory(null);  // Why?? dbc
        p[j] = pb.start();
      }
      

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("started the process "); 
      }
    } //end for loop.

    bufferedReader.close() ; 
    in.close() ; 

    /* Step 4: Start a new thread to handle output from each JVM. */ 

    for (int j = 0; j < processes; j++) {
      outputThreads[j] = new OutputHandler(p[j], peerSocket) ; 
      outputThreads[j].start();
    }

    KillHandler killHandlerThread = new KillHandler(p, peerSocket);
    killHandlerThread.start();

    // Wait for the JVMs to finish, and accumulate maximum exit status.
    
    for (int j = 0; j < processes; j++) {
      int status_j = p [j].waitFor() ;
      if(Math.abs(status_j) > Math.abs(status)) {  // not sure if could be -ve
        status = status_j ;
      }
    }
  }  
}

class MyLogger{

  Logger logger;
  MyLogger(Logger logger) {

    this.logger = logger;

  }

  void debug(Object message){
    synchronized(logger){
      logger.debug(message);
    }
  }
  
  boolean isDebugEnabled(){
      return logger.isDebugEnabled();
  }
}

class OutputHandler extends Thread { 

  Process p = null ;
  Socket peerSocket;  

  public OutputHandler(Process p,Socket peerSocket) { 
    this.p = p;
    this.peerSocket = peerSocket;
  } 

  public void run() {

    DataOutputStream os = null;

    try {
      os = new DataOutputStream(peerSocket.getOutputStream());

      InputStream outp = p.getInputStream() ;  
      BufferedReader reader = new BufferedReader(new InputStreamReader(outp));
       
      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug( "outputting ...");
      }

      String line ;
      while ( (line = reader.readLine()) != null) {
        line += "\n";
        byte [] bytes = line.getBytes() ;
        synchronized (peerSocket) {
          os.writeInt(OutputProtocol.TEXT) ;
          os.writeInt(bytes.length) ;
          os.write(bytes, 0, bytes.length) ;
        } 
        //if(DEBUG && logger.isDebugEnabled()) { 
        //  logger.debug(line);
        //}
      }
    }
    catch (Exception e) {
      ByteArrayOutputStream arrayStream = new ByteArrayOutputStream() ;
      PrintStream stream = new PrintStream(arrayStream) ;
      e.printStackTrace(stream);
      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug (arrayStream.toString());
      }
      try {
        byte [] bytes = arrayStream.toByteArray() ;
        synchronized (peerSocket) {
          os.writeInt(OutputProtocol.TEXT) ;
          os.writeInt(bytes.length) ;
          os.write(bytes, 0, bytes.length) ;
        } 
      }
      catch (Exception e2) {
        // Failed to return message to client, but has been logged already.
      }
    } 
  } //end run.
}

class KillHandler extends Thread{

  Process[] p;
  Socket peerSocket;

  public KillHandler(Process[] p,Socket peerSocket){

    this.peerSocket = peerSocket;
    this.p = p;

  }

  public void run(){
  
    // Can probably be simplified?!  dbc

    InputStream is = null;
    try{
    
      is  = peerSocket.getInputStream();
    
    }
    catch(Exception exc){
    
      for(int i=0; i< p.length;i++)
      {      
        p[i].destroy();      
      }
      return ;
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line =null;
    try{
      line=reader.readLine();
    }catch(Exception e){
      for(int i=0; i< p.length;i++) {      
        p[i].destroy();      
      }
      return ;
    }
    if(line!=null){
      if(line.indexOf("kill")!=-1){
      
        for(int i=0; i< p.length;i++)
        {      
          p[i].destroy();      
        }
      }
    }
  }
}
