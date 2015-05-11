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


public class MyActivity extends Activity {

    public static final String TAG = "PL";

    private static TextView txtEnable;
    private static TextView txtTemperature;
    private static TextView txtStatus;
    private static TextView txtLastDate;

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
    public static final String statusScanExtra = "stsScan";

    private static final String BT_notEnabled = "Bluetooth off";
    private static final String ReadyToStart = "Touch to begin";
    private static final String CurrentlyScanning = "Monitoring...";
    private static final String NotCurrentlyScanning = "Waiting..";

    private static final BroadcastReceiver mBroadcaster = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"Broadcaster got message");
            String Action = intent.getAction();
            if(Action.equals(bluetoothNotEnabled))
            {
                txtEnable.setText(BT_notEnabled);
            }else if(Action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                if(state == BluetoothAdapter.STATE_ON)
                {
                    txtEnable.setText(ReadyToStart);
                }
            }else if(Action.equals(statusOfScan))
            {
                if(intent.getBooleanExtra(statusScanExtra,false))
                {
                    txtEnable.setText(CurrentlyScanning);
                }else{
                    txtEnable.setText(NotCurrentlyScanning);
                }
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

        mIntents.addAction(bluetoothNotEnabled);
        mIntents.addAction(startService);
        mIntents.addAction(statusOfScan);
        mIntents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcaster, mIntents);


        startService();//Once weve initialised the app we need to start the service as it does everything

        LinearLayout mGoTap = (LinearLayout)findViewById(R.id.lyo_enabled);
        mGoTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //We need to start or stop the scan depending on the current state
                Intent mStartScan = new Intent();
                mStartScan.setAction(BR_StartScan);
                sendBroadcast(mStartScan);
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
