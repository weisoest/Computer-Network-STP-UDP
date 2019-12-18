import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.Checksum;


public class Receiver {
	
	// Amount of data received (bytes)
	static long numRcv_data;
	// Total Segments Received
	static int numTotalSeg;
	// Data segments received
	static int numDataSeg;
	// Data segments with Bit Errors
	static int numCorrSeg;
	// Duplicate data segments received
	static int numDupSeg;
	// Duplicate ACKs sent
	static int numDupAck;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
	         System.out.println("The number of required arguments is incorrect");
	         return;
	      }
		int port = Integer.parseInt(args[0]);
		String fileName = args[1];
		DatagramSocket socket = null;
		byte[] head_bytes;
		byte[] payload;
		STPHeader header;
		LogWriter logwriter = new LogWriter();
		File logFile = new File("Receiver_log.txt");
		FileOutputStream fos= new FileOutputStream(logFile);
	  	OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
	  	BufferedWriter bw = new BufferedWriter(osw);
	  	
	  	File copyFile;
	  	FileOutputStream copyFileOut = null ;
	  	
	  	int rcvBufSize = 1024;
	  	String crrTime;
		long checksum;
		int seqNum = 1;
		int lastSeqNum = 0;
		int lastAckNum = 0;
		int startPos = 0;
		int payloadSize;
		byte[] buffer = null;
		int[] seqList = null;
		int mss = 0;
		int reply_ack = 1;
		int writeBufferSize = 2048;
		int winSize = 0;
		int writeStart = 1;
		int maxSeqNum = 1;
		byte[] data_out = null;
		STPHeader sndHeader = null;
		
		int writeNum = 0;
		// boolean corrFlag;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long startTime = 0;
		
		// socket.setSoTimeout(10);
		
		while (true) {
	         // Create a datagram packet to hold incomming UDP packet.
	         DatagramPacket data_in = new DatagramPacket(new byte[rcvBufSize], rcvBufSize);

			// Block until the host receives a UDP packet.
	        // socket.setSoTimeout(10);
	         try {
				socket.receive(data_in);
			} catch (SocketTimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("rcv blocked");
				System.out.println("packet drop");
				// Thread.sleep(1000);
				continue;
			}
	         
	         // Get recieved data.
	         byte[] buf = data_in.getData();
	         numTotalSeg++;        	 
	         
	         head_bytes = Arrays.copyOfRange(buf, 0, STPHeader.HEADER_LEN);
	         
	         header = new STPHeader(head_bytes);
	         seqNum = header.getSeqNum();
	         
	         payloadSize = header.getPayloadSize();
	         checksum = header.getChecksum();
	         header.updateBytesChecksum(head_bytes, 0);
	         System.arraycopy(head_bytes, 0, buf, 0, head_bytes.length);
	         
	         Checksum checksumEngine = new Adler32();
	         checksumEngine.update(buf, 0, buf.length);
	         long checksum_verify = checksumEngine.getValue();
	         
	         if (header.getFlag().equals("S")) {
	        	 startTime = header.getChecksum();
	         }
	         	         
	         if (!header.getFlag().equals("D")) {
	        	 System.out.println("Rcv time: " + startTime + "      " + System.currentTimeMillis());
	        	 crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
	        	 logwriter.setAllLog(header, crrTime, "rcv");
	        	 logwriter.writeLog(bw);
	         }
	         
	         // If farewell from Sender
	         if (header.getFlag().equals("A") && header.getAckNum() == 2 && header.getSeqNum() == reply_ack) {
	        	 System.out.println("close receiver");
	        	 break;
	         }
	         
	         // If 3th handshake received
	         if (header.getFlag().equals("A") && header.getAckNum() == 1 && header.getSeqNum() == 1) {
	        	 System.out.println("handshake success");
	        	 continue;
	         }
	         
	         // Assemble the segments
	         // Initialization when handshake
	         if (header.getFlag().equals("S")) {   	 
	        	 copyFile = new File(fileName);
	        	 copyFileOut = new FileOutputStream(copyFile);
	        	 System.out.println("Snd SA");	        	 
	        	 seqList = new int[header.getNumOfSeg()];
	        	 winSize = header.getMaxWinSize();
	        	 mss = header.getMss();
	        	 writeBufferSize = 6 * winSize;
	        	 buffer = new byte[writeBufferSize];
	        	 rcvBufSize = header.HEADER_LEN + mss;
	        	 sndHeader = new STPHeader("X", 0, seqNum+1, 0, 0);
	        	 data_out = sndHeader.toBytes();
	         }  
	         if (header.getFlag().equals("D")) {
	        	 numDataSeg++;
	        	 numRcv_data += header.getPayloadSize();
	        	 System.out.println("numOfDataSeg: " + numDataSeg + "RcvData: " + numRcv_data);
	        	 if (seqNum == lastSeqNum) {
	        		 System.out.println("done?");
	        		 numDupSeg++;
	        		 // socket.send(lastReply);
	        		 continue;
	        	 }
	        	 if (checksum != checksum_verify) {
	        		 numCorrSeg++;
	        		 crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	 logwriter.setAllLog(header, crrTime, "rcv/corr");
		        	 logwriter.writeLog(bw);
		        	 continue;
		         }
		         else {
		        	 crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	 logwriter.setAllLog(header, crrTime, "rcv");
		        	 logwriter.writeLog(bw);
		         }
	        	 if (seqNum > maxSeqNum) {
	        		 maxSeqNum = seqNum;
	        	 }
	        	 payload = Arrays.copyOfRange(buf, STPHeader.HEADER_LEN, buf.length);
		         System.out.println("payload size: " + payload.length + " " + payloadSize + "packet size: " + buf.length);
		         int index = (seqNum - 1) / mss;
		         seqList[index] = seqNum + payloadSize;
		         System.out.println("Seq  " + seqNum + "Start  " + writeStart);
		         if (maxSeqNum - writeStart + 12 * winSize >= buffer.length) {
		        	 // buffer = new byte[2*writeBufferSize];      	 
		        	 buffer = Arrays.copyOf(buffer, buffer.length*6);
		         }
		         if (seqNum >= writeStart) {
		        	 System.arraycopy(payload, 0, buffer, seqNum - writeStart, payload.length); // writeStart + (index - startPos) * mss 
		         }
		         else {
		        	 System.out.println("Write position error: " + seqNum + "Start: " + writeStart);
		         }
		         while (startPos < seqList.length) {
		        	 if (seqList[startPos] == 0) {
		        		 System.out.println("StartPos:" + startPos);
		        		 break;
		        	 }
		        	 startPos++;
		         }
		         if (startPos == 0) {
		        	 reply_ack = 1;
		         }
		         else {
		        	 reply_ack = seqList[startPos - 1];
		        	 System.out.println("reply_ack  " + reply_ack);
		         }
		         if (reply_ack > maxSeqNum && reply_ack - writeStart + 6 * winSize + mss > writeBufferSize) { // 
		        	 copyFileOut.write(buffer, 0, reply_ack - writeStart);
		        	 writeStart = reply_ack;
		        	 writeNum++;
		         }
		         sndHeader = new STPHeader("A", seqNum, reply_ack, 0, 0);
		         data_out = sndHeader.toBytes();
	         }
	         if (header.getFlag().equals("F")) {
	        	 STPHeader wave_header = new STPHeader("A", 1, reply_ack++, 0, 0);
//	        	 logwriter.setAllLog(header, crrTime, "rcv");
//	        	 logwriter.writeLog(bw);
	        	 
	        	 data_out = wave_header.toBytes();
	        	 InetAddress clientHost = data_in.getAddress();
		         int clientPort = data_in.getPort();
		         DatagramPacket reply = new DatagramPacket(data_out, data_out.length, clientHost, clientPort);
		         socket.send(reply);
		         crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		         logwriter.setSnderRcv_RcverSndLog("snd", crrTime, 1, wave_header);
		         logwriter.writeLog(bw);
		         wave_header.setFlag("F");
		         data_out = wave_header.toBytes();
	         }	        
	         // Send reply.
	         InetAddress clientHost = data_in.getAddress();
	         int clientPort = data_in.getPort();
	         
	         DatagramPacket reply = new DatagramPacket(data_out, data_out.length, clientHost, clientPort);
	         // lastReply = reply;
	         socket.send(reply);
	         if (reply_ack == lastAckNum && header.getFlag().equals("D")) {
	        	 numDupAck++;
	        	 crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		         logwriter.setSnderRcv_RcverSndLog("snd/DA", crrTime, 1, sndHeader);
		         logwriter.writeLog(bw);
	         }
	         else {
	        	 crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		         logwriter.setSnderRcv_RcverSndLog("snd", crrTime, 1, sndHeader);
		         if (sndHeader.getFlag().equals("X")) {
		        	 logwriter.setTypeOfPacket("SA");
		         }
		         logwriter.writeLog(bw);
	         }
	         
	         lastSeqNum = seqNum;
	         lastAckNum = reply_ack;
	      }
		System.out.println("HHHH: " + Sender.numSnd);
		// Write log and close file stream and close socket
		
		bw.write("==============================================\n");
		bw.write("Amount of data received (bytes)                         " + numRcv_data + "\n");
		bw.write("Total Segments Received                                 " + numTotalSeg + "\n");
		bw.write("Data segments received                                  " + numDataSeg + "\n");
		bw.write("Data segments with Bit Errors                           " + numCorrSeg + "\n");
		bw.write("Duplicate data segments received                        " + numDupSeg + "\n");
		bw.write("Duplicate ACKs sent                                     " + numDupAck + "\n");
		bw.write("==============================================\n");
		
		System.out.println("Write Times" + writeNum);
		
		copyFileOut.close();
		bw.flush();
		osw.flush();
		fos.flush();
      
		bw.close();
		osw.close();
	    fos.close();
	    
	    socket.close();
	   }
}