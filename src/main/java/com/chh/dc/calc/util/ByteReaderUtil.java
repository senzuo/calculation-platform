package com.chh.dc.calc.util;

/**
 * Created by Niow on 2016/7/27.
 */
public class ByteReaderUtil {

    public static String readHexString(byte b) {
        String s = Integer.toHexString(b & 0xff);
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    public static String readHexString(byte[] bs, int start, int length) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String sb = Integer.toHexString(bs[start + i] & 0xff);
            if (sb.length() == 1) {
                sb = "0" + sb;
            }
            s.append(sb);
        }
        return s.toString();
    }

    /**
     * 读取无符号整数
     *
     * @param b
     * @return
     */
    public static int readUInt(byte b) {
        return b & 0xff;
    }

    /**
     * 读取有符号整数
     *
     * @param b
     * @return
     */
    public static int readInt(byte b) {
        if ((b & 0x80) > 0) {
            //取反码-->加1补码-->加上符号
            return (((~b) & 0xff) + 1) * -1;
        }
        return b & 0xff;
    }

//    public static void main(String[] args) {
//        byte[] bs = new byte[]{-2, -124};
//        byte a = bs[0];
//        System.out.println("Hex:" + Integer.toHexString(a));
//        System.out.println("Sint:" + readInt(a));
//        System.out.println("Uint:" + readUInt(a));
//        int rs = readS16(bs, 0, false);
//        System.out.println(rs);
//        System.out.println(readU16(bs, 0));
//        for (int i = 0; i < 16; i++) {
//            System.out.print((rs & (0x8000 >> i)) == (0x8000 >> i) ? 1 : 0);
//        }
//        System.out.println();
//        for (int i = 0; i < 8; i++) {
//            System.out.print(readBit(bs[0], i));
//        }
//        System.out.println("xxxx");
//        for (int i = 0; i < 8; i++) {
//            System.out.print(readBit(bs[1], i));
//        }
//    }


    /**
     * 读取指定位
     *
     * @param b
     * @param index byte的指定位置，从高位到地位为0~7
     * @return
     */
    public static int readBit(byte b, int index) {
        int i = readUInt(b);
        return (i & (0x80 >> index)) == (0x80 >> index) ? 1 : 0;
    }


    public static int readU8(byte[] bs, int start) {
        int index = start;
        int rs = readUInt(bs[index]);
        return rs;
    }

    public static int readU16(byte[] bs, int start, boolean isReverse) {
        int index = start;
        String b1 = readHexString(bs[index]);
        String b2 = readHexString(bs[++index]);
        if (isReverse) {
            return Integer.parseInt(b2 + b1, 16);
        } else {
            return Integer.parseInt(b1 + b2, 16);
        }
    }

    public static int readS16(byte[] bs, int start, boolean isReverse) {
        int i = readU16(bs, start);
        if ((i & 0x8000) > 0) {
            return (((~i) & 0xffff) + 1) * -1;
        }
        return i & 0xffff;
    }

    public static int readU16(byte[] bs, int start) {
        return readU16(bs, start, false);
    }

    public static long readU32(byte[] bs, int start) {
        return readU32(bs, start, false);
    }

    public static long readU32(byte[] bs, int start, boolean isReverse) {
        int index = start;
        String b1 = readHexString(bs[index]);
        String b2 = readHexString(bs[++index]);
        String b3 = readHexString(bs[++index]);
        String b4 = readHexString(bs[++index]);
        if (isReverse) {
            return Long.parseLong(b4 + b3 + b2 + b1, 16);
        } else {
            return Long.parseLong(b1 + b2 + b3 + b4, 16);
        }
    }

    /**
     * 读取有符号32位
     *
     * @param bs
     * @param start
     * @param isReverse
     * @return
     */
    public static long readS32(byte[] bs, int start, boolean isReverse) {
        long rs = readU32(bs, start, isReverse);
        if ((rs & 0x80000000) > 0) {
            return (((~rs) & 0xffffffff) + 1) * -1;
        }
        return rs;
    }



}
