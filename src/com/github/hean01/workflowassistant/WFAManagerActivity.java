package com.github.hean01.workflowassistant;

import java.lang.Thread;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;

public class WFAManagerActivity extends Activity
{
    private final static String TAG = "WFAManagerActivity";
    private final static int MENU_OPTION_PREFERENCES = 0;
    private final static int MENU_OPTION_PROGRESS = 1;

    private WFAService _service;
    private boolean _serviceIsBound;

    private ServiceConnection _serviceConn = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder binder) {
		_service = ((WFAService.WFAServiceBinder) binder).getService();
		Log.i(TAG, "Connected to service.");
		setContentView(R.layout.manager);
	    }

	    public void onServiceDisconnected(ComponentName className) {
		_service = null;
		Log.i(TAG, "Disconnected from service.");
	    }
	};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	setContentView(R.layout.splash);
	_serviceIsBound = false;

	Intent i = new Intent(this, WFAService.class);
	startService(i);
    }

    @Override
    protected void onDestroy()
    {
	super.onDestroy();
    }

    @Override
    protected void onPause()
    {
	super.onPause();
	if (_serviceIsBound)
	{
	    unbindService(_serviceConn);
	    _serviceIsBound = false;
	}
    }

    @Override
    protected void onResume()
    {
	super.onResume();
	if (!_serviceIsBound)
	    _serviceIsBound = bindService(new Intent(this, WFAService.class),
					  _serviceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	menu.add(Menu.NONE, MENU_OPTION_PROGRESS, 0, "Progress");
	menu.add(Menu.NONE, MENU_OPTION_PREFERENCES, 1, "Preferences");
	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
	switch(item.getItemId())
	{
	case MENU_OPTION_PREFERENCES:
	    startActivity(new Intent(this, WFAPreferencesActivity.class));
	    return true;

	case MENU_OPTION_PROGRESS:
	    startActivity(new Intent(this, WFAProgressActivity.class));
	    return true;

	default:
	    Log.w(TAG, "Unhandled menu option " + item.getItemId() + ".");
	}

	return false;
    }

    public void onStartButtonClick(View view)
    {
	_service.runWorkflow();
    }
}
