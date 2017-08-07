package com.example.test2_0;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

@TargetApi(12)
public class Datas
{
	private static final String TAG = "PPP";
	  private static final int UdiskERROR = -101;
	  private static final int RecPacketERROR = -102;
	  private UsbManager mUsbManager = null;

	  private UsbEndpoint mEndpointIn = null;
	  private UsbEndpoint mEndpointOut = null;
	  private UsbDeviceConnection mConnection = null;
	  private int mEpOutjni = 2; private int mEpInjni = 129;

	  private int timeOut = 5000;

	  private byte GET_IMAGE = 1;
	  private byte GEN_CHAR = 2;
	  private byte MATCH = 3;
	  private byte SEARCH = 4;
	  private byte REG_MODULE = 5;
	  private byte STORE_CHAR = 6;
	  private byte LOAD_CHAR = 7;
	  private byte UP_CHAR = 8;
	  private byte DOWN_CHAR = 9;
	  private byte UP_IMAGE = 10;
	  private byte DOWN_IMAGE = 11;
	  private byte DEL_CHAR = 12;
	  private byte EMPTY = 13;
	  private byte WRITE_REG = 14;
	  private byte READ_PAR_TABLE = 15;
	  private byte ENROLL = 16;
	  private byte IDENTIFY = 17;
	  private byte SET_PWD = 18;
	  private byte VFY_PWD = 19;

	  private byte GET_RANDOM = 20;
	  private byte SET_CHIPADDR = 21;
	  private byte READ_INFPAGE = 22;
	  private byte PORT_CONTROL = 23;

	  private byte WRITE_NOTEPAD = 24;
	  private byte READ_NOTEPAD = 25;
	  private byte BURN_CODE = 26;
	  private byte HIGH_SPEED_SEARCH = 27;
	  private byte GEN_BINIMAGE = 28;
	  private byte TEMPLATE_NUM = 29;

	  private byte USERDEFINE = 30;
	  private byte GET_CARDMESSAGE = 30;
	  private byte READ_INDEXTABLE = 31;

	  private byte DEVICE_DESPLAY = 49;
	  private byte SENSOR_INIT = 33;
	  private byte CLEAR_RESET = 51;

	  private byte CMD = 1;
	  private byte DATA = 2;
	  private byte ENDDATA = 8;
	  private byte RESPONSE = 7;

	  byte[] MakePackage(byte[] pIn, int wLen, byte nType)
	  {
	    byte[] pOut = new byte[wLen + 11];
	    int CheckSum = 0;

	    pOut[0] = -17;
	    pOut[1] = 1;
	    pOut[2] = -1;
	    pOut[3] = -1;
	    pOut[4] = -1;
	    pOut[5] = -1;
	    pOut[6] = nType;
	    pOut[7] = (byte)(wLen + 2 >> 8 & 0xFF);
	    pOut[8] = (byte)(wLen + 2 & 0xFF);

	    for (int i = 0; i < wLen; i++)
	    {
	      pOut[(9 + i)] = pIn[i];
	    }
	    for (int i = 0; i < 3 + wLen; i++)
	      CheckSum += (pOut[(6 + i)] & 0xFF);
	    pOut[(9 + wLen)] = (byte)(CheckSum >> 8 & 0xFF);
	    pOut[(10 + wLen)] = (byte)(CheckSum & 0xFF);

	    return pOut;
	  }

	  private UsbEndpoint GetEndPoint(UsbDevice device, int nEndpoint)
	  {
	    UsbEndpoint ep1 = null;
	    if (device != null)
	    {
	      UsbInterface intf = device.getInterface(0);
	      ep1 = intf.getEndpoint(nEndpoint);
	      if (ep1.getType() != 2)
	      {
	        ep1 = null;
	      }
	    }
	    return ep1;
	  }

	  private boolean GetUsbEndpoints(UsbDevice device)
	  {
	    this.mEndpointIn = GetEndPoint(device, 0);
	    this.mEndpointOut = GetEndPoint(device, 1);

	    return (this.mEndpointIn != null) && (this.mEndpointOut != null);
	  }

