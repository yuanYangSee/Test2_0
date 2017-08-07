package com.example.test2_0;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.hisign.AS60xSDK.AS60xIO;
import com.hisign.AS60xSDK.SDKUtilty;
import android.graphics.Bitmap;
/*Android 3.1 UsbManager*/
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity implements OnClickListener
{
	private static final String TAG = "PPP";
    /*指纹核验时附加参考信息*/
    private int[] fpFlags = new int[2];
    /*指纹采集是否完毕*/
    private boolean isFpComplated = false;
    /*证件采集是否完毕*/
    private boolean isCardComplated = false;
    /*设备初始化是否成功*/
    private boolean mSensorInited = false;
    /*打开设备传入PID、VID*/
    private UsbDevice mUsbDevice = null;
    private int VendorId = 0x2109;
    private int ProductId = 0x7638;
    
    /*UI信息*/
    private TextView ViewNote;
    private TextView ViewNote1;
    private TextView ViewNote2;
    private TextView ViewScore;//核验分数
    private TextView ViewScore1;//核验分数
    private TextView ViewIdNum;//证件号码
    private ImageView FingerBmp;//指纹图像
    private EditText editUserID ;
    /*音频资源*/
    private SoundPool pool;
    private Map<String, Integer> poolMap;

    private byte GET_IMAGE = 1;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.activity_main);
         setViewContent();
         /*注册监听USB*/
         registerUSBpermisson(this.getApplicationContext());

	}
	
	/*界面控件*/
    private void setViewContent () {     
        findViewById(R.id.Opendevice).setOnClickListener(this);
        findViewById(R.id.CapFingers).setOnClickListener(this);
        findViewById(R.id.CapIDcard).setOnClickListener(this);
        findViewById(R.id.Verification).setOnClickListener(this);
        findViewById(R.id.Cancel).setOnClickListener(this);
    editUserID = (EditText)findViewById(R.id.editRegisterNum);
    FingerBmp = (ImageView)findViewById(R.id.imageView1);
    ViewNote = (TextView)findViewById(R.id.textNote);
    ViewScore = (TextView)findViewById(R.id.IDcard_score);       
    ViewIdNum = (TextView)findViewById(R.id.IDcard_no);  
 
    findViewById(R.id.Button0).setOnClickListener(this);
    findViewById(R.id.Button1).setOnClickListener(this);
    findViewById(R.id.Button2).setOnClickListener(this);
    findViewById(R.id.Button3).setOnClickListener(this);
    findViewById(R.id.Button4).setOnClickListener(this);
    findViewById(R.id.ButtonTest).setOnClickListener(this);
    findViewById(R.id.buttonRegister).setOnClickListener(this);
    findViewById(R.id.buttonFP1Vn).setOnClickListener(this);
    poolMap = new HashMap<String, Integer>();
         pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
         poolMap.put("didi", pool.load(this, R.raw.didi, 1));
   }
    
    /*执行root控制台命令，参数为命令行字符串方式*/
    public boolean RootCommand(String command)
    {
     String packPath = MainActivity.this.getApplicationContext().getFilesDir().getAbsolutePath()+"/../lib";
     File execpath = new File(packPath);
        Process process = null;
        DataOutputStream os = null;
        try
        {
            process = Runtime.getRuntime().exec("su");//执行这一句，superuser.apk就会弹出授权对话框
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("cd " + execpath + "\n");
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e)
        {
            Log.d("*** DEBUG ***", "ROOT REE" + e.getMessage());
            return false;
        } finally
        {
            try
            {
                if (os != null)
                {
                    os.close();
                }
                process.destroy();
            } catch (Exception e)
            {
            }
        }
        Log.d("*** DEBUG ***", "Root SUC ");
        return true;
    }

  /*设备初始化*/
     private boolean InitUsbDevice (int vid, int pid)
     {
          
          /*1、原先的初始化方式，不传递输入、输出节点，SDK内部写死,仍然可用*/
          /*{
              String usbRoot = "chmod -R 777 /dev/bus/usb";
              boolean isRoot = RootCommand(usbRoot);//此命令若想执行成功，需要机器Root
              mUsbDevice = AS60xIO.FCV_OpenDevice(this, vid, pid);
          }*/
     
          //2、以下指定初始化方式及端点等设置
    	 Log.d(TAG, "InitUsbDevice");

          int initType = 0;
          boolean isSucceed = false;
          if (0==initType)//Java
	          {
	        	 
	        //	  mUsbDevice = AS60xIO.FCV_OpenDeviceEx(this, vid, pid, -1, -1);
	        	  Log.d(TAG, "OpenDevice。");
	        	  mUsbDevice = OpenDevice(this, vid, pid);
	        	  if(mUsbDevice==null)
	        	  {
	        		  Log.d(TAG, "mDevice==NULL");
	        	  }
	              Log.d(TAG, "OpenDevice OK");
	
	              mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	              if (mUsbDevice!=null && !mUsbManager.hasPermission(mUsbDevice))//Java方式：若无权限则，主动申请权限，等待用户回馈
	          {
	                   mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
	                   try {
	                        Thread.sleep(5000);//延时5s，等待用户选择
	                   } catch (InterruptedException e) {
	                        e.printStackTrace();}
	          }
	          }
          else 
	          {//Jni方式
        	  	Log.d(TAG, "进入打开设备else函数。");
	              String usbRoot = "chmod -R 777 /dev/bus/usb";
	              boolean isRoot = RootCommand(usbRoot);//此命令若想执行成功，需要机器Root
	              if (isRoot)
	              {
	                   mUsbDevice = AS60xIO.FCV_OpenDeviceEx(this, vid, pid, 0x01, 0x81);//as606:1 as602:2
	              }else {
	                   //Android设备无Root权限，无法初始化
	                    PrintHint(getString(R.string.GetRootFailed));
	              }
	          }
          
          /*验证是否初始化成功*/
          Log.d(TAG, "HS_Verfiy(mUsbDevice)");
    
      //    int nRet=AS60xIO.HS_Verfiy(mUsbDevice);
          int nRet =HS_Verfiy(mUsbDevice);
          if ( 0==nRet )
          {
        	
              if (pool != null) {
                   pool.play(poolMap.get("didi"), 1.0f, 1.0f, 0, 0, 1.0f);
              }
              isSucceed = true;
          //   PrintHint(getString(R.string.OpenSucced));
               ViewNote.setText(getString(R.string.OpenSucced));
          }else {
          //   PrintHint(getString(R.string.OpenFailed));
               ViewNote.setText(getString(R.string.OpenFailed));
          }
          return isSucceed;
     }
   
     private void PrintHint(String noteString) {
          Toast.makeText(MainActivity.this, noteString, Toast.LENGTH_SHORT).show();
     }
    /*获取指纹质量分数*/
    private int getFingerBmpQaScore(byte[] pImageData )
    {
     int score = 0;
          byte[] inScore = new byte[2];
          if (pImageData!=null && pImageData.length!=0 )
          {
               int nRet = AS60xIO.FCV_GetQualityScore(pImageData, inScore);
               if ( 1==nRet )
               {
                   score = inScore[0];
               }
          }
          return (score);
     }
    
    
    //##################
    public  int HS_Verfiy(UsbDevice device)
	  {
    	int nRet = -1;
        boolean isConnected = false;
        Log.d(TAG, "HS_Verfiy 调用 CheckConnection(device)");
        isConnected = CheckConnection(device);
        Log.d(TAG, "isConnected="+isConnected);
        if ((isConnected))
        {
        	Log.d(TAG, "HS_Verfiy 调用 UDiskVerfiy()");
        	nRet = UDiskVerfiy();
            CloseConnection(device);
        }
        return nRet;
	  }
    
     boolean CheckConnection(UsbDevice device)
    {
      boolean ret = false;
      try
      {
        if ((mConnection == null) && (device != null))
        {
        	Log.d(TAG, "CheckConnection调用GetConnection");
        	mConnection = GetConnection(device);
	        if (mConnection == null)
	          {
	        	Log.d(TAG, "mConnection=NULL。");
	            return false;
	          }
	         Log.d(TAG, "claimInterface");
	         mConnection.claimInterface(device.getInterface(0), false);
	         ret = true;
        }
      }
      catch (SecurityException e) {
        Log.e("AS60xDatas", "java.lang.SecurityException e: CheckConnection!!!");
      }
      Log.d(TAG, "CheckConnection OK");
      return ret;
    }
    
    private  UsbDeviceConnection GetConnection(UsbDevice device)
    {
    	Log.d(TAG, "GetConnection(UsbDevice device)。");
    	UsbDeviceConnection connection = null;
    	if ((device != null) && (mUsbManager != null) && (mUsbManager.hasPermission(device)))
      {
        Log.d(TAG, " return connection;");	
        UsbInterface intf = device.getInterface(0);
        connection = mUsbManager.openDevice(device);
        if ((connection == null) || (!connection.claimInterface(intf, true)))
        {
        	 Log.e("AS60xDatas", "连接为NULL 	或者	 声明 接口失败");
        	 connection = null;
        }
      }
      else {
        Log.e("AS60xDatas", "usb has not Permission !");
      }
      return connection;
    }
    
    
    private static byte VFY_PWD = 19;
    private static byte CMD = 1;
    
     int UDiskVerfiy()
	  {
	    ByteBuffer buffer = ByteBuffer.allocate(65536);

	    byte[] cmdData = new byte[5];
	    cmdData[0] = VFY_PWD;
	    cmdData[1] = 0;
	    cmdData[2] = 0;
	    cmdData[3] = 0;
	    cmdData[4] = 0;

	    Log.d(TAG, "UDiskVerfiy 调用 MakePackage(cmdData, 5, CMD);");
	    
	    byte[] verfiyCmd = MakePackage(cmdData, 5, CMD);
	    
	    Log.d(TAG, "UDiskVerfiy 调用 UDiskDownData ;");

	    if (UDiskDownData(verfiyCmd, verfiyCmd.length,5000) != 0)
	    {
	    	
	      return -101;
	    }

	    buffer.clear();
	    
	    Log.d(TAG, "UDiskVerfiy 调用 UDiskGetData(buffer, 64);");
	    
	    int ret = UDiskGetData(buffer, 64);
	    buffer.flip();
	    return ret;
	  }
    
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
	    Log.d(TAG, "pOUT="+pOut);

	    return pOut;
	  }
    
      private  UsbEndpoint mEndpointIn = null;
	  private  UsbEndpoint mEndpointOut = null;
	  private  UsbDeviceConnection mConnection = null;
    
    private  int UDiskDownData(byte[] pBuf, int nLen, int nTimeOut)
	  {
    	Log.d(TAG, "1 UDiskDownData(byte[] pBuf, int nLen, int nTimeOut)");
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

	    if ((mEndpointOut != null) && (mConnection != null))
	    {
	    	Log.d(TAG, "mEndpointOut和mConnection不为NULL");
	    	ret = mConnection.bulkTransfer(mEndpointOut, do_CBW, 16, 5000);
	    	Log.d(TAG, "1-1 ret=mConnection.bulkTransfer(mEndpointOut, do_CBW, 16, 5000) OK ;");
	    	Log.d(TAG, "ret="+ret);
	      
	    }
	    else
	    {
	    	 Log.d(TAG, "(mEndpointOut != null) && (mConnection != null)");
	  
	    }

	   /* if (ret != 31)
	    {
	      Log.e("AS60xDatas", "1...UDiskDownData DO_CBW fail!\n");
	      return -301;
	    }*/

	    int nT = 1;
	    if (nLen > 32000)
	    {
	      nT = 4;
	    }

	    for (int j = 0; j < nT; j++)
	    {
	      int blockLen = nLen / nT;
	      byte[] temp = Arrays.copyOfRange(pBuf, j * blockLen, (j + 1) * blockLen);
	      if ((mEndpointOut != null) && (mConnection != null))
	      {
	    	  Log.d(TAG, "1-2 mConnection.bulkTransfer(mEndpointOut, temp, blockLen, 5000);");
	          ret = mConnection.bulkTransfer(mEndpointOut, temp, blockLen, 5000);
	          Log.d(TAG, " ret = mConnection.bulkTransfer(mEndpointOut, temp, blockLen, 5000) OK");
	      }
	      else
	      {
	    	  Log.d(TAG, "JNI");
	      }
	      Log.d(TAG, "ret="+ret+" blockLen="+blockLen);
	      if (ret == blockLen)
	        continue;
	      Log.e("AS60xDatas", "2...UDiskDownData DI_DATA fail! ret=" + ret);
	      return -302;
	    }

	    if ((mEndpointIn != null) && (mConnection != null))
	    {
	      Log.d(TAG, "1-3 ret = mConnection.bulkTransfer(mEndpointIn, di_CSW, 13, nTimeOut)");	
	      ret = mConnection.bulkTransfer(mEndpointIn, di_CSW, 13, nTimeOut);
	    }
	    else {
	      //ret = ProcessAPI.jniFpCapProcess_s(this.mEpInjni, di_CSW, 13, nTimeOut);
	    }

	    if ((di_CSW[3] != 83) || (di_CSW[12] != 0))
	    {
	    	
	      Log.d(TAG, "3...UDiskDownData DI_CSW fail!\n");
	      return -303;
	    }
	    di_CSW[3] = 67;
	    Log.d(TAG, "di_CSW[3]="+di_CSW[3]+"	di_CSW[12]="+di_CSW[12]);
	    for (i = 4; i < 8; i++)
	    {
	      if (di_CSW[i] == do_CBW[i])
	        continue;
	      Log.d(TAG, "4...UDiskDownData DI_CSW fail!\n");
	      return -303;
	    }
	    Log.d(TAG, "UDiskDownData OK");  
	    return 0;
	  }
    
    private  int UDiskGetData(ByteBuffer img, int nLen)
	  {
    	Log.d(TAG, "2 UDiskGetData(ByteBuffer img, int nLen);");
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

	    if ((mEndpointOut != null) && (mConnection != null))
	    {
	      Log.d(TAG, "2-1 ret=bulkTransfer(mEndpointOut, do_CBW, 16,5000);");
	      ret = mConnection.bulkTransfer(mEndpointOut, do_CBW, 16,5000);
	    }
	    else {
	    //  ret = ProcessAPI.jniFpCapProcess_s(this.mEpOutjni, do_CBW, 31, this.timeOut);
	    }
	    Log.d(TAG, "ret="+ret);
/*
	    if (ret != 31)
	    {
	      Log.e("AS60xDatas", "1...UDiskGetData DO_CBW fail!\n");
	      return -311;
	    }*/

	    int nT = 1;
	    if (nLen > 32000)
	    {
	      nT = 4;
	    }

	    for (int j = 0; j < nT; j++)
	    {
	      int blockLen = nLen / nT;

	      if ((mEndpointIn != null) && (mConnection != null))
	      {
	    	Log.d(TAG, "2-2 mConnection.bulkTransfer(mEndpointIn, recvbuffer, blockLen, 5000);");
	        ret = mConnection.bulkTransfer(mEndpointIn, recvbuffer, blockLen, 5000);
	      }
	      else {
	    //    ret = ProcessAPI.jniFpCapProcess_s(this.mEpInjni, recvbuffer, blockLen, 5000);
	      }

	      if (ret != blockLen)
	      {
	        Log.e(TAG, "2...UDiskGetData DI_DATA fail! ret=" + ret);
	        return -312;
	      }

	      img.put(recvbuffer, 0, blockLen);
	      Log.d(TAG, "2-2 recvbuffer="+recvbuffer[0]+" "+recvbuffer[2]);
	    }

	    if ((mEndpointIn != null) && (mConnection != null))
	    {
	    	 Log.d(TAG, "2-3 bulkTransfer(mEndpointIn, di_CSW, 13, 5000);");
	      ret = mConnection.bulkTransfer(mEndpointIn, di_CSW, 13, 5000);
	    }
	    else {
	   //   ret = ProcessAPI.jniFpCapProcess_s(this.mEpInjni, di_CSW, 13, 5000);
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
	    Log.d(TAG, "di_CSW[3]="+di_CSW[3]+  "     di_CSW[12]="+di_CSW[12]); 
	    Log.d(TAG, "UDiskGetData OK");

	    return 0;
	  }
    
 

    UsbDevice OpenDevice(Context mContext, int VendorId, int ProductId)
    {
    	UsbDevice mdevice = null;
    	
      Log.d("AS60xDatas", "getSystemService(Context.USB_SERVICE)");

      mUsbManager = ((UsbManager)mContext.getSystemService("usb"));
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
    
    private boolean GetUsbEndpoints(UsbDevice device)
    {
      mEndpointIn = GetEndPoint(device, 0);
      mEndpointOut = GetEndPoint(device, 1);

      return (mEndpointIn != null) && (mEndpointOut != null);
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
    
    //采集图像
    public int HS_GetImage(UsbDevice device, int nAddr)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = CheckConnection(device);
	    if (isConnected)
	    {
	      nRet = UdiskGetImage();
	      CloseConnection(device);
	    }
	    return nRet;
	  }
    
    int UdiskGetImage()
	  {
    	 Log.d(TAG, "UdiskGetImage();");
	    ByteBuffer buffer = ByteBuffer.allocate(65536);
	    byte[] cmdData = new byte[5];
	    cmdData[0] = GET_IMAGE;

	    byte[] getImgCmd = MakePackage(cmdData, 1, CMD);

	    if (UDiskDownData(getImgCmd, getImgCmd.length,5000) != 0)
	    {
	      return -101;
	    }

	    buffer.clear();
	    int ret = UDiskGetData(buffer, 64);
	    Log.d(TAG, "2.2 UDiskGetData buffer="+buffer);
	    buffer.flip();
	    if (ret == 0)
	    {
	      byte[] Arr = Arrays.copyOfRange(buffer.array(), 0, 10);
	      
	      Log.d(TAG, "Arr 0-9="+Arr);

	      int result = Arr[9];
	      return result;
	    }
	    return -101;
	  }
    
    void CloseConnection(UsbDevice device)
	  {
	    if( (mConnection != null) && (device != null))
	    {
	      mConnection.releaseInterface(device.getInterface(0));
	      mConnection.close();
	      mConnection = null;
	    }
	  }
    
    public int HS_UpImage(UsbDevice device, int nAddr, byte[] pImageData, int[] iImageLength)
	  {
	    int nRet = -1;
	    boolean isConnected = false;
	    isConnected = CheckConnection(device);
	    if ((isConnected) )
	    {
	      if ((iImageLength.length < 1) || (pImageData.length < 92160))
	      {
	        nRet = -2;
	        Log.e("AS60xIO", "ArrayIndexOutOfBoundsException");
	      }
	      else
	      {
	        ByteBuffer Buffer = ByteBuffer.allocate(92160);
	        nRet = UdiskUpImage(Buffer, 92160);
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
	      CloseConnection(device);
	    } else {
	      nRet = -3;
	      Log.e("AS60xIO", "as60xDatas.CheckConnection error锛� nRet=" + nRet);
	    }
	    return nRet;
	  }
    
	  int UdiskUpImage(ByteBuffer Buff, int nLen)
	  {
	    byte[] upImgCmd = { -17, 1, -1, -1, -1, -1, 
	      1, 0, 3, 10, 0, 14 };

	    if (UDiskDownData(upImgCmd, upImgCmd.length,5000) != 0)
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
    


	@Override
	public void onClick(View v)
	{
		// TODO Auto-generated method stub
		int curId = v.getId();
        if (curId == R.id.Opendevice)//打开设备
        {
        	Log.d(TAG, "点击打开设备。");

            mSensorInited = InitUsbDevice(VendorId, ProductId);
        }
        else if (curId == R.id.CapFingers)//指纹采集
        {
            int nRet = -1;
            long startTime, endTime;
            if( mSensorInited )
           {
                 startTime = System.currentTimeMillis();
                 Log.d(TAG, "点击采集图像。");
             while(true)
             {
                      nRet = HS_GetImage(mUsbDevice, 0 );
                      if ( 0==nRet )
                      {
                    	  Log.d(TAG, "nRet="+nRet);
                      int iImageLength[] = new int[2];
                      byte[] FpArray = new byte[256*360];
                      int result = HS_UpImage(mUsbDevice, 0, FpArray, iImageLength);
                      if ( 0==result && iImageLength[0]==256*360)
                      {
                    	  Log.d(TAG, "result="+result+"   iImageLength[0]="+iImageLength[0]);
                           SDKUtilty.SaveRawToBmp(FpArray, "/sdcard/Fptest.bmp", true);
                           Bitmap fpbmp = SDKUtilty.GetBitMapFromFilePath(MainActivity.this, "/sdcard/Fptest.bmp", 256, 360);
                                FingerBmp.setImageBitmap(fpbmp);
                                isFpComplated = true;
                      }
                      //PrintHint(getString(R.id.))
                      break;
                      }
                      endTime = System.currentTimeMillis();
                 if (1500 <= (endTime-startTime))//超时自动退出
                 {
                      SDKUtilty.showToast(MainActivity.this, "采集失败！", Toast.LENGTH_SHORT);
                      break;
                 }
             }
           }else {
                PrintHint(getString(R.string.DeviceNotinited));
            }
        }
        else if (curId == R.id.CapIDcard)//证件采集
        {
            if( mSensorInited )
        {
                 byte[] dataBuf= new byte[2048];
            byte[] datawzBuf = new byte[256];
            //返回值：0成功 2失败  0x18/0x19身份证中无指纹区域或未注册成功
            /**@Description:身份证中指纹特征信息
             * 0x18：身份证中无指纹区域
         * 0x19：身份证中有指纹区域，但未注册成功
         */
            int nRet = AS60xIO.FCV_ReadIDCard (mUsbDevice, dataBuf, fpFlags, false );              
            if(nRet==0 || nRet==0x18 || nRet==0x19)
            {
                 ViewIdNum.setText("");
                 ViewScore.setText("");
                 if (pool != null) {
                      pool.play(poolMap.get("didi"), 1.0f, 1.0f, 0, 0, 1.0f);
                 }
                 SDKUtilty.showToast(MainActivity.this, "采集成功!", Toast.LENGTH_SHORT);
                 datawzBuf = Arrays.copyOfRange(dataBuf, 16, 256+16);
                 String[] decodeInfo = new String[10];
                 decodeInfo = SDKUtilty.decodePersionInfo(datawzBuf);//解码
                 String Idnum = decodeInfo[5].substring(0, 10)+"****"+decodeInfo[5].substring(14, 18);
                 isCardComplated = true;
                 ViewIdNum.setText(" "+Idnum);
            }
            else {
                 SDKUtilty.showToast(MainActivity.this, "采集失败!", Toast.LENGTH_SHORT);
                 isCardComplated = false;
                 }
        }else {
            SDKUtilty.showToast(MainActivity.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
        }
        else if (curId == R.id.Verification)//（指纹&身份）核验
        {
            if( mSensorInited )
            {
                 if(!isFpComplated ){
                      ViewScore.setText(" 请先采集指纹图像");
                 }else if ( isCardComplated )
             {
                      getFingerFeature(mUsbDevice, "/sdcard/Fptest.bmp", 1);
                      Log.d(TAG, "fpFlags[0]=="+fpFlags[0]);
                      if (0==fpFlags[0])
                 {
                           //int []score = new int[2];
                           int []score = new int[3];
                      int ret = AS60xIO.FCV_Match( mUsbDevice, score);
                      if ( 0==ret)
                      {
                           if (pool != null) {
                                pool.play(poolMap.get("didi"), 1.0f, 1.0f, 0, 0, 1.0f);
                           }
                           float fscore1 = (float)score[0]/10;//采集指纹与身份证内第一枚的比分（特征Buf1与特征Buf2)
                           float fscore2 = (float)score[1]/10; //采集指纹与身份证内第二枚的比分（特征Buf1与特征Buf3)
                           float fscore3 = (float)score[2]/10; //登记互比时第二枚与第三枚的比分（特征Buf2与特征Buf3)
                          ViewScore.setText("【第一枚】"+String.valueOf(fscore1)+"%"+"【第二枚】"+String.valueOf(fscore2)+"%");
                      }else {
                           ViewScore.setText(" FCV_Match核验失败！");
                           }
                 }
                      else if(fpFlags[0]==0x18)
                      {
                           ViewScore.setText(" 证件中无指纹区域，无法核验 !");
                      }
                      else if(fpFlags[0]==0x19)
                      {
                           ViewScore.setText(" 证件中有指纹区域，但未注册成功");
                      }
                      //isCardComplated = false;
             }else{
                  SDKUtilty.showToast(MainActivity.this, getString(R.string.NotCapidcard), Toast.LENGTH_SHORT);
             }
            }else {
                 SDKUtilty.showToast(MainActivity.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
        }
        else if (curId == R.id.ButtonTest)//测试两张指纹图片相似度
        {
            int nRet1, nRet2 = -1;
            {
                 ViewScore.setText("");//显示分数
                 nRet1 = getFingerFeature(mUsbDevice, "/sdcard/01.bmp", 1);
                 nRet2 = getFingerFeature(mUsbDevice, "/sdcard/02.bmp", 2);
                 if (nRet1==0 && nRet2==0)
                 {
                      int []score = new int[2];
                  int ret = AS60xIO.FCV_Match (mUsbDevice, score);
                  if (ret == 0)
                  {
                      String matchResultStr = String.format(" 【指纹核验分数】 %.1f%%", score[0]*0.1);
                       ViewScore.setText(matchResultStr);
                  }else {
                      ViewScore.setText(" FCV_Match比对失败！");
                      }
                 }else {
                      ViewScore.setText(" 未发现指纹图片，指纹特征提取失败无法核验！");
                 }
                 
                 /*
                 //读取特征文件数据，然后下发至模块内部特征缓冲区（1/2/3），用作比对。
                 byte[] DownCharBuffer=new byte[512];
                 readFromSd("/sdcard/posFeature.txt", DownCharBuffer);
                 int nRet0 = AS60xIO.HS_DownChar(mUsbDevice, 0, 1, DownCharBuffer, 512);//下载特征到缓冲区1
                 if (nRet0!=0)
                 {
                      ViewScore.setText(" 指纹特征下载失败，无法核验！");
                      return;
                 }
                 
                 nRet1 = getFingerFeature(mUsbDevice, "/sdcard/01.bmp", 2);
                 nRet2 = getFingerFeature(mUsbDevice, "/sdcard/02.bmp", 3);
                 if (nRet1==0 && nRet2==0){
                      int []score = new int[2];
                      int ret = AS60xIO.FCV_Match (mUsbDevice, score);
                      String Score1 = String.format("%2.1f%%", (score[0]*0.1));
                      String Score2 = String.format("%2.1f%%", (score[1]*0.1));
                      ViewScore.setText("指纹核验分数：【1-2】"+Score1+" 【1-3】"+Score2);    //显示分数1V2, 1V3
                 }else{
                      ViewScore.setText(" 未发现指纹图片，指纹特征提取失败无法核验！");
                 }
                 
                 */
            }
        }
        else if(curId == R.id.buttonRegister) //注册
      {
        if( mSensorInited )
            {
            String s = editUserID.getText().toString();
            if(s.length() == 0) {
                 ViewNote.setText("请输入想保存指纹的位置。");
            }else{
                 int regUserID = Integer.parseInt(s) ;
                 ViewNote.setText("正在注册指纹，位置"+regUserID);
                 int nRet = AS60xIO.FCV_ModuleRegist(mUsbDevice,regUserID);
                 if(nRet == 0 ){
                      ViewNote.setText("注册成功");
                      ViewScore.setText("注册指纹成功，位置"+regUserID);
                 }else{
                      ViewNote.setText("注册失败");
                      ViewScore.setText("注册指纹失败，错误码"+nRet);
                 }
            }
            
            }else{
                 SDKUtilty.showToast(MainActivity.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
      }
      else if(curId == R.id.buttonFP1Vn) //1VN
        //TODO
      {
        if( mSensorInited )
            {
            int []iUserID = new int[2]  ;
            int []iScore = new int[2] ;
            ViewNote.setText("正在指纹库中搜索");
            int nRet = AS60xIO.FCV_Search(mUsbDevice,iUserID,iScore);
            if(nRet == 0 ){
                 ViewNote.setText("验证成功");
                 ViewScore.setText("指纹验证成功，ID="+iUserID[0]+"    Score="+(float)(iScore[0]/10));
            }else{
                 ViewNote.setText("验证失败");
                 ViewScore.setText("指纹验证失败，错误码"+nRet);
            }
            
            }else{
                 SDKUtilty.showToast(MainActivity.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
      }
        else if(curId == R.id.Cancel)
        {
            alertShutdownDialog();
        }
        else if (curId == R.id.Button0)//内部接口测试
        {
            testIntraface();
        }
        else if (curId == R.id.Button1)//测试内部序列号（不可靠，部分机型执行不成功）
        {
            String[] Cmd = {"/system/bin/cat", "/proc/bus/usb/devices"};
            String result = "";
            try {
                 result = runCmd(Cmd, "system/bin/");
            } catch (IOException e) {
                 e.printStackTrace();
            }
            Log.d(TAG, result);
            //"Vendor=261a ProdID=000d"
            //P:  Vendor=2109 ProdID=7638 Rev= 1.00
            int vidIndex = result.indexOf("Vendor=261a ProdID=000d");
            if ( vidIndex >=0 )
            {
                 String serialNum = null;
                 //S:  SerialNumber=805f393913
                 int serialIndex = result.indexOf("SerialNumber=", vidIndex);
                 if ( serialIndex >=0 )
                 {
                      serialNum = result.substring(serialIndex+13, serialIndex+30);
                 }
                 if (serialNum != null)
                 {
                      ViewNote.setText("【定制序列号】"+serialNum);
                 }else {
                      ViewNote.setText("定制序列号获取失败!");
                 }
            }else {
                 ViewNote.setText("vidIndex获取失败!");
            }
        }
        else if (curId == R.id.Button2)//指纹图片质量测试
        {
   
            byte[] data = new byte[256*360];
            byte QaScore[]=new byte[2];
            
            readFromSd("/sdcard/2014-06-04 142918.raw", data);
            int nRet= AS60xIO.FCV_GenCharWithQA(mUsbDevice, 0, 2, data, QaScore);
            ViewNote.setText("nRet="+nRet + "QaScore[0]="+QaScore[0]);
            
            readFromSd("/sdcard/2014-06-04 143001.raw", data);
            AS60xIO.FCV_GenCharWithQA(mUsbDevice, 0, 1, data, QaScore);
            ViewNote1.setText("nRet1="+nRet + "QaScore1[0]="+QaScore[0]);
            
            readFromSd("/sdcard/2014-06-04 142950.raw", data);
            AS60xIO.FCV_GenCharWithQA(mUsbDevice, 0, 3, data, QaScore);
            ViewNote2.setText("nRet2="+nRet + "QaScore2[0]="+QaScore[0]);
            int[]iScore= new int[3];//最小长度必须>=2,否则出错
            AS60xIO.HS_Match(mUsbDevice, 0, iScore);
            Log.d(TAG, "iScore[0]="+iScore[0] +"iScore[1]="+iScore[1]);
            ViewScore.setText("分数1："+iScore[0]+ " 分数2："+iScore[1]+" 分数3："+iScore[2]);
            
            readFromSd("/sdcard/2014-06-04 134837_probe.raw", data);
            AS60xIO.FCV_GenCharWithQA(mUsbDevice, 0, 1, data, QaScore);
            
            AS60xIO.HS_Match(mUsbDevice, 0, iScore);
            Log.d(TAG, "iScore[0]_last="+iScore[0] +"iScore[1]_last="+iScore[1]);
            ViewScore1.setText("分数1_last："+iScore[0]+ " 分数2_last："+iScore[1] +" 分数3_last："+iScore[2]);
            
            /*
            int nRet = -1;
            if( mSensorInited )
           {
                 nRet = AS60xIO.HS_GetImage(mUsbDevice, 0);
                 if (0==nRet)
                 {
                      byte[] pImageData = new byte[256*360];
                      int[] iImageLength = new int[2];
                      nRet = AS60xIO.HS_UpImage(mUsbDevice, 0, pImageData, iImageLength);
                      if (nRet == 0)
                      {
                           byte[] inScore = new byte[2];
                           SDKUtilty.SaveRawToBmp(pImageData, "/sdcard/pImageData.bmp", true);
                           Bitmap fpbmp = SDKUtilty.GetBitMapFromFilePath(AS60xTestDemo.this, "/sdcard/pImageData.bmp", 256, 360);
                           FingerBmp.setImageBitmap(fpbmp);
                           //int ret = AS60xIO.FCV_GetQualityScore(pImageData, inScore);
                           
                      }
                 }else {
                      ViewNote.setText("请将手指按捺在采集器上！");
                 }
                 
           }else {
             SDKUtilty.showToast(AS60xTestDemo.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
            */
        
        }
        else if (curId == R.id.Button3)//获取产品序列号
        {
            if( mSensorInited )
           {
                 String serialNo = "";
                 serialNo= AS60xIO.FCV_getSerialNumber(mUsbDevice);
                 if (serialNo != "")
                 {
                      ViewNote.setText("【产品序列号】"+serialNo);
                 }else {
                      ViewNote.setText("产品序列号获取失败!");
                 }
           }else {
             SDKUtilty.showToast(MainActivity.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
        }
        else if (curId == R.id.Button4)
        {
            if( mSensorInited )
            {
                 int nRet = AS60xIO.HS_InitFp(mUsbDevice, 0);//返回结果需要15秒
                 if (nRet == 0)
                 {
                      ViewNote.setText("指纹传感器初始化成功！");
                 }else if (nRet == 2){
                      ViewNote.setText("未发现传感器！");
                 }else {
                      ViewNote.setText("指纹传感器初始化失败！");
                 }
            }else {
             SDKUtilty.showToast(MainActivity.this, "设备未打开，请打开设备!", Toast.LENGTH_SHORT);
            }
        
   }

		
	}
	
	 /*USB设备控制*/
    private  UsbManager mUsbManager;
     public void registerUSBpermisson(Context context)
     {
          mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
          mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
          IntentFilter filter = new IntentFilter();
          filter.addAction(ACTION_USB_PERMISSION);
          filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);//拔出USB
        context.registerReceiver(mUsbReceiverPermission, filter);
     }
      /*获取USB权限*/
     private PendingIntent mPermissionIntent;
     private static final String ACTION_USB_PERMISSION = "Request_USB_PERMISSION";
     private final BroadcastReceiver mUsbReceiverPermission = new BroadcastReceiver()
     {
         @Override
          public void onReceive(Context context, Intent intent)
         {
             String action = intent.getAction();
             if (ACTION_USB_PERMISSION.equals(action))
             {
               SDKUtilty.showToast(MainActivity.this, "申请USB通信权限！", Toast.LENGTH_SHORT);
               synchronized (this){
                        UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);                   
                        //用户允许权限申请true
                   if (usbDevice != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                   {
                        //用户同意 ;
                        mSensorInited = true;
                   }
                   else//用户拒绝
                  {
                        mSensorInited = false;
                        Log.e(TAG, "permission denied for device！！！");
                  }
               }
             }
             else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
              {
                   mSensorInited = false;
                   AS60xIO.FCV_CloseDevice(mUsbDevice);//关闭设备，释放资源
                   SDKUtilty.showToast(MainActivity.this, "注意：USB采集设备已拔出！", Toast.LENGTH_SHORT);
             }
         }
     };
     
     /*指纹特征提取bmp*/
     private int getFingerFeature(UsbDevice mUsbDevice, String filePath, int bufferID)
     {
      int nRet= -1;
      if (SDKUtilty.fileIsExists(filePath)){
           //读取一张指纹bmp图像，提取特征
           byte imgBuffer[] = new byte[256*360];
           SDKUtilty.ReadBmpToRaw(imgBuffer, filePath, true);//翻转
           nRet = AS60xIO.HS_DownImage(mUsbDevice, 0, imgBuffer, 256*360);
           if( 0==nRet )
           {
               Log.d(TAG, "HS_GenChar.Start.ID="+bufferID);
               AS60xIO.HS_GenChar(mUsbDevice, 0, bufferID);
           }else {
                    nRet = -2;
               }
      }
           return nRet;
      }
    
     /*弹出对话框*/
     private void alertShutdownDialog()
     {
         new AlertDialog.Builder(this)
           .setTitle("注意")
           .setIcon(android.R.drawable.ic_dialog_alert)
           .setMessage("是否退出此终端？：(")
           .setPositiveButton("确定", new DialogInterface.OnClickListener(){
               @Override
               public void onClick(DialogInterface dialogInterface, int i)
               {
                    dialogInterface.dismiss();
                    shutDownMainActivity();
               }
           }).setNegativeButton("取消", null).show();
     }
      /*-------彻底退出系统------------*/
     private void shutDownMainActivity() {
      Log.d(TAG, "shutDownMainActivity....");
           AS60xIO.FCV_CloseDevice(mUsbDevice);
           MainActivity.this.finish();
      System.exit(0);
      return;
      }
     
     @Override
     protected void onDestroy() {
          // 销毁的时候释放SoundPool资源
          if (pool != null) {
              pool.release();
              pool = null;
          }
          AS60xIO.FCV_CloseDevice(mUsbDevice);//关闭设备，释放资源
          super.onDestroy();
     }
     /*删除文件*/
    public boolean deletefile(File filename)
     {
        if (filename.isFile())
        {
          filename.delete();
          return true;
        }else {
              return false;
          }
    }
     /*将数据写入到sd卡中*/
     public void writeToSd(String filename, byte[] buf)
     {
          try {
              FileOutputStream fos = new FileOutputStream(filename);
              fos.write(buf);
              fos.close();
          } catch (FileNotFoundException e) {
              e.printStackTrace();
          } catch (IOException e) {
              e.printStackTrace();
          }
     }
     /*读取图片*/
     public static void readFromSd(String fileName, byte[] data)
     {
          try {
              FileInputStream stream = new FileInputStream(fileName);
              stream.read(data, 0, 256*360);//512
              stream.close();
          } catch (FileNotFoundException e) {
              e.printStackTrace();
          } catch (IOException e) {
              e.printStackTrace();
          }
     }
     /**
     * 执行一个shell命令，并返回字符串值
     * @param cmd
     * 命令名称&参数组成的数组（例如：{"/system/bin/cat", "/proc/version"}）
     * @param workdirectory
     * 命令执行路径（例如："system/bin/"）
     * @return 执行结果组成的字符串
     * @throws IOException
     */
     public static synchronized String runCmd(String[] cmd, String workdirectory) throws IOException {
          StringBuffer result = new StringBuffer();
          try
          {
              // 创建操作系统进程（也可以由Runtime.exec()启动）
              // Runtime runtime = Runtime.getRuntime();
              // Process proc = runtime.exec(cmd);
              // InputStream inputstream = proc.getInputStream();
              ProcessBuilder builder = new ProcessBuilder(cmd);
              InputStream in = null;
              // 设置一个路径（绝对路径了就不一定需要）
              if (workdirectory != null) {
                   // 设置工作目录（同上）
                   builder.directory(new File(workdirectory));
                   // 合并标准错误和标准输出
                   builder.redirectErrorStream(true);
                   // 启动一个新进程
                   Process process = builder.start();
                   // 读取进程标准输出流
                   in = process.getInputStream();
                   byte[] re = new byte[1024];
                   while (in.read(re) != -1)
                   {
                        result = result.append(new String(re));
                   }
              }
              // 关闭输入流
              if (in != null) {
                   in.close();
              }
          } catch (Exception ex) {
              ex.printStackTrace();
          }
          return result.toString();
     }

    
     /*测试内部接口*/
     private void testIntraface () {
           int HS_OK = 0;
           int nRet = -1;
           int nAddr = 0;
           int CHAR_BUFFER_A = 1;
           int CHAR_BUFFER_B = 2;
           int CHAR_BUFFER_C = 3;
           byte imgBuffer[]=new byte[256*360];
           
           ////////////////////////////////////////////////////////////
           //打开设备，UsbDevice表示已连接的USB设备， 包含了访问设备标识信息的方法、接口和挂在点。
           ////////////////////////////////////////////////////////////
           if ( !mSensorInited )
           {
               mUsbDevice = AS60xIO.FCV_OpenDevice(MainActivity.this, 0x2109, 0x7638);
               if (!mUsbManager.hasPermission(mUsbDevice))
           {
                    mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
           }
               AS60xIO.HS_Verfiy(mUsbDevice);
               ViewNote.setText("打开设备成功!");
           }
           
           ///////////////////////////////////////////////////////
           //采集一个指纹并上传图像
           //////////////////////////////////////////////////////
           ViewNote.setText("请将手指按到采集器上！");
           long startTime, endTime;
           startTime = System.currentTimeMillis();
           while(true){
               nRet = AS60xIO.HS_GetImage(mUsbDevice, nAddr);//采集指纹
               if(nRet == HS_OK)
               {
                    break ;
               }
               endTime=System.currentTimeMillis();
               if (700 <= (endTime-startTime))
               {
                    nRet = -1;
                    return;
               }
           }
      
           if (nRet == HS_OK)
           {
               nRet = -1;
               ViewNote.setText("正在上传指纹图像！");
               int[] imgLen = new int[1];
               nRet = AS60xIO.HS_UpImage(mUsbDevice, nAddr, imgBuffer, imgLen);
               //debug
               //AS60xDatas.SaveRawToBmp(imgBuffer, "/sdcard/ceshi_123.bmp", true);
               
           }else {
               return;
           }
           ///////////////////////////////////////////////////////////
           //提取特征并上传
           ///////////////////////////////////////////////////////////
           ViewNote.setText("正在提取特征！");
           nRet = AS60xIO.HS_GenChar(mUsbDevice, nAddr, CHAR_BUFFER_A);
           if(nRet != HS_OK)
               return ;
           
           ViewNote.setText("上传特征！");
           byte[] charBuffer=new byte[512];
           int[] CharLen = new int[1];
           Arrays.fill(charBuffer, (byte)0);
           nRet = AS60xIO.HS_UpChar(mUsbDevice, nAddr, CHAR_BUFFER_A, charBuffer, CharLen);
           if(nRet != HS_OK)
               return ;
           writeToSd("/sdcard/getchar.dat", charBuffer);
           //////////////////////////////////////////////////
           //下载指纹图像，提取特征值并上传
           /////////////////////////////////////////////////
           ViewNote.setText("下载指纹图像！");
           nRet = AS60xIO.HS_DownImage(mUsbDevice, nAddr, imgBuffer, 256*360);
           if(nRet != HS_OK ) return ;
           
           ViewNote.setText("正在提取特征！");
           nRet = AS60xIO.HS_GenChar(mUsbDevice, nAddr, CHAR_BUFFER_A);
           if(nRet != HS_OK) return ;
           ViewNote.setText("上传特征！");
           Arrays.fill(charBuffer, (byte)0);
           nRet = AS60xIO.HS_UpChar(mUsbDevice, nAddr, CHAR_BUFFER_A, charBuffer, CharLen);
           if(nRet != HS_OK) return ;
           writeToSd("/sdcard/regetchar.dat", charBuffer);
           ////////////////////////////////////////////////////
           //下载个特征值，返回比对结果。
           ///////////////////////////////////////////////////
           byte[] DownCharBuffer1=new byte[512];
           byte[] DownCharBuffer2=new byte[512];
           readFromSd("/sdcard/regetchar.dat", DownCharBuffer1);
           readFromSd("/sdcard/FingerChar1.dat", DownCharBuffer2);
           
           ViewNote.setText("下载特征A！");  
           nRet =AS60xIO.HS_DownChar(mUsbDevice, nAddr, CHAR_BUFFER_A, charBuffer, 512);//下载特征到特征缓冲区A
           if(nRet != HS_OK) return ;
           
           ViewNote.setText("下载特征B！");
           nRet = AS60xIO.HS_DownChar(mUsbDevice, nAddr, CHAR_BUFFER_B, DownCharBuffer1, 512);//下载特征到缓冲区B
           if(nRet != HS_OK) return ;
           
           ViewNote.setText("下载特征C！");
           nRet = AS60xIO.HS_DownChar(mUsbDevice, nAddr, CHAR_BUFFER_C, DownCharBuffer1, 512);//下载特征到缓冲区c
           if(nRet != HS_OK) return ;
           
           ViewNote.setText("正在比对！");
           int[]iScore= new int[2];//最小长度必须>=2,否则出错
           nRet = AS60xIO.HS_Match(mUsbDevice, nAddr, iScore);  //比对
           
           String Score1 = String.format("%2.1f%%", (iScore[0]*0.1));//分数
           String Score2 = String.format("%2.1f%%", (iScore[1]*0.1));//分数
           ViewNote.setText("比对分数: A-B:"+Score1+"A-C:"+Score2); //显示分数
           
           
           ///////////////////////////////////////
           //下载指纹模板，并与采集到的指纹比对
           //////////////////////////////////////////////
           //ViewNote.setText("请将手指按到采集器上！");
           startTime = System.currentTimeMillis();
           while(true){
               nRet = AS60xIO.HS_GetImage(mUsbDevice, nAddr);
               if(nRet == HS_OK)
                    break ;
               endTime=System.currentTimeMillis();
               if (700 <= (endTime-startTime))//超时自动退出
               {
                    nRet = -1;
                    break;
               }
           }
           if (nRet == HS_OK)
           {
               //ViewNote.setText("正在提取特征！");
               nRet = AS60xIO.HS_GenChar(mUsbDevice,nAddr,CHAR_BUFFER_A);
               if(nRet != HS_OK) return ;
               nRet = AS60xIO.HS_DownChar(mUsbDevice, nAddr, CHAR_BUFFER_B, DownCharBuffer1, 512);//下载特征到缓冲区B
               if(nRet != HS_OK) return ;
               //ViewNote.setText("正在比对！\r\n");
               int Score[] = new int[2];//最小长度必须>=2,否则出错
               nRet = AS60xIO.HS_Match(mUsbDevice, nAddr, Score);//比对
               String ShowScore = String.format("%2.1f%%", (Score[0]*0.1));//分数
               ViewNote.setText("【比对分数】 "+ShowScore); //显示分数
           }else {
               return;
           }
      }
    
    
     /*压缩转换
      * */
    private void changeByte(byte[] source, byte dest[]){
        
         int tmp = 0;
         for(int j = 0; j<256*360; j+=8)
         {
              tmp = 0;
              for(int i=0; i<8; i++)
              {
                  tmp<<=1;
                  if (source[i+j]==0)
                  {
                       tmp += 1;
                  }
              }
              dest[j/8]=(byte)tmp;
         }
         System.arraycopy(source, 256*360, dest, 256*360/8, 512);
    }
  
    
    
    /**
     * Bit转Byte
     */ 
    public static byte BitToByte(String byteStr) { 
        int re, len; 
        if (null == byteStr) { 
            return 0; 
        } 
        len = byteStr.length(); 
        if (len != 4 && len != 8) { 
            return 0; 
        } 
        if (len == 8) {// 8 bit处理 
            if (byteStr.charAt(0) == '0') {// 正数 
                re = Integer.parseInt(byteStr, 2); 
            } else {// 负数
                re = Integer.parseInt(byteStr, 2) - 256; 
            } 
        } else {//4 bit处理 
            re = Integer.parseInt(byteStr, 2); 
        } 
        return (byte) re; 
    } 
    




	

}
