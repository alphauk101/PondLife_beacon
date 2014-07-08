package com.leetec.pondlife;

import android.content.Context;
import android.content.SharedPreferences;

public class sPrefs {

	private Context appContext;
	private String sharedPrefName = "PLSprefs";
	private String strSPstr = "serviceRunning";
	private SharedPreferences sp;
	
	public sPrefs(Context app)
	{
		appContext = app;
		sp = appContext.getSharedPreferences(sharedPrefName, 0);
	}
	
	public boolean getServiceState()
	{
		return sp.getBoolean(strSPstr, false);
	}
	
	public void setServiceState(boolean state)
	{
		SharedPreferences.Editor edit = sp.edit();
		edit.putBoolean(strSPstr, state);
		edit.commit();
	}
	
}