	  private UsbDeviceConnection GetConnection(UsbDevice device)
	  {
	    UsbDeviceConnection connection = null;
	    if ((device != null) && (this.mUsbManager != null) && (this.mUsbManager.hasPermission(device)))
	    {
	      UsbInterface intf = device.getInterface(0);
	      connection = this.mUsbManager.openDevice(device);
	      if ((connection == null) || (!connection.claimInterface(intf, true)))
	      {
	        Log.e("AS60xDatas", "open device failed!!");
	        connection = null;
	      }
	    }
	    else {
	      Log.e("AS60xDatas", "usb has not Permission !");
	    }
	    return connection;
	  }

	  UsbDevice OpenAS60xDevice(Context mContext, int VendorId, int ProductId)
	  {
	    UsbDevice mdevice = null;

	    Log.d("AS60xDatas", "getSystemService(Context.USB_SERVICE)");

	    this.mUsbManager = ((UsbManager)mContext.getSystemService("usb"));
	    if (this.mUsbManager == null) {
	      Log.d("AS60xDatas", "mUsbManager == null return!!!");
	      return null;
	    }
	    Log.d("AS60xDatas", "mUsbManager=" + this.mUsbManager);

	    HashMap deviceList = this.mUsbManager.getDeviceList();
	    Log.d("AS60xDatas", "devicelist=" + deviceList);

	    if (deviceList.isEmpty()) {
	      Log.d("AS60xDatas", "deviceList.isEmpty");
	    }

	    Log.d("AS60xDatas", "1........");
	    Iterator deviceIterator = deviceList.values().iterator();
	    Log.d("AS60xDatas", "get device list  = " + deviceList.size());

	    if (!deviceIterator.hasNext())
	    {
	      Log.i("AS60xDatas", "Device has no next:");
	    }

	    while (deviceIterator.hasNext())
	    {
	      UsbDevice device = null;
	      device = (UsbDevice)deviceIterator.next();
	      String devInfo = device.getDeviceName() + "(" + device.getVendorId() + ":" + device.getProductId() + ")";
	      Log.e("AS60xDatas", devInfo);

	      if ((device.getVendorId() != VendorId) || (device.getProductId() != ProductId))
	        continue;
	      Log.d("AS60xDatas", "AS60x-mdevice found!");

	      mdevice = device;
	      boolean flag = GetUsbEndpoints(mdevice);
	      if (!flag)
	      {
	        Log.d("AS60xDatas", "Sorry, AS60x-GetUsbEndpoints failed!!!");
	        return null;
	      }
	      Log.d("AS60xDatas", "AS60x-GetUsbEndpoints Succeed!!!");

	      break;
	    }

	    return mdevice;
	  }

	  void tranSmitEndPointParamsJni(int mEpOut, int mEpIn)
	  {
	    this.mEpOutjni = mEpOut;
	    this.mEpInjni = mEpIn;
	    Log.d("AS60xDatas", "mEpOutjni==" + this.mEpOutjni + " mEpInjni=" + this.mEpInjni);
	  }

	  private int UDiskDownData(byte[] pBuf, int nLen)
	  {
	    return UDiskDownData(pBuf, nLen, this.timeOut);
	  }

