package com.example.test2_0;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;


public class OO
{
	  private static final String TAG = "PPP";
	  private static final Datas as60xDatas = new Datas();
	  private static boolean JNI_isdeviceInit = false;

	  public static UsbDevice FCV_OpenDevice(Context mContext, int VendorId, int ProductId)
	  {
	    UsbDevice mdevice = null;
	    mdevice = as60xDatas.OpenAS60xDevice(mContext, VendorId, ProductId);

	    byte[] dataPtr = new byte[4];
	    dataPtr[0] = (byte)(VendorId & 0xFF);
	    dataPtr[1] = (byte)(VendorId >> 8 & 0xFF);
	    dataPtr[2] = (byte)(ProductId & 0xFF);
	    dataPtr[3] = (byte)(ProductId >> 8 & 0xFF);
	    int nRet = ProcessAPI.jniFpCapProcess_s(0, dataPtr, 0, 0);
	    if ((nRet != -1) && (nRet != -101))
	    {
	      JNI_isdeviceInit = true;
	    }

	    return mdevice;
	  }

	  public static UsbDevice FCV_OpenDeviceEx(Context mContext, int VendorId, int ProductId, int mEpOut, int mEpIn)
	  {
	    UsbDevice mdevice = null;
	    if ((-1 == mEpIn) && (-1 == mEpOut))
	    {
	      Log.d("AS60xIO", "Java Inited....");
	      mdevice = as60xDatas.OpenAS60xDevice(mContext, VendorId, ProductId);
	    }
	    else {
	      Log.d("AS60xIO", "JNI Inited....");
	      as60xDatas.tranSmitEndPointParamsJni(mEpOut, mEpIn);

	      byte[] dataPtr = new byte[4];
	      dataPtr[0] = (byte)(VendorId & 0xFF);
	      dataPtr[1] = (byte)(VendorId >> 8 & 0xFF);
	      dataPtr[2] = (byte)(ProductId & 0xFF);
	      dataPtr[3] = (byte)(ProductId >> 8 & 0xFF);
	      int nRet = ProcessAPI.jniFpCapProcess_s(0, dataPtr, 0, 0);
	      if ((nRet != -1) && (nRet != -101))
	      {
	        JNI_isdeviceInit = true;
	      }

	    }

	    return mdevice;
	  }

	  public static void FCV_CloseDevice(UsbDevice device)
	  {
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if (isConnected)
	    {
	      as60xDatas.CloseConnection(device);
	      device = null;
	    }
	    if (JNI_isdeviceInit)
	    {
	      ProcessAPI.jniFpCapProcess_s(-1, null, 0, 0);
	      JNI_isdeviceInit = false;
	    }
	  }

	  public static int FCV_GenImg(UsbDevice device)
	  {
	    int nRet = -1;
	    nRet = HS_GetImage(device, 0);
	    if (nRet == 0)
	    {
	      nRet = HS_GenChar(device, 0, 1);
	    }
	    return nRet;
	  }

	  public static int FCV_ReadIDCard(UsbDevice device, byte[] dataBuf, int[] isFPfeat, boolean isCryptoed)
	  {
	    isCryptoed = false;
	    int nRet = -1;

	    if ((isFPfeat.length < 1) || (dataBuf.length < 1296))
	    {
	      nRet = -2;
	      Log.e("AS60xIO", "HS_UpChar ArrayIndexOutOfBoundsException锛� nRet=" + nRet);
	    }
	    else
	    {
	      nRet = HS_GetCardMessage(device, 0);

	      isFPfeat[0] = nRet;
	      if ((nRet == 0) || (nRet == 24) || (nRet == 25))
	      {
	        int[] iImageLength = new int[2];
	        byte[] cardMsgArr = new byte[92160];
	        nRet = HS_UpImage(device, 0, cardMsgArr, iImageLength);

	        if (nRet == 0)
	        {
	          if (!isCryptoed)
	          {
	            System.arraycopy(cardMsgArr, 0, dataBuf, 0, 1296);
	          }
	        }
	      }

	    }

	    return nRet;
	  }

	  public static int FCV_Match(UsbDevice device, int[] score)
	  {
	    int nRet = -1;
	    nRet = HS_Match(device, 0, score);
	    return nRet;
	  }

	  public static int FCV_GetQualityScore(byte[] FingerImgBuf, byte[] nScore)
	  {
	    int nRet = -1;
	    nRet = EmbedAPI.GetQualityScore_s(FingerImgBuf, nScore);
	    return nRet;
	  }

	  public static int FCV_GetQualityScoreEx(byte[] FingerImgBuf, byte[] nScore, byte[] nResult)
	  {
	    int nRet = -1;
	    nRet =EmbedAPI.GetQualityScoreEx_s(FingerImgBuf, nScore, nResult);
	    return nRet;
	  }

