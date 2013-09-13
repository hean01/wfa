package com.github.hean01.workflowtimer;

import java.lang.Thread;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;

public class WorkflowTimerActivity extends Activity
{
    private WorkflowTimerService _service;

    private ServiceConnection _serviceConn = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder binder) {
		_service = ((WorkflowTimerService.WorkflowTimerBinder) binder).getService();
		Toast.makeText(WorkflowTimerActivity.this, R.string.wft_service_connected,
			       Toast.LENGTH_SHORT).show();

		setContentView(R.layout.manager);
		_service.runWorkflow();
	    }

	    public void onServiceDisconnected(ComponentName className) {
		_service = null;
		Toast.makeText(WorkflowTimerActivity.this, R.string.wft_service_disconnected,
			       Toast.LENGTH_SHORT).show();
	    }
	};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	setContentView(R.layout.splash);
	bindService(new Intent(this, WorkflowTimerService.class), _serviceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy()
    {
	super.onDestroy();

	/** unbind the service */
	unbindService(_serviceConn);
    }
}
