package com.github.hean01.workflowassistant;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;

public class WFAProgressActivity extends Activity implements WorkflowObserver
{
    private final static String TAG = "WFAProgressActivity"; 
    private WorkflowTask _currentTask;
    private WFAService _service;

    private ServiceConnection _serviceConn = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder binder) {
		_service = ((WFAService.WFAServiceBinder) binder).getService();
		Log.i(TAG, "Connected to service.");
		_service.addWorkflowObserver(WFAProgressActivity.this);
	     }

	    public void onServiceDisconnected(ComponentName className) {
		_service = null;
		Log.i(TAG, "Disconnected from service.");
	    }
	};

    public void onChange(WorkflowTask task)
    {
	/* update view with current task info */
	Log.w(TAG, "Task " + task.name() + " has changed...");
    }

    public void onTask(WorkflowTask task)
    {
	/* update view with new task */
	Log.w(TAG, "Starting new task " + task.name());
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	bindService(new Intent(this, WFAService.class), _serviceConn, Context.BIND_AUTO_CREATE);
	setContentView(R.layout.progress);
    }

    @Override
    protected void onDestroy()
    {
	super.onDestroy();
    }
}
