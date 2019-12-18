import java.util.Arrays;


public class STPHeader {
	static final int HEADER_LEN = 33;
	private String flag;
	private int seqNum;
	private int ackNum;
	private int payloadSize;
	private long checksum;
	private int maxWinSize;
	private int numOfSeg;
	private int mss;
	public STPHeader(String flag, int seqNum, int ackNum, int payloadSize, long checksum) {
		super();
		this.flag = flag;
		this.seqNum = seqNum;
		this.ackNum = ackNum;
		this.payloadSize = payloadSize;
		this.checksum = checksum;
	}
	public STPHeader() {
		super();
		// TODO Auto-generated constructor stub
	}
	public STPHeader(byte[] header) {
		super();
		this.flag = ByteConverterUtil.byte1ToString(Arrays.copyOfRange(header, 0, 1));
		this.seqNum = ByteConverterUtil.byte4ToInt(Arrays.copyOfRange(header, 1, 5));
		this.ackNum = ByteConverterUtil.byte4ToInt(Arrays.copyOfRange(header, 5, 9));
		this.payloadSize = ByteConverterUtil.byte4ToInt(Arrays.copyOfRange(header, 9, 13));
		this.checksum = ByteConverterUtil.byte8ToLong(Arrays.copyOfRange(header, 13, 21));
		this.maxWinSize = ByteConverterUtil.byte4ToInt(Arrays.copyOfRange(header, 21, 25));
		this.numOfSeg = ByteConverterUtil.byte4ToInt(Arrays.copyOfRange(header, 25, 29));
		this.mss = ByteConverterUtil.byte4ToInt(Arrays.copyOfRange(header, 29, 33));
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public int getSeqNum() {
		return seqNum;
	}
	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}
	public int getAckNum() {
		return ackNum;
	}
	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}
	public int getPayloadSize() {
		return payloadSize;
	}
	public void setPayloadSize(int payloadSize) {
		this.payloadSize = payloadSize;
	}
	public long getChecksum() {
		return checksum;
	}
	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}
	public void updateBytesChecksum(byte[] header, long checksum) {
		System.arraycopy(ByteConverterUtil.longToByte8(checksum), 0, header, 13, 8);
	}
	public int getMaxWinSize() {
		return maxWinSize;
	}
	public void setMaxWinSize(int maxWinSize) {
		this.maxWinSize = maxWinSize;
	}
	public int getNumOfSeg() {
		return numOfSeg;
	}
	public void setNumOfSeg(int numOfSeg) {
		this.numOfSeg = numOfSeg;
	}
	public int getMss() {
		return mss;
	}
	public void setMss(int mss) {
		this.mss = mss;
	}
	public byte[] toBytes() {
		byte[] header = new byte[HEADER_LEN];
		System.arraycopy(ByteConverterUtil.stringToByte1(this.flag), 0, header, 0, 1);
		System.arraycopy(ByteConverterUtil.intToByte4(this.seqNum), 0, header, 1, 4);
		System.arraycopy(ByteConverterUtil.intToByte4(this.ackNum), 0, header, 5, 4);
		System.arraycopy(ByteConverterUtil.intToByte4(this.payloadSize), 0, header, 9, 4);
		System.arraycopy(ByteConverterUtil.longToByte8(this.checksum), 0, header, 13, 8);
		System.arraycopy(ByteConverterUtil.intToByte4(this.maxWinSize), 0, header, 21, 4);
		System.arraycopy(ByteConverterUtil.intToByte4(this.numOfSeg), 0, header, 25, 4);
		System.arraycopy(ByteConverterUtil.intToByte4(this.mss), 0, header, 29, 4);
		return header;
	}
}