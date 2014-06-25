package com.lee.pondlife;

import com.lee.pondlife.MainActivity.PlaceholderFragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class PLservice extends Service
{

	private Handler plServiceHandler;
	private final int REQUEST_ENABLE_BT = 12854;
	private final int BLUETOOTH_NOT_ENABLED = 986532;
	private final int START_SERVICE = 123455;
	private final int BLUETOOTH_SCAN_FAILED = 54325;
	
	//Reports 
	final static int BEACON_OK = 546454;
	private final int BEACON_BAD = 654651;
	private final int BEACON_NOT_FOUND = 75384;
	private final int SERVICE_STOPPED = 578784;
	private final int BT_NOT_ENABLED = 45389;
	
	private NotificationManager mNotificationManager;
	private LocalBroadcastManager mBroadcastReceiverManager;
	private BroadcastReceiver mBroadcastReceiver;
	private Intent mInfoIntent;
	private AlarmManager mAlarmManager;
	private static BLEcontroller BLECont;
	private static BluetoothAdapter mBluetoothAdapter;
	private int SCANTIME = 20000;//20 secs atm

	@Override
	public void onCreate() 
	{
		
		//We have to assume we have been luanched from a alarm or other activity so 
		//Do our thing and then go to sleep 
		HandlerThread mHThread = new HandlerThread("ServiceStartArguments",Process.THREAD_PRIORITY_BACKGROUND );
		
		final Intent serviceInt = new Intent(this,PLservice.class);
        serviceInt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		plServiceHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
				case START_SERVICE:

			        PendingIntent mPndInt = PendingIntent.getService(getApplicationContext(), MainActivity.PL_SERVICE_ID, serviceInt, PendingIntent.FLAG_CANCEL_CURRENT);
			        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, MainActivity.SERVICE_PERIOD, mPndInt);
			        
					Log.i("PL","////////////////////////////////////////////////////// PL SERVICE STARTED ////////////////////////////////////////////");
					//The service has been started so 
					startScan();
					break;
				case BLUETOOTH_NOT_ENABLED:
					//We need to report this then kill our self.
					killSelf();
					break;
				default:
					break;
				}				
			}
		};
		plServiceHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() 
			{
				//This is due diligence to make sure come what may the scan is stopped after a period of time.
				if(BLECont != null)
				{
					BLECont.stopScanning();
					//If we are here the beacon has not been found.
					Log.i("PL","////////////////////////////////////////////////////// PL TIMED OUT ////////////////////////////////////////////");
					reportActivity(BEACON_NOT_FOUND);
				}
			}
		}, SCANTIME);
		
		
		//Setup notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		//As we only be sending messages here its ok to not register a reciever.
		mBroadcastReceiverManager = LocalBroadcastManager.getInstance(getApplicationContext());
		
		
		super.onCreate();
	}

	
	private void reportActivity(int code)
	{
		//We need to use notifications and a broadcaster here
		mInfoIntent = new Intent();
		mInfoIntent.setAction(MainActivity.IFILTER);
		
		switch (code) {
		case BEACON_OK:
			Log.i("PL","////////////////////////////////////////////////////// PL BEACON FOUND ////////////////////////////////////////////");
			//Weve seen a beacon and its a good one so report it
			mInfoIntent.setFlags(BEACON_OK);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			//Weve done a local braodcast of this event now update the notification
			sendNotification("Pond Level OK", R.drawable.good);
			killSelf();
			break;
		case BEACON_NOT_FOUND:
			mInfoIntent.setFlags(BEACON_NOT_FOUND);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			sendNotification("Pond Life out of range", R.drawable.start);//Start is our amber fish.
			killSelf();
			break;
		default:
			break;
		}
		
	}
	
	private void sendNotification(String message,int draw)
	{
		 NotificationCompat.Builder nmb = new NotificationCompat.Builder(getApplicationContext())
		 .setContentTitle(getString(R.string.Notif_Title))
		 .setContentText(message)
		 .setSmallIcon(draw);
		 mNotificationManager.notify(MainActivity.NOTIFICATION_ID, nmb.build());//updates this apps notif
	}
	
	//This is important so we can do any cleaning up
	private void killSelf()
	{
		this.stopSelf();
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
		     plServiceHandler.sendEmptyMessage(BLUETOOTH_NOT_ENABLED);//Tell the main activity bluetooth is not enabled.
		 }else
		 {
			 mBluetoothAdapter = bluetoothManager.getAdapter();
			 BLECont = new BLEcontroller(getApplicationContext(), mBluetoothAdapter);
			 BLECont.startScanning();//We have started our scan.
			 createDataListener();//This will now attach the data listener from BLE class
		 }
	}
	
	public void createDataListener()
	{
		if(BLECont != null)
		{
			BLECont.setBeaconFound(new BLEcontroller.beaconFound() {
				
				@Override
				public void onBeaconFound(byte[] data) 
				{
					reportActivity(BEACON_OK);//This will be changed
				}
			});
		}
	}
	


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		plServiceHandler.sendEmptyMessage(START_SERVICE);
		return super.onStartCommand(intent, flags, startId);
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
