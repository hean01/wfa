package com.github.hean01.workflowassistant;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.view.WindowManager;

public class WFAProgressActivity extends Activity implements WorkflowObserver
{
    private final static String TAG = "WFAProgressActivity"; 
    private WFAService _service;
    private boolean _serviceIsBound;
    private ProgressView _view;

    private ServiceConnection _serviceConn = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder binder) {
		_service = ((WFAService.WFAServiceBinder) binder).getService();
		Log.i(TAG, "Connected to service.");
		_service.addWorkflowObserver(WFAProgressActivity.this);

		_view = new ProgressView(WFAProgressActivity.this, _service.workflow());
		setContentView(_view);

	     }

	    public void onServiceDisconnected(ComponentName className) {
		_service = null;
		Log.i(TAG, "Disconnected from service.");
	    }
	};

    public void onChange(WorkflowTask task)
    {
	/* update view with current task info */
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
		    _view.update();
		}
	    });
    }

    public void onTask(WorkflowTask task)
    {
	/* advanced to next task */
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
		    _view.nextTask();
		}
	    });
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	_serviceIsBound = false;
    }

    @Override
    protected void onDestroy()
    {
	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	super.onDestroy();
    }

    @Override
    protected void onPause()
    {
	super.onPause();
	if (_serviceIsBound)
	{
	    _service.removeWorkflowObserver(WFAProgressActivity.this);
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