	  private int UDiskDownData(byte[] pBuf, int nLen, int nTimeOut)
	  {
		  Log.d(TAG, "4.0.0");
	    int ret = -1;
	    int i = 0;

	    byte[] do_CBW = { 
	      85, 83, 66, 67, 
	      83, 121, 110, 111, 
	      0, 0, 0, 0, 0, 0, 10, -122 };

	    byte[] di_CSW = new byte[16];

	    do_CBW[8] = (byte)(nLen & 0xFF);
	    do_CBW[9] = (byte)(nLen >> 8 & 0xFF);
	    do_CBW[10] = (byte)(nLen >> 16 & 0xFF);
	    do_CBW[11] = (byte)(nLen >> 24 & 0xFF);

	    if ((this.mEndpointOut != null) && (this.mConnection != null))
	    {
	    	 Log.d(TAG, "4.0.1");
	      ret = this.mConnection.bulkTransfer(this.mEndpointOut, do_CBW, 31, this.timeOut);
	      Log.d(TAG, "4.0.2");
	    }
	    else
	    {
	    
	      ret = ProcessAPI.jniFpCapProcess_s(this.mEpOutjni, do_CBW, 31, this.timeOut);
	    }

	    if (ret != 31)
	    {
	      Log.e("AS60xDatas", "1...UDiskDownData DO_CBW fail!\n");
	      return -301;
	    }

	    int nT = 1;
	    if (nLen > 32000)
	    {
	      nT = 4;
	    }

	    for (int j = 0; j < nT; j++)
	    {
	      int blockLen = nLen / nT;
	      byte[] temp = Arrays.copyOfRange(pBuf, j * blockLen, (j + 1) * blockLen);
	      if ((this.mEndpointOut != null) && (this.mConnection != null))
	      {
	        ret = this.mConnection.bulkTransfer(this.mEndpointOut, temp, blockLen, this.timeOut);
	      }
	      else
	      {
	        ret = ProcessAPI.jniFpCapProcess_s(this.mEpOutjni, temp, blockLen, this.timeOut);
	      }

	      if (ret == blockLen)
	        continue;
	      Log.e("AS60xDatas", "2...UDiskDownData DI_DATA fail! ret=" + ret);
	      return -302;
	    }

	    if ((this.mEndpointIn != null) && (this.mConnection != null))
	    {
	      ret = this.mConnection.bulkTransfer(this.mEndpointIn, di_CSW, 13, nTimeOut);
	    }
	    else {
	      ret = ProcessAPI.jniFpCapProcess_s(this.mEpInjni, di_CSW, 13, nTimeOut);
	    }

	    if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
	    {
	      Log.e("AS60xDatas", "3...UDiskDownData DI_CSW fail!\n");
	      return -303;
	    }

	    di_CSW[3] = 67;
	    for (i = 4; i < 8; i++)
	    {
	      if (di_CSW[i] == do_CBW[i])
	        continue;
	      Log.e("AS60xDatas", "4...UDiskDownData DI_CSW fail!\n");
	      return -303;
	    }

	    return 0;
	  }

	  private int UDiskGetData(ByteBuffer img, int nLen)
	  {
	    int ret = -1;
	    byte[] recvbuffer = new byte[65536];

	    byte[] do_CBW = { 
	      85, 83, 66, 67, 
	      83, 121, 110, 111, 
	      0, 0, 0, 0, -128, 0, 10, -123 };

	    byte[] di_CSW = new byte[16];

	    do_CBW[8] = (byte)(nLen & 0xFF);
	    do_CBW[9] = (byte)(nLen >> 8 & 0xFF);
	    do_CBW[10] = (byte)(nLen >> 16 & 0xFF);
	    do_CBW[11] = (byte)(nLen >> 24 & 0xFF);

	    if ((this.mEndpointOut != null) && (this.mConnection != null))
	    {
	      ret = this.mConnection.bulkTransfer(this.mEndpointOut, do_CBW, 31, this.timeOut);
	    }
	    else {
	      ret = ProcessAPI.jniFpCapProcess_s(this.mEpOutjni, do_CBW, 31, this.timeOut);
	    }

	    if (ret != 31)
	    {
	      Log.e("AS60xDatas", "1...UDiskGetData DO_CBW fail!\n");
	      return -311;
	    }

	    int nT = 1;
	    if (nLen > 32000)
	    {
	      nT = 4;
	    }

	    for (int j = 0; j < nT; j++)
	    {
	      int blockLen = nLen / nT;

	      if ((this.mEndpointIn != null) && (this.mConnection != null))
	      {
	        ret = this.mConnection.bulkTransfer(this.mEndpointIn, recvbuffer, blockLen, this.timeOut);
	      }
	      else {
	        ret = ProcessAPI.jniFpCapProcess_s(this.mEpInjni, recvbuffer, blockLen, this.timeOut);
	      }

	      if (ret != blockLen)
	      {
	        Log.e("AS60xDatas", "2...UDiskGetData DI_DATA fail! ret=" + ret);
	        return -312;
	      }

	      img.put(recvbuffer, 0, blockLen);
	    }

	    if ((this.mEndpointIn != null) && (this.mConnection != null))
	    {
	      ret = this.mConnection.bulkTransfer(this.mEndpointIn, di_CSW, 13, this.timeOut);
	    }
	    else {
	      ret = ProcessAPI.jniFpCapProcess_s(this.mEpInjni, di_CSW, 13, this.timeOut);
	    }

	    if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
	    {
	      Log.e("AS60xDatas", "3...UDiskGetData DI_CSW fail!\n");
	      return -313;
	    }
	    for (int i = 4; i < 8; i++)
	    {
	      if (di_CSW[i] == do_CBW[i])
	        continue;
	      Log.e("AS60xDatas", "4...UDiskGetData DI_CSW fail!\n");
	      return -313;
	    }

	    return 0;
	  }