	  private static void CompressByte(byte[] source, byte[] dest, int offset)
	  {
	    int temp = 0;
	    for (int j = 0; j < 92160; j += 8)
	    {
	      temp = 0;
	      for (int i = 0; i < 8; i++)
	      {
	        temp <<= 1;
	        if (source[(i + j)] != 0)
	          continue;
	        temp++;
	      }

	      dest[(offset + j / 8)] = (byte)temp;
	    }
	    System.arraycopy(source, 92160, dest, offset + 11520, 512);
	  }

	  public static int FCV_GenCharWithQA(UsbDevice device, int nAddr, int iBufferID, byte[] FingerImgBuf, byte[] nQaScore)
	  {
	    int nRet = -1;
	    byte[] tmpResult = new byte[184320];
	    nRet = FCV_GetQualityScoreEx(FingerImgBuf, nQaScore, tmpResult);

	    if (nRet >= 0)
	    {
	      byte[] changeRaw = new byte[92160];
	      CompressByte(tmpResult, changeRaw, 0);

	      HS_DownImage(device, nAddr, changeRaw, changeRaw.length);

	      HS_GenChar(device, nAddr, iBufferID + 16);
	    }
	    return nRet;
	  }

	  public static String FCV_getSerialNumber(UsbDevice device)
	  {
	    int nRet = -1;
	    byte[] dataBufAll = new byte['聵'];
	    byte[] dataBuf = new byte[24];
	    String serialNo = "";

	    nRet = HS_GetPageInfo(device, 0, dataBufAll);
	    System.arraycopy(dataBufAll, 128, dataBuf, 0, 24);

	    if (nRet == 0)
	    {
	      try
	      {
	        serialNo = new String(dataBuf, "UTF-8");
	      }
	      catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	      }
	    }
	    return serialNo;
	  }

	  public static int HS_Verfiy(UsbDevice device)
	  {
		Log.d(TAG, "2.0.0");
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      Log.d(TAG, "2.0.1");
	      nRet = as60xDatas.UDiskVerfiy();
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int debugSendData()
	  {
	    Log.e("AS60xIO", "enter debugSendData");
	    as60xDatas.UDiskVerfiy();
	    Log.e("AS60xIO", "leave debugSendData");
	    return 0;
	  }

	  public static int HS_GenChar(UsbDevice device, int nAddr, int iBufferID)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskGenFPFeature(iBufferID);
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_UpChar(UsbDevice device, int nAddr, int iBufferID, byte[] pTemplet, int[] iTempletLength)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      if ((iTempletLength.length < 1) || (pTemplet.length < 512))
	      {
	        nRet = -2;
	        Log.e("AS60xIO", "HS_UpChar ArrayIndexOutOfBoundsException锛� nRet=" + nRet);
	      }
	      else
	      {
	        ByteBuffer Buffer = ByteBuffer.allocate(512);
	        nRet = as60xDatas.UdiskUpChar(Buffer, 512, iBufferID);
	        if (nRet == 0)
	        {
	          iTempletLength[0] = 512;
	          System.arraycopy(Buffer.array(), 0, pTemplet, 0, 512);
	        }
	        else
	        {
	          iTempletLength[0] = 0;
	          Log.e("AS60xIO", "UdiskUpChar error锛� nRet=" + nRet);
	        }
	        Buffer.clear();
	      }
	      as60xDatas.CloseConnection(device);
	    }
	    else
	    {
	      nRet = -3;
	      Log.e("AS60xIO", "HS_UpChar as60xDatas.CheckConnection error锛� nRet=" + nRet);
	    }
	    return nRet;
	  }

	  public static int HS_DownChar(UsbDevice device, int nAddr, int iBufferID, byte[] pTemplet, int iTempletLength)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);

	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskDownChar(pTemplet, iTempletLength, iBufferID);
	      as60xDatas.CloseConnection(device);
	    }
	    else
	    {
	      nRet = -3;
	      Log.e("AS60xIO", "HS_DownChar as60xDatas.CheckConnection error锛� nRet=" + nRet);
	    }
	    return nRet;
	  }

	  public static int HS_GetImage(UsbDevice device, int nAddr)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskGetImage();
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_UpImage(UsbDevice device, int nAddr, byte[] pImageData, int[] iImageLength)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      if ((iImageLength.length < 1) || (pImageData.length < 92160))
	      {
	        nRet = -2;
	        Log.e("AS60xIO", "ArrayIndexOutOfBoundsException锛� nRet = -2");
	      }
	      else
	      {
	        ByteBuffer Buffer = ByteBuffer.allocate(92160);
	        nRet = as60xDatas.UdiskUpImage(Buffer, 92160);
	        if (nRet == 0)
	        {
	          iImageLength[0] = 92160;
	          System.arraycopy(Buffer.array(), 0, pImageData, 0, 92160);
	        } else {
	          iImageLength[0] = 0;
	          Log.e("AS60xIO", "UdiskUpImage error锛� nRet=" + nRet);
	        }
	        Buffer.clear();
	      }
	      as60xDatas.CloseConnection(device);
	    } else {
	      nRet = -3;
	      Log.e("AS60xIO", "as60xDatas.CheckConnection error锛� nRet=" + nRet);
	    }
	    return nRet;
	  }

	  public static int HS_DownImage(UsbDevice device, int nAddr, byte[] pImageData, int iLength)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskDownImage(pImageData, iLength);
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_GetCardMessage(UsbDevice device, int nAddr)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskGetCardMessage();

	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_Match(UsbDevice device, int nAddr, int[] iScore)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      if (iScore.length < 2)
	      {
	        nRet = -2;
	        Log.e("AS60xIO", "HS_Match iScore.length<2 nRet=" + nRet);
	      }
	      else
	      {
	        nRet = as60xDatas.UdiskMatch(iScore);
	        as60xDatas.CloseConnection(device);
	      }
	    }
	    return nRet;
	  }

	  public static int HS_InitFp(UsbDevice device, int nAddr)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskInitFp();
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_ClearModelFalgs(UsbDevice device, int nAddr)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskClearModelFlags();
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_GetPageInfo(UsbDevice device, int nAddr, byte[] dataBuf)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      ByteBuffer PageBuffer = ByteBuffer.allocate(1024);
	      nRet = as60xDatas.UdiskReadPageInfo(PageBuffer, 512);
	      if (nRet == 0)
	      {
	        System.arraycopy(PageBuffer.array(), 0, dataBuf, 0, dataBuf.length);
	        PageBuffer.clear();
	      }
	      else {
	        Log.e("AS60xIO", "HS_GetPageInfo is Error锛侊紒锛�");
	      }
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int HS_DownFacefeat(UsbDevice device, int nAddr, byte[][] faceFeat)
	  {
	    int nRet = -1;
	    long length = 0L;
	    byte[] feat = new byte[92160];

	    for (int i = 0; i < faceFeat.length; i++)
	    {
	      length += faceFeat[i].length;
	      if (length >= 41984L)
	      {
	        nRet = -2;
	        break;
	      }

	      System.arraycopy(faceFeat[i], 0, feat, 50176 + i * faceFeat[i].length, faceFeat[i].length);
	      nRet = 0;
	    }
	    if (nRet == 0)
	    {
	      nRet = HS_DownImage(device, nAddr, feat, 92160);
	    }
	    return nRet;
	  }

	  public static int FCV_FaceMatch(UsbDevice device, int nAddr, byte[][] faceFeat, int[] iScore)
	  {
	    int nRet = -1;
	    nRet = HS_DownFacefeat(device, nAddr, faceFeat);
	    if (nRet == 0)
	    {
	      nRet = HS_Match(device, nAddr, iScore);
	    }
	    return nRet;
	  }

	  public static int HS_StoreChar(UsbDevice device, int nAddr, int iBufferID, int iStoreID)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskStoreChar((byte)(iBufferID & 0xFF), iStoreID);
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }
	  public static int HS_Search(UsbDevice device, int nAddr, int iBufferID, int[] iUserID, int[] iScore) {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = as60xDatas.CheckConnection(device);
	    if ((isConnected) || (JNI_isdeviceInit))
	    {
	      nRet = as60xDatas.UdiskHSSearch((byte)(iBufferID & 0xFF), iUserID, iScore);
	      as60xDatas.CloseConnection(device);
	    }
	    return nRet;
	  }

	  public static int FCV_ModuleRegist(UsbDevice device, int UserID) {
	    byte[] pImageData = new byte[92160];
	    int[] iImageLength = new int[2];
	    int nRet = -1;
	    byte[] nScore = new byte[2];
	    int loopCount = 10;
	    do {
	      nRet = HS_GetImage(device, 0);
	      if (nRet == 0) {
	        nRet = HS_UpImage(device, 0, pImageData, iImageLength);
	        if (nRet == 0)
	          break;
	      }
	      loopCount--; } while (loopCount > 0);

	    if (loopCount <= 0) return -1;
	    nRet = HS_GenChar(device, 0, 1);
	    if (nRet != 0) return -2;
	    nRet = HS_StoreChar(device, 0, 1, UserID);
	    if (nRet != 0) return -3;
	    return 0;
	  }

	  public static int FCV_ModuleRegist(int UserID) {
	    return 0;
	  }

	  public static int FCV_Search(UsbDevice device, int[] UserID, int[] iScore) {
	    byte[] pImageData = new byte[92160];
	    int[] iImageLength = new int[2];
	    int nRet = -1;
	    byte[] nScore = new byte[2];
	    int loopCount = 10;
	    do {
	      nRet = HS_GetImage(device, 0);
	      if (nRet == 0) {
	        nRet = HS_UpImage(device, 0, pImageData, iImageLength);
	        if (nRet == 0)
	          break;
	      }
	      loopCount--; } while (loopCount > 0);

	    if (loopCount <= 0) return -1;
	    nRet = HS_GenChar(device, 0, 1);
	    if (nRet != 0) return -2;
	    nRet = HS_Search(device, 0, 1, UserID, iScore);
	    if (nRet != 0) return -3;
	    return 0;
	  }
}
