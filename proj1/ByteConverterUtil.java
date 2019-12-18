public class ByteConverterUtil {
	/**
     * 
     * Convert 32-bits integer to 4-bytes array.
     * 
     * @param sum
     * @return
     */
    public static byte[] intToByte4(int sum) {
        byte[] arr = new byte[4];
        arr[0] = (byte) ((sum >> 24) & 0xff);
        arr[1] = (byte) ((sum >> 16) & 0xff);
        arr[2] = (byte) ((sum >> 8) & 0xff);
        arr[3] = (byte) (sum & 0xff);
        return arr;
    }

    /**
     *
     * Convert 4-bytes array to 32-bits integer.
     *
     * @param arr
     * @return
     */
    public static int byte4ToInt(byte[] arr) {
        if (arr == null || arr.length != 4) {
            throw new IllegalArgumentException("byte array must not be null and has 4-bytes.");
        }
        return (int) (((arr[0] & 0xff) << 24) | ((arr[1] & 0xff) << 16) | ((arr[2] & 0xff) << 8) | ((arr[3] & 0xff)));
    }
    
    /**
     * 
     * Convert 1-character string to 1-byte array.
     * 
     * @param sum
     * @return
     */
    public static byte[] stringToByte1(String str) {
    	return str.getBytes();
    }
    
    /**
     * 
     * Convert 1-byte array to string.
     * 
     * @param sum
     * @return
     */
    public static String byte1ToString(byte[] arr) {
    	String str = new String(arr);
    	return str;
    }
    
    /**
     * 
     * Convert 8-bytes array to 64-bits long.
     * 
     * 0xff corresponds to Hex, f represents to 1111,0xff is 8-bits byte[]
     * 
     * @param arr
     * @return
     */
    public static long byte8ToLong(byte[] arr) {
        if (arr == null || arr.length != 8) {
            throw new IllegalArgumentException("Byte array must not to be null and has 8-bits.");
        }
        return (long) (((long) (arr[0] & 0xff) << 56) | ((long) (arr[1] & 0xff) << 48) | ((long) (arr[2] & 0xff) << 40)
                        | ((long) (arr[3] & 0xff) << 32) | ((long) (arr[4] & 0xff) << 24)
                        | ((long) (arr[5] & 0xff) << 16) | ((long) (arr[6] & 0xff) << 8) | ((long) (arr[7] & 0xff)));
    }

    /**
     *  Convert 64-bits long to 8-bytes array.
     */
    public static byte[] longToByte8(long sum) {
        byte[] arr = new byte[8];
        arr[0] = (byte) (sum >> 56);
        arr[1] = (byte) (sum >> 48);
        arr[2] = (byte) (sum >> 40);
        arr[3] = (byte) (sum >> 32);
        arr[4] = (byte) (sum >> 24);
        arr[5] = (byte) (sum >> 16);
        arr[6] = (byte) (sum >> 8);
        arr[7] = (byte) (sum & 0xff);
        return arr;
    }
}
