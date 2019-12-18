import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
public class Sender {
	// STPHeader header;
	// LogWriter log_writer;
	static long startTime;
	// Segments transmitted (including drop & RXT)
	private static int numSnd;
	// Number of Segments handled by PLD
	private static int numByPLD;
	// Number of Segments dropped
	private static int numDrop;
	// Number of Segments Corrupted
	private static int numCorrp;
	// Number of Segments Re-ordered
	private static int numReOrder;
	// Number of Segments Duplicated
	private static int numDup;
	// Number of Segments Delayed
	private static int numDelay;
	// Number of Retransmissions due to TIMEOUT
	private static int numTimeOut;
	// Number of FAST RETRANSMISSION
	private static int numFastTransit;
	// Number of DUP ACKS received
	private static int numDupAck;

	public static void main(String[] args) throws Exception {
		if (args.length != 14) {
	         System.out.println("The number of required arguments is incorrect");
	         return;
	      }
		// Get the server host address
	      InetAddress address = null;
		try {
			address = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      String recv_ip = address.getHostAddress();
	   // Get the server port number
	      int recv_port = Integer.parseInt(args[1]);
	   // Get the pdf file name
	      String fileName = args[2];
	   // Get the max window size   
	      int mws = Integer.parseInt(args[3]);
	   // Get the max segment size
	      int mss = Integer.parseInt(args[4]);
	   // Get gamma value
	      int gamma = Integer.parseInt(args[5]);
	      float pDrop = Float.parseFloat(args[6]);
	      float pDuplicate = Float.parseFloat(args[7]);
	      float pCorrupt = Float.parseFloat(args[8]);
	      float pOrder = Float.parseFloat(args[9]);
	      int maxOrder = Integer.parseInt(args[10]);
	      float pDelay = Float.parseFloat(args[11]);
	      int maxDelay = Integer.parseInt(args[12]);
	      long seed = Long.parseLong(args[13]);
	      PLD pld = new PLD(pDrop, pDuplicate, pCorrupt, pOrder, pDelay, seed);
	      
	      // fileName = "D:\\eclipse-workspace\\STPTransmission\\src\\test0.pdf";
	      File file = new File(fileName);
	      if (!file.exists() || file.isDirectory()) {
	    	  return;
	      }
	      
	      long fileLength = file.length();
	      int win_size = mws / mss;
	      int num_segments = (int) Math.ceil((double) fileLength / mss);
	      System.out.println("num_segments: " + num_segments);
	      STPHeader header = new STPHeader("S", 0, 0, 0, 0);
	      header.setMaxWinSize(mws);
	      header.setNumOfSeg(num_segments);
	      header.setMss(mss);
	      byte[] data = header.toBytes();
	      
	      DatagramSocket socket = null;
	      socket = new DatagramSocket();
		
	      // long time = System.currentTimeMillis();
	      String crrTime;
	      File logFile = new File("Sender_log.txt");
	  	  OutputStreamWriter osw = null;
	  	  FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(logFile);
			osw = new OutputStreamWriter(fos, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startTime = System.currentTimeMillis();
		// Three-way hand shake
		// 1st hand shake
		  LogWriter log_writer;
		  BufferedWriter bw;
		  DatagramPacket data_out;
	  	  bw = new BufferedWriter(osw);
	  	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000).toString();
	      log_writer = new LogWriter("snd", crrTime, header.getFlag(), header.getSeqNum(), header.getPayloadSize(), header.getAckNum());
	      log_writer.writeLog(bw);
	      data_out = new DatagramPacket(data, data.length, address, recv_port);
	      socket.send(data_out);
	      numSnd++;
		
	    // 2nd hand shake  
	      DatagramPacket data_in = new DatagramPacket(new byte[STPHeader.HEADER_LEN], STPHeader.HEADER_LEN); // 1024
	      try {
			socket.receive(data_in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      byte[] recv_data = data_in.getData();
	      header = new STPHeader(recv_data);
	      if (!header.getFlag().equals("X")) {
	    	  System.out.println("HandShake erro");
	    	  return;
	      }
	      crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000).toString();
	      log_writer.setAllLog(header, crrTime, "rcv");
	      log_writer.setTypeOfPacket("SA");
	      log_writer.writeLog(bw);
	    // 3rd hand shake
	      header = new STPHeader("A", 1, 1, 0, 0);
	      data = header.toBytes();
	      data_out = new DatagramPacket(data, data.length, address, recv_port);
	      
	      socket.send(data_out);
	      numSnd++;
		
	      crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000).toString();
	      log_writer.setAllLog(header, crrTime, "snd");
	      log_writer.writeLog(bw);
	      
	    // Send packets data  
	      int startseq = 1;
	      int endseq = startseq + mws;
	      // int currseq = startseq;
	      // long oldest_time;
	      // int requestSeq;
	      
	      List<Integer> seqList = new ArrayList<Integer>();
	      seqList.add(startseq);
	      
	      
	      List<byte[]> temp = new ArrayList<byte[]>();
	      List<Long> timeList = new ArrayList<Long>();
	      byte[] packet = new byte[STPHeader.HEADER_LEN + mss];
	      long estimateRTT = 500;
	      long DEVRTT = 250;
	      long TimeoutInterval = estimateRTT + gamma * DEVRTT;
	      socket.setSoTimeout(0);
	      
	      FileInputStream in = new FileInputStream(fileName);
		
	      STPHeader head;
	      int lastAckNum = 1;
	      int dupNum = 0;
	      
	      int reOrderNum = 0;
	      boolean reOrderFlag = false;
	      byte[] reOderTemp = null;
	      boolean slideFlag = false;
	      
	      while (true) {
	    	  
	    	  while (seqList.get(seqList.size()-1) < endseq) {
	    		  byte[] tempbytes = new byte[mss];
		          int byteread = 0;
		          byteread = in.read(tempbytes);
		          /*if (byteread == -1) {
		        	  break;
		          }	*/			
		          head = new STPHeader("D", seqList.get(seqList.size()-1), 1, byteread, 0);		          
			      			      
		          System.arraycopy(head.toBytes(), 0, packet, 0, head.HEADER_LEN);
		          System.out.println("read size: " + tempbytes.length + " " + byteread);
		          System.arraycopy(tempbytes, 0, packet, head.HEADER_LEN, byteread);
		         
		          
		          Checksum checksumEngine = new Adler32();
		          checksumEngine.update(packet, 0, packet.length);
		          long checksum_verify = checksumEngine.getValue();
		          byte[] temp_head = head.toBytes();
		          head.updateBytesChecksum(temp_head, checksum_verify);
		          System.arraycopy(temp_head, 0, packet, 0, head.HEADER_LEN);
		          
		          temp.add(packet);
		          seqList.add(seqList.get(seqList.size()-1) + byteread);
		          data_out = new DatagramPacket(packet, packet.length, address, recv_port);
		          
		          // PLD module		          
		          int num = pld.roller();
		          System.out.println("PLD: " + num);
		          
		          if (reOrderFlag && numSnd - reOrderNum >= maxOrder) {
		        	  numSnd++;
		        	  numByPLD++;
		        	  numReOrder++;
		        	  data_out = new DatagramPacket(reOderTemp, reOderTemp.length, address, recv_port);
		        	  socket.send(data_out);
		        	  reOrderFlag = false;
		        	  // num = -1;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/rord");
		        	  log_writer.writeLog(bw);
		          }
		          
		          switch (num) {
		          case 0:
		        	  numSnd++;
		        	  numByPLD++;
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 1:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDrop++;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "drop");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 2:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDup++;
		        	  socket.send(data_out);
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/dup");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 3:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numCorrp++;
		        	  packet[6] = (byte) (packet[6] ^ (1 << 6));
		        	  data_out = new DatagramPacket(packet, packet.length, address, recv_port);
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/corr");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 4:
		        	  if (reOrderFlag) {
		        		  numSnd++;
			        	  numByPLD++;
			        	  socket.send(data_out);
			        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
			        	  log_writer.setAllLog(head, crrTime, "snd");
			        	  log_writer.writeLog(bw);
		        	  }
		        	  else {
		        		  reOrderNum = numSnd;
			        	  reOrderFlag = true;
			        	  reOderTemp = packet;
		        	  }
		        	  break;
		          case 5:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDelay++;
		        	  Random random = new Random();
		        	  int delay = random.nextInt(maxDelay);
		        	  DatagramPacket dataP = data_out;
		        	  DatagramSocket dataS = socket;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/dely");
		        	  log_writer.writeLog(bw);
		        	  Timer timer = new Timer();
		        	  timer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								dataS.send(dataP);
								System.out.println("Delay time: " + delay);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							this.cancel();
						}
					}, delay);
		        	  break;
		          }
		          
