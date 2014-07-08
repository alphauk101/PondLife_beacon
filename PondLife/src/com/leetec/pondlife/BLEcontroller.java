package com.leetec.pondlife;

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
	
	
	
	public interface beaconFound
	{
		void onBeaconFound(byte[] data);
	}
	private beaconFound mBeaconFound;
	
	public void setBeaconFound(beaconFound bcf)
	{
		mBeaconFound = bcf;
	}
	
	
	public BLEcontroller(Context con, BluetoothAdapter bleAd)
	{
		this.mBLEAdpater = bleAd;
	}
	
	public void startScanning()
	{
		Log.i(MainActivity.TAG,"++++++++++++ SCAN STARTED ++++++++++++");
		this.mLeScanCallBack = new BluetoothAdapter.LeScanCallback() 
		{
			@Override
			public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
			{
				String devName = device.getName();
				if(devName.equals(mDeviceName))
				{
					Log.i(MainActivity.TAG,"++++++++++++ DEVICE FOUND ++++++++++++");
					mBLEAdpater.stopLeScan(mLeScanCallBack);
					//We have our device and we have stopped the scan
					returnAdvData(scanRecord);//Send the data.
				}
				Log.i(MainActivity.TAG,"++++++++++++ DEVICE ++++++++++++");
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
	
	public void stopScanning()
	{
		if(mBLEAdpater != null)
		{
			Log.i(MainActivity.TAG,"++++++++++++ SCAN STOPPED ++++++++++++");
			mBLEAdpater.stopLeScan(mLeScanCallBack);//Stop the scan for bat reasons.
		}
	}
	
	public void returnAdvData(byte[] advData)
	{
		mBeaconFound.onBeaconFound(advData);	//Send the data
	}


}

