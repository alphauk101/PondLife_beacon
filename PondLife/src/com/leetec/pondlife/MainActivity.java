package com.leetec.pondlife;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class MainActivity extends Activity {

	public final static int NOTIFICATION_ID = 1289;
	public final static int PL_SERVICE_ID = 127845;
	public final static String TAG = "PONDLIFE";
	public final static String IFILTER = "PLSERVICE";
	private LocalBroadcastManager mBroadcastReceiverManager;
	private BroadcastReceiver mBroadcastReceiver;
	private sPrefs myPrefs;
	private TextView mEdtReportText;
	private TextView mTextBatt;
	private TextView mTextTemp;
	private Switch mSwitch;
	PendingIntent mPndInt;
	private NotificationManager mNotificationManager = null;
	private ImageView mSignalImg;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myPrefs = new sPrefs(this);
        mEdtReportText = (TextView)findViewById(R.id.edt_report);
        setReportText("Starting");
        
        mSignalImg = (ImageView)findViewById(R.id.img_signal);
        mTextBatt = (TextView)findViewById(R.id.txt_batlvl);
        mTextTemp = (TextView)findViewById(R.id.edt_temp);
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        //Create our notification
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.start)
        .setContentTitle(getString(R.string.Notif_Title))
        .setContentText("Starting Pond Life");
        mBuilder.setContentIntent(pendingIntent);
       
		new Intent(this, MainActivity.class);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		// mId allows you to update the notification later on.
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		
		
        //Initialize broadcast receiver
        initReciever();
        
        mSwitch = (Switch)findViewById(R.id.sw_enable);
        if(getServiceState())
        {
        	mSwitch.setChecked(true);
        	//Should probably rerun the service incase
        	startService();
        	setReportText("Service is running");
        }else
        {
        	mSwitch.setChecked(false);
        	setReportText("Ready to start");
        }
        
        
        mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
				{
					startService();
				}else
				{
					//At this point we should probably destory the notification as we want the service to appear as if its died
					mNotificationManager.cancel(NOTIFICATION_ID);
					setReportText("Switch to enable service");
				}
				setServiceState(isChecked);//Make sure we save our current button selection.
			}
		});
        
    }
    
    
    private void setReportText(String msg)
    {
    	mEdtReportText.setText(msg);
    }
    
    private boolean getServiceState()
    {
    	boolean res = myPrefs.getServiceState();
    	return res;
    }
    
    private void setServiceState(boolean state)
    {
    	myPrefs.setServiceState(state);
    }
    
    private void startService()
    {
    	//Start Service
        Intent serviceInt = new Intent(this,PLservice.class);
        startService(serviceInt);
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
					//Received a A OK from the service.
					setReportText("Pond level: OK");
					
					break;
				case PLservice.BEACON_BAD:
					setReportText("POND LEVEL LOW !!!!!");
					break;
				case PLservice.BT_NOT_ENABLED:
					setReportText("Bluetooth needs to be enabled");
					mSwitch.setChecked(getServiceState());
					break;
				case PLservice.START_SERVICE:
					setReportText("Background Service Started");
					break;
				case PLservice.BATTERY_LEVEL:
						setBatteryText(intent.getIntExtra(PLservice.BATTERY_LVL_REPORT, 0));
					break;
				case PLservice.TEMP_LEVEL:
						setTempText(intent.getFloatExtra(PLservice.TEMP_LEVEL_REPORT, (float) 0.0));
					break;
				case PLservice.SIGNAL_LEVEL:
						setSignalStrength(intent.getIntExtra(PLservice.SIGNAL_LEVEL_REPORT, 0));
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

    
    private void setSignalStrength(int rssi)
    {
    	if(rssi != 0)
    	{
    		Log.i("PONDLIFE", ">>>>>>>>>> RSSI: " + rssi);
    		//Should be something sensible
    		if(rssi > -50)
    		{
    			mSignalImg.setImageResource(R.drawable.siggood);
    		}else if (rssi > -90)
    		{
    			mSignalImg.setImageResource(R.drawable.sigok);
    		}else
    		{
    			mSignalImg.setImageResource(R.drawable.sigbad);
    		}
    	}else
    	{
    		mSignalImg.setImageResource(R.drawable.signo);
    	}
    }
    
    private void setBatteryText(int level)
    {
    	String mTmpBatt = String.valueOf(level) + "%";
    	mTextBatt.setText(mTmpBatt);
    }
    
    private void setTempText(float temp)
    {
    	mTextTemp.setText(String.valueOf(temp) + (char)0xB0 + "C");
    }
    
    
    @Override
	protected void onStop() {
		
    	//Make sure we do this here.
    	//We also want to remove the notification
    	if (mNotificationManager != null)
    	{
    		mNotificationManager.cancel(NOTIFICATION_ID);//So it isnt there after we go.
    	}
    	mBroadcastReceiverManager.unregisterReceiver(mBroadcastReceiver);
    	super.onStop();
	}

}
