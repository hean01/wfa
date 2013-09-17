package com.github.hean01.workflowassistant;

import java.lang.Thread;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.view.View;
import android.util.Log;

public class WFAEditorActivity extends Activity
{
    private final static String TAG = "WFAEditorActivity";

    private WFAService _service;
    private boolean _serviceIsBound;

    private ServiceConnection _serviceConn = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder binder) {
		_service = ((WFAService.WFAServiceBinder) binder).getService();
		Log.i(TAG, "Connected to service.");
		setContentView(R.layout.editor);
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
}
