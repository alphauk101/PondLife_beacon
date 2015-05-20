package com.leetec.pondlife;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Lee_Laptop on 11/05/2015.
 */
public class PLservice extends Service
{

    private NotificationManager mNotificationManager = null;
    private final int NOTIFICATION_ID = 0;
    private static int SCAN_TIMEOUT = 5000;
    private static boolean USER_REQUESTED_MODE = false; //This represents what the user has requested
    //we need to keep track if they've stopped the scan then we need to make sure we do

    private static int LONG_WAIT = ((1000*60)*2);
    //private static int SHORT_WAIT = ((1000*60)*1);
    private static int SHORT_WAIT = 10000;

    private static int WAIT_WHILE_BT_OFF = ((1000*60)*30);//we wait 30 mins before trying again

   private static int mBeaconCount = 0;

    public PLservice() {

    }

    Handler serviceHandler = new Handler(){


    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    IntentFilter mIntentFilter;
    BroadcastReceiver mServiceBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(MyActivity.TAG,"Service Broadcaster got a message: " + action);

            if(action == MyActivity.BR_StartScan)
            {

                updateNotification("Pondlife","Silent running...");//Only update this if the user starts the scan

                USER_REQUESTED_MODE=true;
                if(startScan())
                {
                    Intent mInfo = new Intent();
                    mInfo.setAction(MyActivity.getScanStatus);
                    sendBroadcast(mInfo);//We are calling our own Brd rec but it will do all the work for us
                }else{
                    checkBluetoothAdapter();//The fact the scan has failed to start is probably due to the bt adapter not working properly.
                }

            }else if(action == MyActivity.BR_StopScan)
            {
                updateNotification("Pondlife","Scan stopped.");//Only update this if the user starts the scan
                USER_REQUESTED_MODE = false;
                stopScan();

                Intent mInfo = new Intent();
                mInfo.setAction(MyActivity.getScanStatus);
                sendBroadcast(mInfo);//We are calling our own Brd rec but it will do all the work for us
            }else if (action == MyActivity.getScanStatus)
            {
                Intent Result = new Intent();
                Result.setAction(MyActivity.statusOfScan);
                Boolean resultFlag;
                if(USER_REQUESTED_MODE)
                {
                    resultFlag = true;
                }else{
                    resultFlag = false;
                }
                Result.putExtra(MyActivity.statusScanExtra,resultFlag);
                sendBroadcast(Result);
            }else if(action.equals(MyActivity.checkBTstate))
            {
                //The activity has requested we check the state of the bt
                checkBluetoothAdapter();
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
        mIntentFilter.addAction(MyActivity.checkBTstate);
        registerReceiver(mServiceBroadcastReciever,mIntentFilter);

        Intent infoIntent = new Intent();
        infoIntent.setAction(MyActivity.startService);
        sendBroadcast(infoIntent);

        checkBluetoothAdapter();


        Notification notification = new Notification.Builder(this)
                .setContentTitle("Pond Life")
                .setContentText("Service Started").setSmallIcon(R.drawable.start)
                .build();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, notification);

        super.onCreate();
    }


    /***
     * Starts the BLE scann for the beacon
     * @return if false is returned assume something went wrong.
     */
    public boolean startScan()
    {
        if( (mBluetoothAdapter != null) && (mBluetoothAdapter.isEnabled()) )
        {
            updateNotification("PondLife","Silent scanning.");

            if(mCurrentState != ScanState.RUNNING) {
                if(mBLEController == null) {
                    mBLEController = new BLEcontroller(getApplication(), mBluetoothAdapter);
                    mBLEController.setBeaconFound(new BLEcontroller.beaconFound() {
                        @Override
                        public void onBeaconFound(byte[] data) {
                            processBeacon(data);
                        }
                    });
                }
                Log.i(MyActivity.TAG,">>>>>>>>>>>>>>>>>>> Scan started");
                mBLEController.startScanning();
                mCurrentState = ScanState.RUNNING;
                //We will report our status to the app
                Intent mInfo = new Intent();
                mInfo.setAction(MyActivity.getScanStatus);
                sendBroadcast(mInfo);//We are calling our own Brd rec but it will do all the work for us

                mBeaconCount=0;//reset this.
                //We need set to set a timer to stop the scan because if it carries on indefinitly then its wrong

                //We need to make sure we set up a stop timer
                serviceHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopScan();
                    }
                },SCAN_TIMEOUT);

