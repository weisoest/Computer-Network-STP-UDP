import java.io.*;

public class LogWriter {
	private String event;
	private String time;
	private String typeOfPacket;
	private int seqNum;
	private int packetSize;
	private int ackNum;
	
	public LogWriter(String event, String time, String typeOfPacket, int seqNum, int packetSize, int ackNum) {
		super();
		this.event = event;
		this.time = time;
		this.typeOfPacket = typeOfPacket;
		this.seqNum = seqNum;
		this.packetSize = packetSize;
		this.ackNum = ackNum;
	}
	public LogWriter() {
		super();
		// TODO Auto-generated constructor stub
	}
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getTypeOfPacket() {
		return typeOfPacket;
	}
	public void setTypeOfPacket(String typeOfPacket) {
		this.typeOfPacket = typeOfPacket;
	}
	public int getSeqNum() {
		return seqNum;
	}
	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}
	public int getPacketSize() {
		return packetSize;
	}
	public void setPacketSize(int packetSize) {
		this.packetSize = packetSize;
	}
	public int getAckNum() {
		return ackNum;
	}
	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}
	
	public void setAllLog(STPHeader header, String time, String event) {
		this.event = event;
		this.time = time;
		this.typeOfPacket = header.getFlag();
		this.seqNum = header.getSeqNum();
		this.packetSize = header.getPayloadSize();
		this.ackNum = header.getAckNum();
	}
	
	public void setSnderRcv_RcverSndLog(String event, String time, int seqNum, STPHeader header) {
		this.event = event;
		this.time = time;
		this.typeOfPacket = header.getFlag();
		this.seqNum = seqNum;
		this.packetSize = header.getPayloadSize();
		this.ackNum = header.getAckNum();
	}

	public void writeLog(BufferedWriter bw) {
		try {
			bw.write(String.format("%1$-10s%2$10s%3$10s%4$10d%5$10d%6$10d\n", event, time, typeOfPacket, seqNum, packetSize, ackNum));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
