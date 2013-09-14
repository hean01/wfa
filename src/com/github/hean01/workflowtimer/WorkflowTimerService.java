package com.github.hean01.workflowtimer;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Locale;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.app.AlarmManager;
import android.widget.Toast;

public class WorkflowTimerService extends Service implements TextToSpeech.OnInitListener
{
    public static final String TAG = "WorkflowTimerService";
    public static final int CLOCK_RESOLUTION_MS = 1000;

    public static final int MSG_SAY = 1;

    private WorkflowManager _workflowManager;
    private Workflow _currentWorkflow;
    private static Timer _timer;
    private boolean _useAudioFeedback = false;
    private TextToSpeech _tts;

    public class WorkflowTimerServiceBinder extends Binder {
	WorkflowTimerService getService() {
	    return WorkflowTimerService.this;
	}
    };

    private final IBinder _serviceBinder = new WorkflowTimerServiceBinder();

    private final Handler _serviceHandler = new Handler()
    {
	@Override
	public void handleMessage(Message msg)
	{
	    switch(msg.what)
	    {
	    case MSG_SAY:
		say((String)msg.obj);
		break;
	    default:
		Log.w(TAG, "No handler for message code " + msg.what);
		break;
	    }
	}
    };

    private class ClockTask extends TimerTask
    {
	public void run()
	{
	    /* clock the workflow */
	    _currentWorkflow.clock(WorkflowTimerService.CLOCK_RESOLUTION_MS);

	    /* check if workflow is finished */
	    if (_currentWorkflow.isFinished())
	    {
		_currentWorkflow = null;
		_timer.cancel();
		_timer = null;

		_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Workflow is completed."));
	    }
	}
    };

    private final ClockTask _clockTask = new ClockTask();

    public void runWorkflow()
    {
	/* check if already running */
	if (!(_timer == null))
	{
	    Toast.makeText(this, "Workflow already in progress", Toast.LENGTH_SHORT).show();
	    return;
	}

	/* start new workflow */
	_currentWorkflow = _workflowManager.get(0);
	_currentWorkflow.reset();

	_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Starting workflow with task:" + _currentWorkflow.task().name()));


	_timer = new Timer();
	_timer.scheduleAtFixedRate(_clockTask, 0, WorkflowTimerService.CLOCK_RESOLUTION_MS);

    }

    @Override
    public void onCreate()
    {
	_tts = new TextToSpeech(this, this);
	_workflowManager = new WorkflowManager(this);

	Toast.makeText(this, R.string.wft_service_started, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy()
    {
	if (_timer != null)
	    _timer.cancel();

	if (_tts != null)
	{
	    _tts.stop();
	    _tts.shutdown();
	}

	Toast.makeText(this, R.string.wft_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInit(int status)
    {
	boolean result = true;
	if (status != TextToSpeech.SUCCESS)
	    result = false;

	if (result && _tts.setLanguage(Locale.US) == TextToSpeech.LANG_NOT_SUPPORTED)
	    result = false;

	_useAudioFeedback = result;
    }

    public Handler handler()
    {
	return _serviceHandler;
    }

    public void say(String message)
    {
	if (!_useAudioFeedback)
	    Log.i("WorkflowTimerService", message);
	else
	    _tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
	return _serviceBinder;
    }
}
