package com.lee.pondlife;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

	private TextView statusText;
	public final static int NOTIFICATION_ID = 1289;
	public final static int PL_SERVICE_ID = 127845;
	public final static String TAG = "PONDLIFE";
	public final static String IFILTER = "PLSERVICE";
	
	private LocalBroadcastManager mBroadcastReceiverManager;
	private BroadcastReceiver mBroadcastReceiver;
	
	private AlarmManager mAlarmManager;
	static int SERVICE_PERIOD = 3 * (1000*60);//
	PendingIntent mPndInt;
	
    @SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.start)
                .setContentTitle(getString(R.string.Notif_Title))
                .setContentText("Starting Pond Life");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        
        
        //Initialize broadcast receiver
        initReciever();
        
      //Start Service
        Intent serviceInt = new Intent(this,PLservice.class);
        serviceInt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startService(serviceInt);
       
        mPndInt = PendingIntent.getService(getApplicationContext(), PL_SERVICE_ID, serviceInt, PendingIntent.FLAG_CANCEL_CURRENT);
        //Create our alarm manager
        //This now takes a reading every 1 min.
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SERVICE_PERIOD, mPndInt);

        
        
    }

    
    private void initReciever()
    {
    	mBroadcastReceiverManager = LocalBroadcastManager.getInstance(getApplicationContext());
    	mBroadcastReceiver = new BroadcastReceiver()
    	{
			
			@Override
			public void onReceive(Context context, Intent intent)
			{
				//We need to pick the codes out of the intents here.
				switch (intent.getFlags()) {
				case PLservice.BEACON_OK:
					Toast.makeText(getApplicationContext(), "FROM DA APP", 2000).show();
					break;
					
				default:
					break;
				}
			}
		};
		IntentFilter IFil = new IntentFilter();
		IFil.addAction(IFILTER);
		mBroadcastReceiverManager.registerReceiver(mBroadcastReceiver, IFil);
    }

    
    @Override
	protected void onStop() {
		
    	//Make sure we do this here.
    	mBroadcastReceiverManager.unregisterReceiver(mBroadcastReceiver);
    	super.onStop();
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.disableService) {
        	mAlarmManager.cancel(mPndInt);
           }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
