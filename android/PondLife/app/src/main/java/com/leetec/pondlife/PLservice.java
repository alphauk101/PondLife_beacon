package com.leetec.pondlife;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Lee_Laptop on 11/05/2015.
 */
public class PLservice extends Service
{
    public PLservice() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    IntentFilter mIntentFilter;
    BroadcastReceiver mServiceBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(MyActivity.TAG,"Service got a message: " + action);
            if(action == MyActivity.BR_StartScan)
            {
                startScan();
            }else if(action == MyActivity.BR_StopScan)
            {

            }else if (action == MyActivity.getScanStatus)
            {
                Intent Result = new Intent();
                Result.setAction(MyActivity.statusOfScan);
                Boolean resultFlag;
                if(mCurrentState == ScanState.RUNNING)
                {
                    resultFlag = true;
                }else{
                    resultFlag = false;
                }
                Result.putExtra(MyActivity.statusScanExtra,resultFlag);
                sendBroadcast(Result);

            }
        }
    };


    enum ScanState
    {
        RUNNING,
        NOT_RUNNING
    }
    private BLEcontroller mBLEController;

    private BluetoothAdapter mBluetoothAdapter;
    private static ScanState mCurrentState = ScanState.NOT_RUNNING;


    @Override
    public void onCreate() {

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(MyActivity.BR_StartScan);
        mIntentFilter.addAction(MyActivity.BR_StopScan);
        mIntentFilter.addAction(MyActivity.getScanStatus);
        registerReceiver(mServiceBroadcastReciever,mIntentFilter);


        Intent infoIntent = new Intent();
        infoIntent.setAction(MyActivity.startService);
        sendBroadcast(infoIntent);

        if(checkBluetoothAdapter())
        {
            //We are ok to continue
            //We will report our status to the app
            Intent mInfo = new Intent();
            mInfo.setAction(MyActivity.getScanStatus);
            sendBroadcast(mInfo);//We are calling our own Brd rec but it will do all the work for us
        }else{
            //Our BT adapter is switched off
            Intent nBT = new Intent();
            nBT.setAction(MyActivity.bluetoothNotEnabled);
            sendBroadcast(nBT);
        }
        super.onCreate();
    }

    /***
     * Starts the BLE scann for the beacon
     * @return if false is returned assume something went wrong.
     */
    public boolean startScan()
    {
        if(mBluetoothAdapter != null)
        {
            mBLEController = new BLEcontroller(getApplication(),mBluetoothAdapter);
            mBLEController.setBeaconFound(new BLEcontroller.beaconFound() {
                @Override
                public void onBeaconFound(byte[] data) {

                }
            });
            mBLEController.startScanning();
            mCurrentState = ScanState.RUNNING;
            //We will report our status to the app
            Intent mInfo = new Intent();
            mInfo.setAction(MyActivity.getScanStatus);
            sendBroadcast(mInfo);//We are calling our own Brd rec but it will do all the work for us
            return true;
        }else{
            return false;
        }

    }

    private boolean checkBluetoothAdapter()
    {
        //My code.
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if ((mBluetoothAdapter == null) || (mBluetoothAdapter.isEnabled() == false))
        {
           return false;//Tell the main activity bluetooth is not enabled.
        }else
        {
            return true;
        }
    }

    public boolean isScanRunning()
    {
        if(mCurrentState == ScanState.RUNNING)
        {
            return true;
        }else
        return false;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(MyActivity.TAG,"!!!!!!!!!!!! Service destroyed");
        unregisterReceiver(mServiceBroadcastReciever);
    }

}
