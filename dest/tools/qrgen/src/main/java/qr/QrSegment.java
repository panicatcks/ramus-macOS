package qr;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class QrSegment {
    public final Mode mode;
    public final int numChars;
    public final BitSet data;
    public final int bitLength;

    public enum Mode {
        BYTE(0x4, new int[]{8,16,16});
        final int modeBits;
        final int[] numCharCountBits;
        Mode(int mb, int[] nccb){modeBits=mb; numCharCountBits=nccb;}
        int numCharCountBits(int ver){return (ver<=9)?numCharCountBits[0] : (ver<=26?numCharCountBits[1]:numCharCountBits[2]);}
    }

    private QrSegment(Mode md, int numCh, BitSet bits, int bitLen){mode=md; numChars=numCh; data=bits; bitLength=bitLen;}

    public static QrSegment makeBytes(byte[] data){
        BitSet bs = new BitSet();
        int len=0;
        for(byte b: data){int v=b&0xFF; for(int i=7;i>=0;i--) if(((v>>>i)&1)!=0) bs.set(len+(7-i)); len+=8;}
        return new QrSegment(Mode.BYTE, data.length, bs, len);
    }

    public static int getTotalBits(List<QrSegment> segs, int ver){
        long result=0;
        for(QrSegment seg: segs){
            int ccbits=seg.mode.numCharCountBits(ver);
            if(seg.numChars >= (1<<ccbits)) return -1;
            result += 4 + ccbits + seg.bitLength;
            if(result > Integer.MAX_VALUE) return -1;
        }
        return (int)result;
    }
}

