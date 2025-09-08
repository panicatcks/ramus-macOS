/* Minimal QR Code generator (Nayuki) - single file version trimmed for our build use. */
package qr;

import java.util.Arrays;
import java.util.List;

public final class QrCode {
	public static final class Ecc {
		final int ordinal;
		final int dataCodewordsPerBlock;
		final int reedSolomonDegree;
		private Ecc(int ord,int dcw,int rs){ordinal=ord;dataCodewordsPerBlock=dcw;reedSolomonDegree=rs;}
		public static final Ecc LOW=new Ecc(0,19,7);
		public static final Ecc MEDIUM=new Ecc(1,16,10);
		public static final Ecc QUARTILE=new Ecc(2,13,13);
		public static final Ecc HIGH=new Ecc(3,9,17);
	}

	public final int size;
	public final boolean[][] modules;
	public final boolean[][] isFunction;

	private QrCode(int size){this.size=size;modules=new boolean[size][size];isFunction=new boolean[size][size];}

	public static QrCode encodeText(String text, Ecc ecl){
		return encodeSegments(java.util.Arrays.asList(QrSegment.makeBytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))), ecl);
	}

	public static QrCode encodeSegments(List<QrSegment> segs, Ecc ecl){
		int minVer=1,maxVer=40,mask=-1;
		for(int ver=minVer;ver<=maxVer;ver++){
			int dataCapacityBits=getNumDataCodewords(ver,ecl)*8;
			int dataUsedBits=QrSegment.getTotalBits(segs,ver);
			if(dataUsedBits!=-1 && dataUsedBits<=dataCapacityBits){
				int dataCapacityBytes=dataCapacityBits/8;
				java.util.BitSet data=new java.util.BitSet();
				int bitLen=0;
				for(QrSegment seg:segs){
					appendBits(data,bitLen,seg.mode.modeBits,4);bitLen+=4;
					appendBits(data,bitLen,seg.numChars,seg.mode.numCharCountBits(ver));bitLen+=seg.mode.numCharCountBits(ver);
					for(int i=0;i<seg.bitLength;i++) if(seg.data.get(i)) {data.set(bitLen+i);} bitLen+=seg.bitLength;
				}
				int terminator=Math.min(4,dataCapacityBits-bitLen);bitLen+=terminator;
				bitLen+=(-bitLen)&7;
				int padByte=0xEC;int pad2=0x11;int i=0;
				while(bitLen/8<dataCapacityBytes){int val=((i&1)==0)?padByte:pad2;appendBits(data,bitLen,val,8);bitLen+=8;i++;}
				byte[] finalBytes=new byte[dataCapacityBytes];
				for(i=0;i<dataCapacityBytes;i++){int b=0;for(int j=0;j<8;j++){if(data.get(i*8+j)) b|=1<<(7-j);}finalBytes[i]=(byte)b;}
				return new QrCode(ver,ecl,finalBytes,mask);
			}
		}
		throw new IllegalArgumentException("Data too long");
	}

	private static void appendBits(java.util.BitSet bs,int bitOffset,int val,int len){for(int i=len-1;i>=0;i--){boolean bit=((val>>>i)&1)!=0; if(bit) bs.set(bitOffset+(len-1-i));}}

	private QrCode(int ver, Ecc ecl, byte[] dataCodewords, int mask){
		int size=ver*4+17;this.size=size;modules=new boolean[size][size];isFunction=new boolean[size][size];
		drawFunctionPatterns(ver);
		byte[] allCodewords=addEccAndInterleave(dataCodewords, ver, ecl);
		java.util.BitSet bits=new java.util.BitSet();
		for(int i=0;i<allCodewords.length;i++) for(int j=0;j<8;j++) if(((allCodewords[i]>>>j)&1)!=0) bits.set(i*8+(7-j));
		int bitLen=allCodewords.length*8;
		int i=0;int dir=-1;int row=size-1;int col=size-1;for(col=size-1;col>0;col-=2){if(col==6) col--; for(;;){for(int c=0;c<2;c++){int cc=col-c;if(!isFunction[row][cc]){boolean bit=false; if(i<bitLen) bit=bits.get(i++); modules[row][cc]=bit;}} row+=dir; if(row<0||row>=size){row+=dir; dir=-dir; break;}}}
		applyMask(mask);
		drawFormatBits(ecl, this.mask);
	}

	private int mask;
	private void applyMask(int m){int minPenalty=Integer.MAX_VALUE;int best=0;for(int i=0;i<8;i++){ if(m!=-1 && i!=m) continue; boolean[][] temp=copy(modules); for(int r=0;r<size;r++)for(int c=0;c<size;c++) if(!isFunction[r][c]) temp[r][c]^=maskBit(i,r,c); int penalty=penaltyScore(temp); if(penalty<minPenalty){minPenalty=penalty;best=i;}} this.mask=best; for(int r=0;r<size;r++)for(int c=0;c<size;c++) if(!isFunction[r][c]) modules[r][c]^=maskBit(best,r,c);
	}

	private static boolean[][] copy(boolean[][] a){boolean[][] b=new boolean[a.length][a[0].length]; for(int i=0;i<a.length;i++) System.arraycopy(a[i],0,b[i],0,a[0].length); return b;}

	private static boolean maskBit(int m,int r,int c){switch(m){case 0:return (r+c)%2==0;case 1:return r%2==0;case 2:return c%3==0;case 3:return (r+c)%3==0;case 4:return ((r/2)+(c/3))%2==0;case 5:return (r*c)%2+(r*c)%3==0;case 6:return ((r*c)%2+(r*c)%3)%2==0;case 7:return ((r+c)%2+(r*c)%3)%2==0;default: throw new IllegalArgumentException();}}

	private static int penaltyScore(boolean[][] m){int n=m.length;int result=0; // Adjacent row/col runs
		for(int y=0;y<n;y++){int runColor=m[y][0]?1:0;int runLen=1; for(int x=1;x<n;x++){int c=m[y][x]?1:0;if(c==runColor) runLen++; else {if(runLen>=5) result+=3+(runLen-5); runColor=c; runLen=1;}} if(runLen>=5) result+=3+(runLen-5);} 
		for(int x=0;x<n;x++){int runColor=m[0][x]?1:0;int runLen=1; for(int y=1;y<n;y++){int c=m[y][x]?1:0;if(c==runColor) runLen++; else {if(runLen>=5) result+=3+(runLen-5); runColor=c; runLen=1;}} if(runLen>=5) result+=3+(runLen-5);} 
		// 2x2 blocks
		for(int y=0;y<n-1;y++)for(int x=0;x<n-1;x++){int c=m[y][x]?1:0; if(c==(m[y][x+1]?1:0) && c==(m[y+1][x]?1:0) && c==(m[y+1][x+1]?1:0)) result+=3;}
		// Finder-like patterns
		int[] pat={1,0,1,1,1,0,1,0,0,0,0};
		for(int y=0;y<n;y++) for(int x=0;x<n-10;x++){int sum=0; for(int k=0;k<11;k++) sum+= (m[y][x+k]?1:0)==pat[k]?1:0; if(sum==11) result+=40;}
		for(int x=0;x<n;x++) for(int y=0;y<n-10;y++){int sum=0; for(int k=0;k<11;k++) sum+= (m[y+k][x]?1:0)==pat[k]?1:0; if(sum==11) result+=40;}
		// Balance of black/white
		int black=0; for(int y=0;y<n;y++) for(int x=0;x<n;x++) if(m[y][x]) black++; int total=n*n; int k=Math.abs(black*20-total*10)/total; result+=k*10; return result;
	}

	private void drawFunctionPatterns(int ver){for(int i=0;i<size;i++){setFunction(6,i,true);setFunction(i,6,true);} drawFinderPattern(3,3); drawFinderPattern(size-4,3); drawFinderPattern(3,size-4); drawAlignmentPatterns(ver); drawTiming(); drawVersion(ver); }
	private void drawFinderPattern(int x,int y){for(int dy=-4;dy<=4;dy++) for(int dx=-4;dx<=4;dx++){int xx=x+dx,yy=y+dy; if(0<=xx&&xx<size&&0<=yy&&yy<size){int dist=Math.max(Math.abs(dx),Math.abs(dy)); boolean val = dist!=2 && dist!=4; setFunction(xx,yy,val);} }}
	private void drawAlignmentPatterns(int ver){if(ver==1) return; int[] pos=getAlignmentPatternPositions(ver); for(int i=0;i<pos.length;i++) for(int j=0;j<pos.length;j++){int x=pos[i], y=pos[j]; if(isFunction[y][x]) continue; for(int dy=-2;dy<=2;dy++) for(int dx=-2;dx<=2;dx++){boolean val=Math.max(Math.abs(dx),Math.abs(dy))!=1; setFunction(x+dx,y+dy,val);} }}
	private void drawTiming(){for(int i=0;i<size;i++){boolean b=i%2==0; if(!isFunction[6][i]) setFunction(6,i,b); if(!isFunction[i][6]) setFunction(i,6,b);} }
	private void drawVersion(int ver){if(ver<7) return; int rem=ver; int bits=0; for(int i=0;i<6;i++){bits=(bits<<1)|(rem&1); rem>>>=1;} for(int i=0;i<18;i++){boolean bit=((BITS_VERSION[ver-7]>>>i)&1)!=0; int a=size-11+i%3, b=i/3; setFunction(a,b,bit); setFunction(b,a,bit);} }
	private void drawFormatBits(Ecc ecl,int mask){int eclBits=(ecl.ordinal<<3)>>>1; int data=eclBits<<3|mask; int rem=data; int bits=data; for(int i=0;i<10;i++) bits=(bits<<1)^(((bits>>>9)&1)!=0?0x537:0); bits=(bits^0x5412)&0x7FFF; int fmt= (data<<10)|bits; // draw
		for(int i=0;i<6;i++) setFunction(8,i,((fmt>>>i)&1)!=0);
		setFunction(8,7,((fmt>>>6)&1)!=0); setFunction(8,8,((fmt>>>7)&1)!=0); setFunction(7,8,((fmt>>>8)&1)!=0);
		for(int i=9;i<15;i++) setFunction(14-i,8,((fmt>>>i)&1)!=0);
		for(int i=0;i<8;i++) setFunction(size-1-i,8,((fmt>>>i)&1)!=0);
		for(int i=8;i<15;i++) setFunction(8,size-15+i,((fmt>>>i)&1)!=0);
		setFunction(8,size-8,true);
	}
	private void setFunction(int x,int y,boolean val){modules[y][x]=val;isFunction[y][x]=true;}

	private static int[] getAlignmentPatternPositions(int ver){if(ver==1) return new int[]{}; int num=ver/7+2; int step = (ver==32)?26 : (ver*4+sizeBase(ver)-13)/(num*2-2); int[] result=new int[num]; result[0]=6; result[num-1]=ver*4+10; for(int i=1;i<num-1;i++) result[i]=result[0]+step*i; return result;}
	private static int sizeBase(int ver){return 17;}

	private static byte[] addEccAndInterleave(byte[] data,int ver,Ecc ecc){
		int numBlocks=NUM_ERROR_CORRECTION_BLOCKS[ecc.ordinal][ver];
		int blockEccLen=ECC_CODEWORDS_PER_BLOCK[ecc.ordinal][ver];
		int rawCodewords=getNumRawDataModules(ver)/8;
		int dataLen=rawCodewords - numBlocks*blockEccLen;
		int blockLen=dataLen/numBlocks; int shortBlocks = numBlocks - dataLen%numBlocks; int shortBlockLen=blockLen-1;
		byte[][] blocks=new byte[numBlocks][]; java.util.Arrays.fill(blocks,null);
		int k=0; for(int i=0;i<numBlocks;i++){int len=i<shortBlocks?shortBlockLen:blockLen; byte[] dat=new byte[len]; System.arraycopy(data,k,dat,0,len); k+=len; byte[] ecc=reedSolomonComputeRemainder(dat, blockEccLen); blocks[i]=concat(dat,ecc);} 
		byte[] result=new byte[rawCodewords]; int idx=0; for(int i=0;i<blockLen;i++) for(int j=0;j<numBlocks;j++){int len=blocks[j].length-blockEccLen; if(i<len) result[idx++]=blocks[j][i];}
		for(int i=0;i<blockEccLen;i++) for(int j=0;j<numBlocks;j++){int len=blocks[j].length; int eccIndex=len-blockEccLen+i; result[idx++]=blocks[j][eccIndex];}
		return result;
	}

	private static byte[] reedSolomonComputeRemainder(byte[] data,int degree){int[] gflog=GFLOG, gfilog=GFILOG; int[] result=new int[degree]; for(byte b : data){int factor=(b^result[0])&0xFF; System.arraycopy(result,1,result,0,degree-1); result[degree-1]=0; int[] gen=GEN_POLY[degree]; for(int i=0;i<degree;i++){result[i]^= multiply(gen[i], factor, gflog, gfilog);} } byte[] out=new byte[degree]; for(int i=0;i<degree;i++) out[i]=(byte)result[i]; return out;}
	private static int multiply(int x,int y,int[] gflog,int[] gfilog){ if(x==0||y==0) return 0; return gfilog[(gflog[x]+gflog[y])%255]; }
	private static byte[] concat(byte[] a,byte[] b){byte[] r=new byte[a.length+b.length]; System.arraycopy(a,0,r,0,a.length); System.arraycopy(b,0,r,a.length,b.length); return r;}

	private static int getNumRawDataModules(int ver){int result=(ver*4+17)*(ver*4+17) -  (3*8+2)*2 - (15*2+1) - (ver>=7?2*6*3:0); int[] align=getAlignmentPatternPositions(ver); int numAlign = align.length; result -= (numAlign*numAlign -3)*25; return result;}
	private static int getNumDataCodewords(int ver,Ecc ecl){return getNumRawDataModules(ver)/8 - ECC_CODEWORDS_PER_BLOCK[ecl.ordinal][ver]*NUM_ERROR_CORRECTION_BLOCKS[ecl.ordinal][ver];}

	private static final int[][] ECC_CODEWORDS_PER_BLOCK=new int[][]{
		{0,7,10,15,20,26,18,20,24,30,18,20,24,26,30,22,24,28,30,28,28,28,30,30,26,28,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30},
		{0,10,16,26,18,24,16,18,22,28,16,18,22,24,28,22,24,28,30,28,28,28,30,30,26,28,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30},
		{0,13,22,18,26,18,24,18,22,26,24,26,24,26,26,24,28,28,26,26,26,28,30,30,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28},
		{0,17,28,22,16,22,28,26,26,24,28,30,26,28,28,30,28,28,26,28,28,28,30,30,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28,28}
	};
	private static final int[][] NUM_ERROR_CORRECTION_BLOCKS=new int[][]{
		{0,1,1,1,1,2,2,2,2,2,1,4,2,4,3,5,5,1,5,3,3,4,2,4,4,3,5,5,4,4,4,5,5,5,1,5,3,5,3,5,4},
		{0,1,1,1,2,2,4,2,3,4,1,4,2,4,3,5,5,5,1,5,3,3,4,4,4,4,5,4,4,5,5,5,1,5,5,1,5,3,5,3,4},
		{0,1,1,2,2,4,2,4,4,4,2,4,4,4,4,4,4,4,4,4,3,4,5,4,4,5,5,4,5,5,4,5,5,1,5,3,5,3,5,3,4},
		{0,1,1,2,4,4,4,5,5,4,4,4,4,4,4,4,4,4,4,4,4,5,4,5,5,5,4,5,4,5,5,5,5,1,5,3,5,3,5,3,4}
	};
}