	  int UDiskVerfiy()
	  {
		  Log.d(TAG, "3.0.0");  
	    ByteBuffer buffer = ByteBuffer.allocate(65536);

	    byte[] cmdData = new byte[5];
	    cmdData[0] = this.VFY_PWD;
	    cmdData[1] = 0;
	    cmdData[2] = 0;
	    cmdData[3] = 0;
	    cmdData[4] = 0;

	    byte[] verfiyCmd = MakePackage(cmdData, 5, this.CMD);

	    if (UDiskDownData(verfiyCmd, verfiyCmd.length) != 0)
	    {
	    	
	      return -101;
	    }

	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();
	    return ret;
	  }

	  int UdiskDownImage(byte[] buff, int nLen)
	  {
	    byte[] downImgCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 11, 0, 15 };

	    if (UDiskDownData(downImgCmd, downImgCmd.length) != 0)
	    {
	      return -101;
	    }

	    int iTmpLen = nLen / 2;

	    int ret = UDiskDownData(buff, iTmpLen);
	    if (ret != 0)
	    {
	      return -101;
	    }

	    ret = UDiskDownData(Arrays.copyOfRange(buff, iTmpLen, buff.length), iTmpLen);
	    if (ret != 0)
	    {
	      return -101;
	    }

	    return 0;
	  }

	  int UdiskUpImage(ByteBuffer Buff, int nLen)
	  {
	    byte[] upImgCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 10, 0, 14 };

	    if (UDiskDownData(upImgCmd, upImgCmd.length) != 0)
	    {
	      return -101;
	    }

	    Buff.clear();
	    int iTmpLen = nLen / 2;

	    int ret = UDiskGetData(Buff, iTmpLen);
	    if (ret != 0)
	    {
	      return -101;
	    }

	    ret = UDiskGetData(Buff, iTmpLen);
	    if (ret != 0)
	    {
	      return -101;
	    }

	    Buff.flip();

	    return ret;
	  }

	  int UdiskGetImage()
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);
	    byte[] cmdData = new byte[5];
	    cmdData[0] = this.GET_IMAGE;

	    byte[] getImgCmd = MakePackage(cmdData, 1, this.CMD);

	    if (UDiskDownData(getImgCmd, getImgCmd.length) != 0)
	    {
	      return -101;
	    }

	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();
	    if (ret == 0)
	    {
	      byte[] Arr = Arrays.copyOfRange(buffer.array(), 0, 10);

	      int result = Arr[9];
	      return result;
	    }
	    return -101;
	  }

	  int UdiskGenFPFeature(int iBufferID)
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);

	    if (((iBufferID & 0xF) < 1) || ((iBufferID & 0xF) > 3))
	    {
	      iBufferID = 1;
	    }

	    byte[] genFPfeatCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 4, 2, (byte)iBufferID, 0, (byte)(iBufferID + 7) };

	    if (UDiskDownData(genFPfeatCmd, genFPfeatCmd.length) != 0)
	    {
	      return -101;
	    }

	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    if (ret != 0)
	    {
	      return -101;
	    }
	    buffer.flip();
	    return ret;
	  }

	  int UdiskUpChar(ByteBuffer pTemplet, int nLen, int iBufferID)
	  {
	    if ((iBufferID < 1) || (iBufferID > 3))
	    {
	      iBufferID = 1;
	    }

	    byte[] upCharCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 4, 8, (byte)iBufferID, 0, (byte)(13 + iBufferID) };

	    if (UDiskDownData(upCharCmd, upCharCmd.length) != 0)
	    {
	      return -101;
	    }

	    pTemplet.clear();
	    int iTmpLen = nLen;
	    int ret = UDiskGetData(pTemplet, iTmpLen);
	    if (ret != 0)
	    {
	      return -101;
	    }

	    pTemplet.flip();

	    return ret;
	  }

	  int UdiskDownChar(byte[] FeatBuff, int nLen, int iBufferID)
	  {
	    if ((iBufferID < 1) || (iBufferID > 3))
	    {
	      iBufferID = 1;
	    }

	    byte[] downCharCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 4, 9, (byte)iBufferID, 0, (byte)(14 + iBufferID) };

	    if (UDiskDownData(downCharCmd, downCharCmd.length) != 0)
	    {
	      return -101;
	    }

	    int iTmpLen = nLen;
	    int ret = UDiskDownData(FeatBuff, iTmpLen);
	    if (ret != 0)
	    {
	      return -101;
	    }

	    return 0;
	  }

	  int UdiskGetCardMessage()
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);

	    byte[] getCardCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 30, 0, 34 };

	    int ret = UDiskDownData(getCardCmd, getCardCmd.length);
	    if (ret != 0) {
	      Log.e("AS60xDatas", "UdiskGetCardMessage UDiskDownData " + ret);
	      return -101;
	    }

	    buffer.clear();
	    ret = UDiskGetData(buffer, 64);

	    buffer.flip();
	    if (ret == 0)
	    {
	      byte[] Arr = Arrays.copyOfRange(buffer.array(), 0, 10);

	      int result = Arr[9];
	      return result;
	    }
	    return -101;
	  }

	  int UdiskMatch(int[] result)
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);
	    byte[] matchCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 3, 0, 7 };
	    if (UDiskDownData(matchCmd, matchCmd.length) != 0)
	    {
	      return -101;
	    }
	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();

	    if (ret == 0)
	    {
	      byte[] Arr = new byte[20];
	      Arr = Arrays.copyOfRange(buffer.array(), 0, 20);

	      result[0] = ((Arr[10] & 0xFF) * 256 + (Arr[11] & 0xFF));
	      result[1] = ((Arr[12] & 0xFF) * 256 + (Arr[13] & 0xFF));
	      Log.d("AS60xDatas", "result[0]==" + result[0] + "\nresult[1]==" + result[1]);
	      if (result.length > 2)
	      {
	        result[2] = ((Arr[14] & 0xFF) * 256 + (Arr[15] & 0xFF));
	        Log.d("AS60xDatas", "result[2]==" + result[2]);
	      }
	    }
	    return ret;
	  }

	  int UdiskReadPageInfo(ByteBuffer Buff, int nLen)
	  {
	    byte[] getPageCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 22, 0, 26 };

	    if (UDiskDownData(getPageCmd, getPageCmd.length) != 0)
	    {
	      return -101;
	    }

	    Buff.clear();
	    int ret = UDiskGetData(Buff, nLen);
	    if (ret != 0)
	    {
	      return -101;
	    }
	    Buff.flip();
	    return ret;
	  }

	  int UdiskInitFp()
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);
	    byte[] getPageCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 50, 0, 54 };

	    if (UDiskDownData(getPageCmd, getPageCmd.length, 15000) != 0)
	    {
	      return -101;
	    }

	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();
	    if (ret == 0)
	    {
	      byte[] Arr = Arrays.copyOfRange(buffer.array(), 0, 10);

	      int result = Arr[9];
	      return result;
	    }

	    return -101;
	  }

	  int UdiskClearModelFlags()
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);
	    byte[] getPageCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 51, 0, 55 };

	    if (UDiskDownData(getPageCmd, getPageCmd.length) != 0)
	    {
	      return -101;
	    }

	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    if (ret != 0)
	    {
	      return -101;
	    }
	    buffer.flip();
	    return ret;
	  }

	  boolean CheckConnection(UsbDevice device)
	  {
	    boolean ret = false;
	    try
	    {
	      if ((this.mConnection == null) && (device != null))
	      {
	        this.mConnection = GetConnection(device);
	        if (this.mConnection == null)
	        {
	          return false;
	        }
	        this.mConnection.claimInterface(device.getInterface(0), false);
	        ret = true;
	      }
	    }
	    catch (SecurityException e) {
	      Log.e("AS60xDatas", "java.lang.SecurityException e: CheckConnection!!!");
	    }

	    return ret;
	  }

	  void CloseConnection(UsbDevice device)
	  {
	    if ((this.mConnection != null) && (device != null))
	    {
	      this.mConnection.releaseInterface(device.getInterface(0));
	      this.mConnection.close();
	      this.mConnection = null;
	    }
	  }

	  boolean CmdChkCom(byte[] cpRecBuf)
	  {
	    int nwCHK = 0;
	    int nwPKLen = (cpRecBuf[7] & 0xFF) * 256 + (cpRecBuf[8] & 0xFF);
	    int nwRecCHK = (cpRecBuf[(7 + nwPKLen)] & 0xFF) * 256 + (cpRecBuf[(8 + nwPKLen)] & 0xFF);

	    for (int i = 6; i < 7 + nwPKLen; i++) {
	      nwCHK += (cpRecBuf[i] & 0xFF);
	    }

	    return nwRecCHK == (nwCHK & 0xFFFF);
	  }

	  private int checkDataPacketIntegrity(byte[] Arr, int mode)
	  {
	    if (((Arr[0] & 0xFF) != 239) || ((Arr[1] & 0xFF) != 1))
	      return -102;
	    if ((Arr[6] & 0xFF) != mode) {
	      return -102;
	    }
	    if (CmdChkCom(Arr)) {
	      return 0;
	    }
	    return -102;
	  }
	  int UdiskStoreChar(byte iBufferID, int iPageID) {
	    ByteBuffer buffer = ByteBuffer.allocate(1024);
	    byte[] cmdData = new byte[4];
	    cmdData[0] = this.STORE_CHAR;
	    cmdData[1] = iBufferID;
	    cmdData[2] = (byte)(iPageID >> 8 & 0xFF);
	    cmdData[3] = (byte)(iPageID & 0xFF);
	    byte[] getPageCmd = MakePackage(cmdData, 4, this.CMD);

	    if (UDiskDownData(getPageCmd, getPageCmd.length) != 0)
	    {
	      return -101;
	    }
	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();
	    if (ret == 0)
	    {
	      byte[] Arr = Arrays.copyOfRange(buffer.array(), 0, buffer.remaining());

	      int check = checkDataPacketIntegrity(Arr, 7);
	      if (check == 0) {
	        return Arr[9];
	      }
	      return -101;
	    }
	    return -101;
	  }

	  int UdiskHSSearch(byte iBufferID, int[] UserID, int[] iScore)
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(1024);
	    byte[] cmdData = new byte[64];
	    cmdData[0] = this.SEARCH;
	    cmdData[1] = iBufferID;
	    cmdData[2] = 0;
	    cmdData[3] = 0;
	    cmdData[4] = 0;
	    cmdData[5] = 0;
	    byte[] getPageCmd = MakePackage(cmdData, 6, this.CMD);

	    if (UDiskDownData(getPageCmd, getPageCmd.length) != 0)
	    {
	      return -101;
	    }
	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();
	    if (ret == 0)
	    {
	      byte[] Arr = Arrays.copyOfRange(buffer.array(), 0, buffer.remaining());

	      int check = checkDataPacketIntegrity(Arr, 7);
	      if (check == 0) {
	        ret = Arr[9];
	        UserID[0] = ((Arr[10] & 0xFF) * 256 + (Arr[11] & 0xFF));
	        iScore[0] = ((Arr[12] & 0xFF) * 256 + (Arr[13] & 0xFF));
	      }
	      else {
	        return -101;
	      }
	    } else {
	      return -101;
	    }
	    return ret;
	  }
}
