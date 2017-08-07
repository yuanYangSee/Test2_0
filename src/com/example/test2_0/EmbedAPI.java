package com.example.test2_0;

import java.io.PrintStream;
public class EmbedAPI
{
	private static boolean isSoLoaded = false;

	  static
	  {
	    try {
	      System.loadLibrary("FpQualityEmbed_jni");
	      isSoLoaded = true;
	      System.out.println("isSoLoaded = " + isSoLoaded);
	    }
	    catch (Throwable e) {
	      isSoLoaded = false;
	      e.printStackTrace();
	    }
	  }

	  private static synchronized native int GetQualityScore(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2);

	  private static synchronized native int GetQualityScoreEx(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3);

	  public static int GetQualityScore_s(byte[] FingerImgBuf, byte[] nScore)
	  {
	    if (!isSoLoaded) {
	      return -101;
	    }
	    return GetQualityScore(FingerImgBuf, nScore);
	  }

	  public static int GetQualityScoreEx_s(byte[] FingerImgBuf, byte[] nScore, byte[] nResult)
	  {
	    if (!isSoLoaded) {
	      return -102;
	    }
	    return GetQualityScoreEx(FingerImgBuf, nScore, nResult);
	  }
}
