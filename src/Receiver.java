import java.io.*;
import java.net.*;

/*
 * Server to process ping requests over UDP. 
 * The server sits in an infinite loop listening for incoming UDP packets. 
 * When a packet comes in, the server simply sends the encapsulated data back to the client.
 */

public class Receiver {

   public static void main(String[] args) throws Exception {
	  String line = new String();
	  boolean flag = true; 
	  
	  int seqNumberIn = 0; 
	  int ackNumberIn = 0; 
	  int seqNumberOut = 80; 
	  int ackNumberOut = 0; 
	  int dataSegmentReceived = 0;
	  int bytesReceived = 0;
	  int MSS = 0; 
	  
	  String log = new String();
	  
	  long startTime = System.currentTimeMillis();
		//System.out.println("start time is " + startTime);
	  long currentTime = 0; 
		
	  String header = new String();
	  header = "SYN1 " + "ACK0 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + MSS + " | ";

      int port = Integer.parseInt(args[0]);
      String filename = args[1];

      DatagramSocket socket = new DatagramSocket(port);
      DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
      socket.receive(request);
      byte[] requestData = request.getData();
      
      InetAddress clientHost = request.getAddress();
      int clientPort = request.getPort();
      
      ////////////////////////////////////////////////////////////
      requestData = request.getData();      
      ByteArrayInputStream bais = new ByteArrayInputStream(requestData);
      InputStreamReader isr = new InputStreamReader(bais);
      BufferedReader br = new BufferedReader(isr);
      line = br.readLine();

	  currentTime = System.currentTimeMillis();
	  log = log + "snd " + (currentTime - startTime) + " A " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
      if (line.contains("SYN1")) {
          seqNumberIn = Integer.parseInt(line.split(" ")[3]);
          ackNumberIn = Integer.parseInt(line.split(" ")[4]);          
          MSS = Integer.parseInt(line.split(" ")[5]);
          
          ackNumberOut = seqNumberIn + 1;
          
    	  header = "SYN1 " + "ACK1 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + MSS + " | ";
    	  byte[] headerByte = header.getBytes();
    	  DatagramPacket reply = new DatagramPacket(headerByte, headerByte.length, clientHost, clientPort);
    	  socket.send(reply);
    	  currentTime = System.currentTimeMillis();
    	  log = log + "snd " + (currentTime - startTime) + " A " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
      }
      
      ////////////////////////////////////////////////////////////
      PrintWriter writer = new PrintWriter(filename, "US-ASCII");
      while (flag) {
    	  socket.receive(request);
    	  requestData = request.getData();
    	  String data = new String(requestData);
    	  
          seqNumberIn = Integer.parseInt(data.split(" ")[3]);
          ackNumberIn = Integer.parseInt(data.split(" ")[4]);
          MSS = Integer.parseInt(data.split(" ")[5]);
          currentTime = System.currentTimeMillis();
          log = log + "rcv " + (currentTime - startTime) + " D " + seqNumberIn + " MSS " + ackNumberIn + "\r\n";
          dataSegmentReceived++;
	      if (!data.contains("FIN1")) {
	    	  String newline = data.split("\\|")[1];
	    	  //System.out.println("newline received: " + newline);
	    	  newline = newline.substring(1, MSS + 1);
	    	  //System.out.println("newlinesub received: " + newline);

	          bytesReceived = bytesReceived + MSS;
	    	  ackNumberOut = seqNumberIn + MSS;
	    	  //System.out.println("ackNumberOut is " + ackNumberOut);
	    	  seqNumberOut = ackNumberIn + 1;
	    	  header = "SYN0 " + "ACK1 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + MSS + " | ";
	    	  byte[] headerByte = header.getBytes();
	    	  DatagramPacket reply = new DatagramPacket(headerByte, headerByte.length, clientHost, clientPort);
	    	  socket.send(reply);

	          currentTime = System.currentTimeMillis();
	          log = log + "rcv " + (currentTime - startTime) + " A " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
	    	  //System.out.println(header + "replied2");
	    	  writer.print(newline);
		 } else {
			 ackNumberOut = seqNumberIn + 1; 
			 seqNumberOut++;
			 header = "SYN0 " + "ACK1 " + "FIN1 " + seqNumberOut + " " + ackNumberOut + " " + MSS + " | ";
			 byte[] headerByte = header.getBytes();
			 DatagramPacket reply = new DatagramPacket(headerByte, headerByte.length, clientHost, clientPort);
			 socket.send(reply);

	    	  currentTime = System.currentTimeMillis();
	    	  log = log + "snd " + (currentTime - startTime) + " FA " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
			 //System.out.println(header + "replied3");
			 flag = false;
			 log = log + dataSegmentReceived + " packets received" + "\r\n";
			 log = log + bytesReceived + " bytes received" + "\r\n";
			 writer = new PrintWriter("Receiver_log.txt", "US-ASCII");
			 
			 writer.print(log);
			 writer.close();
			 
		     socket.close();
		 }
      }
         
      
      
      
   }
}

