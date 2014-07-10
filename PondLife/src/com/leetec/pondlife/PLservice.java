package com.leetec.pondlife;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class PLservice extends IntentService
{

	public PLservice() {
		super("PLService");
	}


	private Handler plServiceHandler;
	private final int REQUEST_ENABLE_BT = 12854;
	private final int BLUETOOTH_NOT_ENABLED = 986532;
	static final int START_SERVICE = 123455;
	private final int BLUETOOTH_SCAN_FAILED = 54325;
	//static int SERVICE_PERIOD = 5 * (1000*60);//
	static int SERVICE_PERIOD = 5000;//
	//Reports 
	final static int BEACON_OK = 546454;
	final static int BEACON_BAD = 654651;
	private final int BEACON_NOT_FOUND = 75384;
	private final int SERVICE_STOPPED = 578784;
	final static int BT_NOT_ENABLED = 45389;
	static final int BATTERY_LEVEL = 656659;
	static final String BATTERY_LVL_REPORT = "battlvl";
	
	private static byte SERVICEUUID_LSB = (byte)0x16;
	private static byte SERVICEUUID_MSB = (byte)0x9A;
	private static String BEACON_NAME = "PondLife";
	
	private NotificationManager mNotificationManager;
	private LocalBroadcastManager mBroadcastReceiverManager;
	private BroadcastReceiver mBroadcastReceiver;
	private Intent mInfoIntent;
	private AlarmManager mAlarmManager;
	private BLEcontroller BLECont;
	private BluetoothAdapter mBluetoothAdapter;
	private int SCANTIME = 5000;//5 secs atm
	static PendingIntent mPndInt;
	private boolean IGNORE_TIMEOUT = false;
	private sPrefs mSprefs;
	
	enum BeaconResult
	{
		Not_A_Beacon,
		Beacon_OK,
		Beacon_LevelLow;
	};
	
	@Override
	public void onCreate() 
	{
		mSprefs = new sPrefs(getApplicationContext());
		
		plServiceHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
				case START_SERVICE:
					IGNORE_TIMEOUT = false;//Set this when we first start a scann
			        int id = android.os.Process.myPid();
					Log.i("PL","////////////////////////////////////////////////////// PL SERVICE STARTED PID "+ String.valueOf(id) +" ////////////////////////////////////////////");
					//The service has been started so 
					startScan();
					reportActivity(START_SERVICE,0);
					break;
				case BLUETOOTH_NOT_ENABLED:
					//We need to report this then kill our self
					reportActivity(BT_NOT_ENABLED,0);
					stopAndSetWakeUp();
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
					if(! IGNORE_TIMEOUT)
					{
						BLECont.stopScanning();
						//If we are here the beacon has not been found.
						Log.i("PL","////////////////////////////////////////////////////// PL TIMED OUT ////////////////////////////////////////////");
						reportActivity(BEACON_NOT_FOUND,0);
					}
				}
			}
		}, SCANTIME);
		
		
		//Setup notification
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		//As we only be sending messages here its ok to not register a reciever.
		mBroadcastReceiverManager = LocalBroadcastManager.getInstance(getApplicationContext());
		
		
		super.onCreate();
	}

	
	private void reportActivity(int code, int data)
	{
		//We need to use notifications and a broadcaster here
		mInfoIntent = new Intent();
		mInfoIntent.setAction(MainActivity.IFILTER);
		
		switch (code) {
		case BATTERY_LEVEL:
			mInfoIntent.setFlags(BATTERY_LEVEL);
			mInfoIntent.putExtra(BATTERY_LVL_REPORT, data);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			
			break;
		case BEACON_OK:
			Log.i("PL","////////////////////////////////////////////////////// PL BEACON FOUND OK////////////////////////////////////////////");
			//Weve seen a beacon and its a good one so report it
			IGNORE_TIMEOUT = true;
			mInfoIntent.setFlags(BEACON_OK);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			//Weve done a local braodcast of this event now update the notification
			sendNotification("Pond Level OK", R.drawable.good,BeaconResult.Beacon_OK);
			stopAndSetWakeUp();
			break;
		case BEACON_BAD:
			Log.i("PL","////////////////////////////////////////////////////// PL BEACON FOUND BAD////////////////////////////////////////////");
			//Weve seen a beacon and its a good one so report it
			IGNORE_TIMEOUT = true;
			mInfoIntent.setFlags(BEACON_BAD);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			//Weve done a local braodcast of this event now update the notification
			sendNotification("Pond Level WARNING", R.drawable.bad,BeaconResult.Beacon_LevelLow);
			stopAndSetWakeUp();
			break;
		case BEACON_NOT_FOUND:
			mInfoIntent.setFlags(BEACON_NOT_FOUND);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			sendNotification("Pond Life out of range", R.drawable.start,BeaconResult.Not_A_Beacon);//Start is our amber fish.
			stopAndSetWakeUp();
			break;
		case BT_NOT_ENABLED:
			mInfoIntent.setFlags(BT_NOT_ENABLED);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			mSprefs.setServiceState(false);//Were not running as a service so stop it now.
			//the service will not wake back up until starting
			break;
		case START_SERVICE:
			mInfoIntent.setFlags(START_SERVICE);
			mBroadcastReceiverManager.sendBroadcast(mInfoIntent);
			break;
		default:
			stopAndSetWakeUp();
			break;
		}
		
	}
	
	private void sendNotification(String message,int draw, BeaconResult bRes)
	{
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		 NotificationCompat.Builder nmb = new NotificationCompat.Builder(getApplicationContext())
		 .setContentTitle(getString(R.string.Notif_Title))
		 .setContentText(message)
		 .setSmallIcon(draw);
		 if(bRes == BeaconResult.Beacon_LevelLow)
		 {
			 //We add bad sound here
			 Uri muri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.horn);
			 nmb.setSound(muri);
		 }
		 
		 nmb.setContentIntent(pendingIntent);
		 mNotificationManager.notify(MainActivity.NOTIFICATION_ID, nmb.build());//updates this apps notif
	}
	
	//This is important so we can do any cleaning up
	private void stopAndSetWakeUp()
	{
		
		
		
		final Intent serviceInt = new Intent(this,PLservice.class);
        mPndInt = PendingIntent.getService(getApplicationContext(), MainActivity.PL_SERVICE_ID, serviceInt, PendingIntent.FLAG_ONE_SHOT);
        mAlarmManager = null;
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long timeB4Wake = SystemClock.elapsedRealtime() + SERVICE_PERIOD;
        Log.i("PL","////////////////////////////////////////////////////// PL SERVICE KILLED: Time: "+ (timeB4Wake/1000) + "////////////////////////////////////////////");
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, timeB4Wake, mPndInt);

		//this.stopSelf();
		/*
		plServiceHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				plServiceHandler.sendEmptyMessage(START_SERVICE);
			}
		}, MainActivity.SERVICE_PERIOD);
		*/
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
			 Log.i("PL",">>>>>>>>>>>>>>>>>>>>>>>>>>>>> init adapter");
			 if(mBluetoothAdapter != null)
			 {
				 mBluetoothAdapter = bluetoothManager.getAdapter();
			 }
			 BLECont = new BLEcontroller(getApplicationContext(), mBluetoothAdapter);
			 
			 createDataListener();//This will now attach the data listener from BLE class
			 
			 BLECont.startScanning();//We have started our scan.
		 }
	}
	
	
	BeaconResult br;
	public void createDataListener()
	{
		if(BLECont != null)
		{
			BLECont.setBeaconFound(new BLEcontroller.beaconFound() {
				
				@Override
				public void onBeaconFound(byte[] data) 
				{
					Log.i("beacons",">>>>>PL..");
					br = processBeaconData(data);
					if(br == BeaconResult.Beacon_OK)
					{
						reportActivity(BEACON_OK,0);//This will be changed
					}else if(br == BeaconResult.Beacon_LevelLow)
					{
						reportActivity(BEACON_BAD,0);
					}
				}
			});
		}
	}
	
	
	String beaconName;
	byte resultByte;
	int bat = 0;
	private BeaconResult processBeaconData(byte[] data)
	{

			//We have our beacon
		resultByte = getLevel(data);
		
		//Get and report battery level
		bat = getBattery(data);
		if(bat > 100) bat = 100;
		if (bat < 0) bat = 0;
		reportActivity(BATTERY_LEVEL, bat);
		
		getTemp(data);
		
		if(resultByte != (byte)0x00)
		{
			if(resultByte == (byte) 0xAA)
			{
				return BeaconResult.Beacon_OK;
			}else
			{
				return BeaconResult.Beacon_LevelLow;
			}
		}
		return BeaconResult.Not_A_Beacon;
	}
	
	private int getBattery(byte[] data)
	{
		for (int i = 0; i < data.length; i++) {
			if((data[i] == (byte) 0x0A) && (data[i+1] == SERVICEUUID_LSB) && (data[i+2] == SERVICEUUID_MSB))
			{
				//We know our float
				i+=9;
				if(data[i] == (byte)0xB1)
				{
					return data[i+1];
				}//Else we have not got a temp reading
				else
				{
					return 0;//Gone wrong.
				}
			}
		}
		return 0;
	}
	
	private float getTemp(byte[] data)
	{
		for (int i = 0; i < data.length; i++) {
			if((data[i] == (byte) 0x0A) && (data[i+1] == SERVICEUUID_LSB) && (data[i+2] == SERVICEUUID_MSB))
			{
				i+=4;
				if(data[i] == (byte) 0xA1)
				{
					i++;
					int tempBUF = (data[i] & 0xff); 
					tempBUF = tempBUF | (data[i+1] << 8); 
					tempBUF = tempBUF | (data[i+2] << 16); 
					tempBUF = tempBUF | (data[i+3] << 24); 
					

				}else
				{
					return 00;
				}
			}
		}
		return 0x00;
	}
	
	private byte getLevel(byte[] data)
	{
		for (int i = 0; i < data.length; i++) {
			if((data[i] == (byte) 0x0A) && (data[i+1] == SERVICEUUID_LSB) && (data[i+2] == SERVICEUUID_MSB))
			{
				//We have our data
				return data[i+3];
			}
		}
		return 0x00;
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("PL",">>>>>>>>>>>>>>>>>>>>>>>>>>>>> PL ON START");
		if(mSprefs.getServiceState())
		{
			plServiceHandler.sendEmptyMessage(START_SERVICE);
		}else
		{
			mNotificationManager.cancel(MainActivity.NOTIFICATION_ID);
		}
		return super.onStartCommand(intent, flags, startId);
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		
	}

	
}
