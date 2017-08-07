package com.example.test2_0;

import java.io.PrintStream;

public class ProcessAPI
{
	private static boolean isSoLoaded = false;

	  static
	  {
	    try {
	      System.loadLibrary("as60x_fp");
	      isSoLoaded = true;
	      System.out.println("isSoLoaded = " + isSoLoaded);
	    }
	    catch (Throwable e) {
	      isSoLoaded = false;
	      e.printStackTrace();
	    }
	  }

	  private static synchronized native int jniFpCapProcess(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3);

	  private static synchronized native int jniGetModuleType();

	  public static int jniFpCapProcess_s(int nEp, byte[] buff, int nlen, int nTimeOut)
	  {
	    if (!isSoLoaded) {
	      return -101;
	    }
	    return jniFpCapProcess(nEp, buff, nlen, nTimeOut);
	  }

	  public static int jnigetModuleType_s()
	  {
	    if (!isSoLoaded) {
	      return -102;
	    }
	    return jniGetModuleType();
	  }
}
