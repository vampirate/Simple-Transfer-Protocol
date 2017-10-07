import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Random;

public class Sender {
	
    public static void main(String[] args) throws Exception {
    	//////////////////////////////Initialize the variables
	    int elementCount = 0;
	    int packetCount = 0;
	    int countMWS = 0;
	    
	    int dataSegmentSent = 0; 
	    int packetsDropped = 0; 
	    int retrasmitted = 0;
	    int bytesReceived = 0; 
	    boolean sendAndListenFlag = true; 
	    boolean sendFlag = true;
	    String log = new String();
	    float rand = 0; 
	    
	    
	    //////////////////////////////Read from arguments
	    InetAddress IPAddress = InetAddress.getByName(args[0]);
		int port = Integer.parseInt(args[1]);	
		String filename = args[2];
		int MWS = Integer.parseInt(args[3]);
		int MSS = Integer.parseInt(args[4]);
		int timeout = Integer.parseInt(args[5]);
		float pdrop = Float.parseFloat(args[6]);
		int seed = Integer.parseInt(args[7]);

		
		/////////////////////////////Generate rand
	    Random random = new Random(seed);
	    
	    
	    /////////////////////////////Read from file
		Scanner scanner = new Scanner(new File(filename));
		String data = scanner.useDelimiter("\\Z").next();
		byte[] textData = data.getBytes();
		DatagramSocket clientSocket = new DatagramSocket(); 
		clientSocket.setSoTimeout(timeout);
	
		
		/////////////////////////////Initialize seqNumber and ackNumbers
		int seqNumberIn = 0;
		int ackNumberIn = 0;
		int seqNumberOut = 0;
		int ackNumberOut = 0;
		long startTime = System.currentTimeMillis();
		//System.out.println("start time is " + startTime);
		long currentTime = 0; 
		
		byte[] headerByte = new byte[1024];
		byte[] sendDataSegment = new byte[MSS]; 
		byte[] receiveData = new byte[1024];

		
		////////////////////////////////////////////////////////////////SENDING SYN
		String header = new String();
		header = "SYN1 " + "ACK0 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + 0 + " | ";
		headerByte = header.getBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(headerByte, headerByte.length, IPAddress, port); 
		clientSocket.send(sendPacket);
		
		currentTime = System.currentTimeMillis();
		//System.out.println("currentTime is " + currentTime);
		log = log + "snd " + (currentTime - startTime) + " S " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
		
		
		////////////////////////////////////////////////////////////////RECEIVING SYNACK
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		clientSocket.receive(receivePacket); 
		currentTime = System.currentTimeMillis();
		receiveData = receivePacket.getData();

		ByteArrayInputStream bais = new ByteArrayInputStream(receiveData);
		InputStreamReader isr = new InputStreamReader(bais);
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine();
		seqNumberIn = Integer.parseInt(line.split(" ")[3]);
		ackNumberIn = Integer.parseInt(line.split(" ")[4]);
		log = log + "rcv " + (currentTime - startTime) + " SA " + seqNumberIn + " 0 " + ackNumberIn + "\r\n";
		
		
		////////////////////////////////////////////////////////////////REPLYING ACK
		ackNumberOut = seqNumberIn + 1;
		seqNumberOut++;
		header = "SYN0 " + "ACK1 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + 0 + " | ";
		headerByte = header.getBytes();
		
		sendPacket = new DatagramPacket(headerByte, headerByte.length, IPAddress, port); 
		clientSocket.send(sendPacket);
		
		currentTime = System.currentTimeMillis();
		log = log + "snd " + (currentTime - startTime) + " A " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
		seqNumberOut++;
		countMWS = 0;
		
		
		/////////////////////////////////////////////////////////////////TRANSMITTING DATA
		receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		clientSocket.receive(receivePacket); 
		while (sendAndListenFlag == true) {//////////////////////////////////////////While there are still data to send and listen
			if (sendFlag == true) {////////////////////////////////////////While there are still data to send
				
				for (; countMWS < MWS; countMWS = countMWS + MSS) {/////////While the bytes sent is smaller than the window
					sendDataSegment = new byte[MSS]; 
					for (packetCount = 0; packetCount < MSS; packetCount++) {
						if (packetCount + elementCount * MSS < textData.length) {
							sendDataSegment[packetCount] = textData[packetCount + elementCount * MSS];
						} else {
							break;
						}
					}
					
					
					elementCount++;
					rand = random.nextFloat();
					if (rand < pdrop) {
						packetsDropped++;
						currentTime = System.currentTimeMillis();
						log = log + "drp " + (currentTime - startTime) + " D " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
						ackNumberOut++;	
						seqNumberOut = seqNumberOut + MSS;
					} else {
						dataSegmentSent++;
						ackNumberOut++;		
						header = "SYN0 " + "ACK0 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + packetCount + " | ";
						headerByte = header.getBytes();
						bytesReceived = bytesReceived + packetCount;
						
						byte[] sendTotal = new byte[headerByte.length + sendDataSegment.length];
						System.arraycopy(headerByte, 0, sendTotal, 0, headerByte.length);
						System.arraycopy(sendDataSegment, 0, sendTotal, headerByte.length, sendDataSegment.length);
						sendPacket = new DatagramPacket(sendTotal, sendTotal.length, IPAddress, port); 
						clientSocket.send(sendPacket); 
						
						currentTime = System.currentTimeMillis();
						log = log + "snd " + (currentTime - startTime) + " D " + seqNumberOut + " MSS " + ackNumberOut + "\r\n";
						seqNumberOut = seqNumberOut + MSS;
					}
					
					
					if ((elementCount - 1) * MSS > textData.length) {//if there are no data to transmite
						sendFlag = false;
						break;
					}
				}	
			}
			
			
			////////////////////////////////////////////////////////////Listening for packets
			try {
				if ((ackNumberIn + MSS >= seqNumberOut) && ((elementCount - 1) * MSS > textData.length)) {
					sendAndListenFlag = false;
					break;
				}

				receivePacket = new DatagramPacket(receiveData, receiveData.length); 
				clientSocket.receive(receivePacket); 
				
				currentTime = System.currentTimeMillis();
				
				
				receiveData = receivePacket.getData();
				bais = new ByteArrayInputStream(receiveData);
			    isr = new InputStreamReader(bais);
			    br = new BufferedReader(isr);
			    line = br.readLine();
			    
			    seqNumberIn = Integer.parseInt(line.split(" ")[3]);
			    ackNumberIn = Integer.parseInt(line.split(" ")[4]);
			    log = log + "rcv " + (currentTime - startTime) + " A " + seqNumberIn + " 0 " + ackNumberIn + "\r\n";
			    
				countMWS = countMWS - MSS;

				
			} catch (SocketTimeoutException e) {///////////////////////If time out, resend the last packet
				System.out.println("timeout");
				retrasmitted++;
				elementCount--;
				ackNumberOut--;
				countMWS = countMWS - MSS;
			}
			
		}
	
		
		/////////////////////////////////////////////////////////////////////////SENDING FIN
		header = "SYN0 " + "ACK0 " + "FIN1 " + seqNumberOut + " " + ackNumberOut + " " + 0 + " | ";
		headerByte = header.getBytes(); 
		
		sendPacket = new DatagramPacket(headerByte, headerByte.length, IPAddress, port); 
		clientSocket.send(sendPacket);
		
		currentTime = System.currentTimeMillis();
		log = log + "snd " + (currentTime - startTime) + " F " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
	
		
		////////////////////////////////////////////////////////////////////////RECEIVING FINACK
		receivePacket = new DatagramPacket(receiveData, receiveData.length); 
		clientSocket.receive(receivePacket); 
		
		receiveData = receivePacket.getData();
		bais = new ByteArrayInputStream(receiveData);
	    isr = new InputStreamReader(bais);
	    br = new BufferedReader(isr);
	    line = br.readLine();
	    
	    seqNumberIn = Integer.parseInt(line.split(" ")[3]);
	    ackNumberIn = Integer.parseInt(line.split(" ")[4]);

		seqNumberOut = seqNumberOut + 1;
		ackNumberOut = seqNumberIn + 1;
		currentTime = System.currentTimeMillis();
		log = log + "rcv " + (currentTime - startTime) + " FA " + seqNumberIn + " 0 " + ackNumberIn + "\r\n";
		
		
		////////////////////////////////////////////////////////////////////////SENDING ACK
		header = "SYN0 " + "ACK1 " + "FIN0 " + seqNumberOut + " " + ackNumberOut + " " + 0 + " | ";
		headerByte = header.getBytes(); 
		
		sendPacket = new DatagramPacket(headerByte, headerByte.length, IPAddress, port); 
		clientSocket.send(sendPacket);
		
		currentTime = System.currentTimeMillis();
		log = log + "snd " + (currentTime - startTime) + " A " + seqNumberOut + " 0 " + ackNumberOut + "\r\n";
		//System.out.println(log);
		log = log + dataSegmentSent + " packets sent" + "\r\n";
		log = log + packetsDropped + " packets dropped" + "\r\n";
		log = log + retrasmitted + " packets re-transmitted" + "\r\n";
		log = log + bytesReceived + " bytes sent" + "\r\n";
		PrintWriter writer = new PrintWriter("Sender_log.txt", "US-ASCII");
		writer.print(log);
		writer.close();
		clientSocket.close();
		scanner.close();
    }
}