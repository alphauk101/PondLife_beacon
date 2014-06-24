package com.lee.pondlife;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;

public class PLservice extends Service
{
	private Handler plServiceHandler;
	private final int REQUEST_ENABLE_BT = 12854;
	private final int START_SERVICE = 123455;
	private final int BLUETOOTH_SCAN_FAILED = 54325;
	private BLEcontroller BLECont;
	private BluetoothAdapter mBluetoothAdapter;
	
	@Override
	public void onCreate() 
	{
		HandlerThread mHThread = new HandlerThread("ServiceStartArguments",Process.THREAD_PRIORITY_BACKGROUND );

		
		plServiceHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
				case START_SERVICE:
					
					break;

				default:
					break;
				}				
			}
		};
		
		super.onCreate();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2) @SuppressLint("NewApi") 
	private void startScan()
	{
		 
        //My code.
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
		 // Ensures Bluetooth is available on the device and it is enabled. If not,
		 // displays a dialog requesting user permission to enable Bluetooth.
		 if ((mBluetoothAdapter == null) || (mBluetoothAdapter.isEnabled() == false)) 
		 {
		     //BLUETOOTH NOT ENABLED
		 }else
		 {
			 mBluetoothAdapter = bluetoothManager.getAdapter();
			 BLECont = new BLEcontroller(getApplicationContext(), mBluetoothAdapter);
		 }
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		plServiceHandler.sendEmptyMessage(START_SERVICE);
		return START_STICKY;
	}



	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
