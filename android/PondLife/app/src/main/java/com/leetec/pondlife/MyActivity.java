package com.leetec.pondlife;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;


public class MyActivity extends Activity {

    public static final String TAG = "PL";

    private static TextView txtEnable;
    private static TextView txtTemperature;
    private static TextView txtStatus;
    private static TextView txtLastDate;
    private static boolean CURRENT_STATE = false;

    /***
     * This is our service broadcast catcher
     */
    private static IntentFilter mIntents = new IntentFilter();
    public static final String BR_StartScan = "com.leetec.startScan";
    public static final String BR_StopScan = "com.leetec.stopScan";
    public static final String startService = "com.leetec.StartService";
    public static final String bluetoothNotEnabled = "com.leetec.BluetoothOff";
    public static final String getScanStatus = "com.leetec.getScanStatus";
    public static final String statusOfScan = "com.leetec.statusScan";
    public static final String checkBTstate = "com.leetec.BTStatus";
    public static final String BTstateResult = "com.leetec.BTstatusReport";
    public static final String BeaconDetails = "com.leetec.BeaconResults";

    public static final String statusScanExtra = "stsScan";
    public static final String BTStatusExtra = "btScan";
    public static final String BeaconResultsExtraTemperature = "beaconTemp";
    public static final String BeaconResultsExtraStatus = "beaconStatus";

    private static final String BT_notEnabled = "Bluetooth off";
    private static final String ReadyToStart = "Touch to begin";
    private static final String CurrentlyScanning = "Monitoring...";
    private static final String NotCurrentlyScanning = "Waiting.";

    private BroadcastReceiver mBroadcaster = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String Action = intent.getAction();
            Log.i(TAG,"Activity Broadcaster got message: " + Action);
            if(Action.equals(BTstateResult))
            {
                if(! intent.getBooleanExtra(BTStatusExtra,false))
                {
                    Log.i(TAG,"BT not enabled");
                    txtEnable.setText(BT_notEnabled);
                }
            }else if(Action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                Log.i(TAG,"Status changed");
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                if(state == BluetoothAdapter.STATE_ON)
                {
                    //The bluetooth adapter should be on so lets make a request to check it.
                    Log.i(TAG, "BT not enabled");
                    txtEnable.setText("Bluetooth switched on");
                }

            }else if(Action.equals(statusOfScan))
            {
                Log.i(TAG,"Scan Status changed");
                if(intent.getBooleanExtra(statusScanExtra,false))
                {
                    CURRENT_STATE = true; //We are scanning
                    txtEnable.setText(CurrentlyScanning);
                }else{
                    CURRENT_STATE = false;//We are not scanning
                    txtEnable.setText(NotCurrentlyScanning);

                }
            }else if (Action.equals(startService))
            {
                //Our service has just been created
                txtEnable.setText(NotCurrentlyScanning);
            }else if (Action.equals(BeaconDetails))
            {
                String temperatue = String.valueOf(intent.getFloatExtra(BeaconResultsExtraTemperature,(float)0.0));
                txtTemperature.setText(temperatue+"°C");

                if(intent.getByteExtra(BeaconResultsExtraStatus,(byte)0x00) == (byte)0xAA)
                {
                    txtStatus.setText("Level OK");
                }else{
                    txtStatus.setText("Level LOW!!");
                }

                Calendar c = Calendar.getInstance();
                String date;
                date = c.get(Calendar.DAY_OF_MONTH)+"/"+c.get(Calendar.MONTH)+"/"+c.get(Calendar.YEAR)
                        +" "+c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
                txtLastDate.setText(date);

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        txtEnable = (TextView)findViewById(R.id.txt_Waiting);
        txtTemperature = (TextView)findViewById(R.id.txt_temperature);
        txtStatus = (TextView)findViewById(R.id.txt_status);
        txtLastDate = (TextView)findViewById(R.id.txt_lasthit);

        mIntents.addAction(BeaconDetails);
        mIntents.addAction(bluetoothNotEnabled);
        mIntents.addAction(startService);
        mIntents.addAction(statusOfScan);
        mIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mIntents.addAction(BTstateResult);
        registerReceiver(mBroadcaster, mIntents);


        startService();//Once weve initialised the app we need to start the service as it does everything

        Intent mGetStatus = new Intent();
        mGetStatus.setAction(getScanStatus);
        sendBroadcast(mGetStatus);

        LinearLayout mGoTap = (LinearLayout)findViewById(R.id.lyo_enabled);
        mGoTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mScanMode = new Intent();
                //We need to start or stop the scan depending on the current state
                if(! CURRENT_STATE) {//We are not scanning
                    mScanMode.setAction(BR_StartScan);
                }else{
                    //We are scanning so best to stop the scan
                    mScanMode.setAction(BR_StopScan);
                }

                sendBroadcast(mScanMode);
            }
        });

    }

    private void startService()
    {
        //Start Service
        Intent serviceInt = new Intent(this,PLservice.class);

        startService(serviceInt);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcaster);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
