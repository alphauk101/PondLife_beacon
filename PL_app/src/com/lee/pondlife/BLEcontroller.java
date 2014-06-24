package com.lee.pondlife;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

@SuppressLint("NewApi") public class BLEcontroller
{
	private BluetoothAdapter mBLEAdpater;
	private BluetoothAdapter.LeScanCallback mLeScanCallBack;
	private String str_pl_UUID = "3b6617a0-fbac-11e3-a3ac-0800200c9a66";
	private String mDeviceName = "PondLife";
	

	public BLEcontroller(Context con, BluetoothAdapter bleAd)
	{
		this.mBLEAdpater = bleAd;
	}
	
	public void startScanning()
	{
		this.mLeScanCallBack = new BluetoothAdapter.LeScanCallback() 
		{
			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
			{
				if(device.getName().equals(mDeviceName));
				{
					mBLEAdpater.stopLeScan(this);
				}
			}
		};
		try{
			//UUID mplU = UUID.fromString(str_pl_UUID);
			//UUID[] listU = {mplU};
			mBLEAdpater.startLeScan(mLeScanCallBack);
		}catch(Exception e)
		{
			Log.i("Pondlife",e.getMessage());
		}

	}


}