		          timeList.add(System.currentTimeMillis());
		          // currseq += byteread;
	    	  }
	    	  if (dupNum >= 3) {
	    		  // Fast Retransmission if 3 duplicate ack
	    		  numFastTransit++;
	    		  int index = seqList.indexOf(lastAckNum);
	    		  System.out.println("FastRetransit index: " + index);
	    		  data_out = new DatagramPacket(temp.get(index), temp.get(index).length, address, recv_port);
	    		  byte[] headByte = Arrays.copyOfRange(temp.get(index), 0, STPHeader.HEADER_LEN);
	    		  head = new STPHeader(headByte);
	    		  
	    		  // PLD module
	    		  int num = pld.roller();		          
		          if (reOrderFlag && numSnd - reOrderNum >= maxOrder) {
		        	  numSnd++;
		        	  numByPLD++;
		        	  numReOrder++;
		        	  data_out = new DatagramPacket(reOderTemp, reOderTemp.length, address, recv_port);
		        	  socket.send(data_out);
		        	  reOrderFlag = false;
		        	  // num = -1;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/rord");
		        	  log_writer.writeLog(bw);
		          }
		          
		          switch (num) {
		          case 0:
		        	  numSnd++;
		        	  numByPLD++;
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/RXT");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 1:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDrop++;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "drop");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 2:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDup++;
		        	  socket.send(data_out);
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/dup");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 3:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numCorrp++;
		        	  byte[] segment = temp.get(index);
		        	  segment[6] = (byte) (segment[6] ^ (1 << 6));
		        	  data_out = new DatagramPacket(segment, segment.length, address, recv_port);
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/corr");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 4:
		        	  if (reOrderFlag) {
		        		  numSnd++;
			        	  numByPLD++;
			        	  socket.send(data_out);
			        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
			        	  log_writer.setAllLog(head, crrTime, "snd/RXT");
			        	  log_writer.writeLog(bw);
		        	  }
		        	  else {
		        		  reOrderNum = numSnd;
			        	  reOrderFlag = true;
			        	  reOderTemp = temp.get(index);
		        	  }
		        	  break;
		          case 5:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDelay++;
		        	  Random random = new Random(seed);
		        	  int delay = random.nextInt(maxDelay);
		        	  DatagramPacket dataP = data_out;
		        	  DatagramSocket dataS = socket;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/dely");
		        	  log_writer.writeLog(bw);
		        	  Timer timer = new Timer();
		        	  timer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								dataS.send(dataP);
								System.out.println("Delay time: " + delay);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							this.cancel();
						}
					}, delay);
		        	  break;
		          }
	    		  timeList.set(index, System.currentTimeMillis());
	    	  }
	    	  else if (System.currentTimeMillis() - timeList.get(0) > TimeoutInterval) {
	    		  System.out.println("Timeout Retransmit: " + seqList.indexOf(lastAckNum));
	    		  // Retransmission due to timeout
	    		  numTimeOut++;
	    		  int index = seqList.indexOf(lastAckNum);
	    		  data_out = new DatagramPacket(temp.get(index), temp.get(index).length, address, recv_port);
	    		  byte[] headByte = Arrays.copyOfRange(temp.get(index), 0, STPHeader.HEADER_LEN);
	    		  head = new STPHeader(headByte);
	    		  
	    		  // PLD module
	    		  int num = pld.roller();		          
		          if (reOrderFlag && numSnd - reOrderNum >= maxOrder) {
		        	  numSnd++;
		        	  numByPLD++;
		        	  numReOrder++;
		        	  data_out = new DatagramPacket(reOderTemp, reOderTemp.length, address, recv_port);
		        	  socket.send(data_out);
		        	  reOrderFlag = false;
		        	  // num = -1;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/rord");
		        	  log_writer.writeLog(bw);
		          }
		          
		          switch (num) {
		          case 0:
		        	  numSnd++;
		        	  numByPLD++;
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/RXT");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 1:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDrop++;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "drop");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 2:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDup++;
		        	  socket.send(data_out);
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/dup");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 3:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numCorrp++;
		        	  byte[] corrPacket = temp.get(index);
		        	  corrPacket[6] = (byte) (corrPacket[6] ^ (1 << 6));
		        	  data_out = new DatagramPacket(corrPacket, corrPacket.length, address, recv_port);
		        	  socket.send(data_out);
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/corr");
		        	  log_writer.writeLog(bw);
		        	  break;
		          case 4:
		        	  if (reOrderFlag) {
		        		  numSnd++;
			        	  numByPLD++;
			        	  socket.send(data_out);
			        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
			        	  log_writer.setAllLog(head, crrTime, "snd/RXT");
			        	  log_writer.writeLog(bw);
		        	  }
		        	  else {
		        		  reOrderNum = numSnd;
			        	  reOrderFlag = true;
			        	  reOderTemp = temp.get(index);
		        	  }
		        	  break;
		          case 5:
		        	  numSnd++;
		        	  numByPLD++;
		        	  numDelay++;
		        	  Random random = new Random(seed);
		        	  int delay = random.nextInt(maxDelay);
		        	  DatagramPacket dataP = data_out;
		        	  DatagramSocket dataS = socket;
		        	  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		        	  log_writer.setAllLog(head, crrTime, "snd/dely");
		        	  log_writer.writeLog(bw);
		        	  Timer timer = new Timer();
		        	  timer.schedule(new TimerTask() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								dataS.send(dataP);
								System.out.println("Delay time: " + delay);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							this.cancel();
						}
					}, delay);
		        	  break;
		          }
	    		  timeList.set(index, System.currentTimeMillis());
	    		  // TimeoutInterval *= 2;
	    	  }
	    	  
	    	  // Receive data from receiver
	    	  socket.setSoTimeout(0);
	    	  DatagramPacket data_rcv = new DatagramPacket(new byte[STPHeader.HEADER_LEN], STPHeader.HEADER_LEN); // 1024
	    	  try {
				socket.receive(data_rcv);
				System.out.println("data size: " + data_rcv.getData().length);
			} catch (SocketTimeoutException e) {
				System.out.println("rcv blocked");
				continue;
			}
	    	  
	    	  /*if (data_rcv.getData().length == 0 || data_rcv.getData()[0] == 0) {
	    		  continue;
	    	  }*/
	    	  
	    	  head = new STPHeader(data_rcv.getData());
	    	  if (!head.getFlag().equals("A")) {
	    		  System.out.println("Error ack msg: " + head.getFlag());
	    		  return;
	    	  }
	    	  int ackNum = head.getAckNum();
	    	  int rcv_seq = head.getSeqNum();
	    	  // int rcv_size = head.getPayloadSize();
	    	  
	    	  // update estimate_RTT
	    	  if (ackNum <= rcv_seq + mss) {
	    		  int index = seqList.indexOf(rcv_seq);
	    		  System.out.println("Time Index: " + index + " rcv_seq: " + rcv_seq);
	    		  if (index != -1) {
	    			  long sampleRTT = System.currentTimeMillis() - timeList.get(index);
		    		  estimateRTT = (long) (0.875 * estimateRTT + 0.125 * sampleRTT);
		    		  DEVRTT = (long) (0.75 * DEVRTT + 0.25 * Math.abs(sampleRTT - estimateRTT));
		    		  TimeoutInterval = estimateRTT + gamma * DEVRTT;
	    		  }
	    		  
	    		  if (TimeoutInterval > 60 * 1000) {
	    			  TimeoutInterval /= 2;
	    		  }
	    		  // System.out.println("RTT update: " + TimeoutInterval + "\t" + sampleRTT);
	    	  }
	    	  
	    	  // Statistic the duplicate ack
	    	  if (ackNum == lastAckNum) {
	    		  dupNum++;
	    		  numDupAck++;
	    		  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
	    		  log_writer.setSnderRcv_RcverSndLog("rcv/DA", crrTime, 1, head);
	    		  log_writer.writeLog(bw);
	    	  }
	    	  else {
	    		  crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
	    		  log_writer.setSnderRcv_RcverSndLog("rcv", crrTime, 1, head);
	    		  log_writer.writeLog(bw);
	    		  dupNum = 0;
	    		  // Slide the window
	    		  slideFlag = true;
	    		  int moveStep = seqList.indexOf(ackNum);
	    		  // int moveStep = (int) Math.ceil((double) (ackNum - startseq) / mss);
	    		  startseq = ackNum;
	    		  endseq = startseq + mws;
	    		  //endseq = endseq + moveStep * mss;
	    		  for (int i=0; i<moveStep; i++) {
	    			  seqList.remove(0);
	    			  timeList.remove(0);
	    			  temp.remove(0);
	    		  }
	    	  }
	    	  // If the file has been received completely
	    	  if (ackNum == fileLength + 1) {
	    		  System.out.println("transfer done.");
	    		  break;
	    	  }
	    	  lastAckNum = ackNum;	    	      	  
	      }
	      try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      
	      // 4-way handwave
	      int waveSeqNum = (int) fileLength+1;
	      STPHeader waveHeader = new STPHeader("F", waveSeqNum, 1, 0, 0);
	      byte[] dataByte = waveHeader.toBytes();
	      data_out = new DatagramPacket(dataByte, dataByte.length, address, recv_port);
	      socket.send(data_out);
	      numSnd++;
	      
	      crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
	      log_writer.setAllLog(waveHeader, crrTime, "snd");
	      log_writer.writeLog(bw);
	      
	      for (int i=0; i<2; i++) {
	    	  socket.setSoTimeout(1000);
		      DatagramPacket data_rcv = new DatagramPacket(new byte[STPHeader.HEADER_LEN], STPHeader.HEADER_LEN);
		      socket.receive(data_rcv);		      
		      waveHeader = new STPHeader(data_rcv.getData());
		      if (i == 0 && waveHeader.getFlag().equals("A")) {
		    	  System.out.println("Wave done");
		      }
		      if (i == 1 && waveHeader.getFlag().equals("F")) {
		    	  System.out.println("Rcv goodbye");
		      }
		      crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		      log_writer.setSnderRcv_RcverSndLog("rcv", crrTime, 1, waveHeader);
		      log_writer.writeLog(bw);
	      }
	      // 4th wave
	      if (waveHeader.getFlag().equals("F")) {
	    	  waveSeqNum++;
		      waveHeader = new STPHeader("A", waveSeqNum, 2, 0, 0);
		      data_out = new DatagramPacket(waveHeader.toBytes(), waveHeader.toBytes().length, address, recv_port);
		      socket.send(data_out);
		      numSnd++;
		      crrTime = String.format("%.2f", (double) (System.currentTimeMillis() - startTime) / 1000);
		      log_writer.setAllLog(waveHeader, crrTime, "snd");
		      log_writer.writeLog(bw);
	      }
	      
	      // Write log and close file stream and close socket
	      bw.write("=============================================================\n");
	      bw.write("Size of the file (in Bytes)                             "+ fileLength + "\n");
	      bw.write("Segments transmitted (including drop & RXT)             "+ numSnd + "\n");
	      bw.write("Number of Segments handled by PLD                       "+ numByPLD + "\n");
	      bw.write("Number of Segments dropped                              "+ numDrop + "\n");
	      bw.write("Number of Segments Corrupted                            "+ numCorrp + "\n");
	      bw.write("Number of Segments Re-ordered                           "+ numReOrder + "\n");
	      bw.write("Number of Segments Duplicated                           "+ numDup + "\n");
	      bw.write("Number of Segments Delayed                              "+ numDelay + "\n");
	      bw.write("Number of Segments Delayed                              "+ numDelay + "\n");
	      bw.write("Number of Retransmissions due to TIMEOUT                "+ numTimeOut + "\n");
	      bw.write("Number of FAST RETRANSMISSION                           "+ numFastTransit + "\n");
	      bw.write("Number of DUP ACKS received                             "+ numDupAck + "\n");
	      bw.write("=============================================================\n");
	      
	      bw.flush();
	      osw.flush();
	      fos.flush();
	      
	      bw.close();
	      osw.close();
	      fos.close();
	      
	      socket.close();
	}
}