                return true;
            }else{
                //We are already running
                return false;
            }
        }else{
            Log.i(MyActivity.TAG,"!!!!!!!!!!!!!!! Unable to use BT adapter so a scan has not been started!!!!!!!!!!!!!!!!!!!");
            checkBluetoothAdapter();
            updateNotification("PondLife","Bluetooth not enabled not scanning!");
            mCurrentState = ScanState.NOT_RUNNING;//Make sure we know were not running

            //We need to make sure we set up a stop timer but now it can wait extra time
            serviceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            },WAIT_WHILE_BT_OFF);

            return false;
        }

    }

    /***
     * Checks the BT adapter state and reports it to the calling app
     */
    private void checkBluetoothAdapter()
    {
        Intent mIntent = new Intent();
        mIntent.setAction(MyActivity.BTstateResult);
        Boolean result;
        // Initializes Bluetooth adapter.
        if(mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            if ((mBluetoothAdapter == null) || (mBluetoothAdapter.isEnabled() == false)) {
                result = false;
                //Tell the main activity bluetooth is not enabled.
            } else {
                result = true;
            }
        }else {
            result = true;//we can assume its alrewady init.
        }

        mIntent.putExtra(MyActivity.BTStatusExtra,result);
        sendBroadcast(mIntent);
    }

    public boolean isScanRunning()
    {
        if(mCurrentState == ScanState.RUNNING)
        {
            return true;
        }else
        return false;
    }


    private void stopScan()
    {
        Log.i(MyActivity.TAG,">>>>>>>>>>>>>>>>>>> Scan stopped");
        if(mCurrentState == ScanState.RUNNING)
        {
            mBLEController.stopScanning();
            mCurrentState = ScanState.NOT_RUNNING;
        }
        if(USER_REQUESTED_MODE)//We are still requested by the activity to carry on scanning
        {

            Log.i(MyActivity.TAG,"Resetting for next scan.");

            //We have still got to carry on running because the user has not stopped us

            int timer=0;

            //First we must decide at what time periods to wait
            if(mBeaconCount>0)
            {
                //We have one so we could probably wait 20 mins before trying again
                Log.i(MyActivity.TAG,"Resetting to scan in: " + String.valueOf(LONG_WAIT) + " M Seconds");
                timer = LONG_WAIT;
            }else{
                Log.i(MyActivity.TAG,"Resetting to scan in: " + String.valueOf(SHORT_WAIT) + " M Seconds");
                timer = SHORT_WAIT;
            }

            serviceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startScan();
                }
            },timer);
        }else{
            removeNotification();
            Log.i(MyActivity.TAG,"User has requested a stop scan.");
        }
    }

    void removeNotification()
    {
        if(mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }
    }

    void updateNotification(String title, String msg)
    {
        if(mNotificationManager != null) {

            Intent resultIntent = new Intent(this, MyActivity.class);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(this,0,resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(msg).setSmallIcon(R.drawable.start)
                    .setContentIntent(resultPendingIntent)
                    .build();
            // mId allows you to update the notification later on.
            mNotificationManager.notify(NOTIFICATION_ID, notification);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(MyActivity.TAG,"!!!!!!!!!!!! Service destroyed");
        unregisterReceiver(mServiceBroadcastReciever);
    }

    /***
     * This process the information to deliver back tot he main activity but
     * also this must determine how regularly to rescan.
     * @param data
     */
    private void processBeacon(byte[] data)
    {
        mBeaconCount++;
        //If weve found a beacon then the scan has stopped
        stopScan();
        mCurrentState=ScanState.NOT_RUNNING;

    }

}
