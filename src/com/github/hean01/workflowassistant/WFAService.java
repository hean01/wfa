package com.github.hean01.workflowassistant;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Locale;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.SoundPool;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WFAService extends Service implements TextToSpeech.OnInitListener
{
    public static final String TAG = "WFAService";
    public static final int CLOCK_RESOLUTION_MS = 1000;

    public static final int MSG_SAY = 1;
    public static final int MSG_PLAY_BELL = 2;

    private int _refCount;
    private WorkflowManager _workflowManager;
    private Workflow _currentWorkflow;
    private static Timer _timer;
    private boolean _useTextToSpeech = false;
    private TextToSpeech _tts;
    private SoundPool _sp;
    private int _soundBell;
    private SharedPreferences _preferences;

    private Set<WorkflowObserver> _observers;

    public class WFAServiceBinder extends Binder {
	WFAService getService() {
	    return WFAService.this;
	}
    };

    private final IBinder _serviceBinder = new WFAServiceBinder();

    private final Handler _serviceHandler = new Handler()
    {
	@Override
	public void handleMessage(Message msg)
	{
	    switch(msg.what)
	    {
	    case MSG_SAY:
		if (_preferences.getBoolean("tts_feedback", false))
		    say((String)msg.obj);
		break;
	    case MSG_PLAY_BELL:
		if (_preferences.getBoolean("bell_feedback", false))
		    _sp.play(_soundBell, 1.0f, 1.0f, 0, 0, 1.0f);
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
	    _currentWorkflow.clock(WFAService.CLOCK_RESOLUTION_MS);

	    /* check if workflow is finished and cleanup */
	    if (_currentWorkflow.isFinished())
	    {
		_currentWorkflow = null;
		_timer.cancel();
		_timer = null;

		_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Workflow is completed."));

		/* if not bound to activity stop service */
		if (_refCount == 0)
		    shutdown();
	    }
	}
    };

    private final ClockTask _clockTask = new ClockTask();

    /** Speak message using TextToSpeech */
    private void say(String message)
    {
	if (_useTextToSpeech)
	    _tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
	else
	    Log.i(TAG, message);
    }

    /** Register a WorkflowObserver with the service */
    public void addWorkflowObserver(WorkflowObserver observer)
    {
	if (_observers.contains(observer))
	    return;

	_observers.add(observer);

	/* Add to current workflow is exists */
	if (_currentWorkflow != null)
	    _currentWorkflow.addObserver(observer);
    }

    /** Unregister a WorkflowObserver from the service */
    public void removeWorkflowObserver(WorkflowObserver observer)
    {
	if (!_observers.contains(observer))
	    return;

	_observers.remove(observer);

	/* Remove from current workflow is in use */
	if (_currentWorkflow != null)
	    _currentWorkflow.removeObserver(observer);
    }

    public void runWorkflow()
    {
	/* check if already running */
	if (_timer != null)
	{
	    Toast.makeText(this, "Workflow already in progress", Toast.LENGTH_SHORT).show();
	    return;
	}

	/* Do we have any workflows loaded */
	if (_workflowManager.size() == 0)
	{
	    Toast.makeText(this, "No workflows available", Toast.LENGTH_SHORT).show();
	    return;
	}

	/* start new workflow */
	_currentWorkflow = _workflowManager.get(0);
	_currentWorkflow.reset();

	/* add observers */
	for (Iterator<WorkflowObserver> it = _observers.iterator(); it.hasNext();)
	{
	    WorkflowObserver observer = it.next();
	    _currentWorkflow.addObserver(observer);
	}


	_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Starting workflow with task:" + _currentWorkflow.task().name()));


	_timer = new Timer();
	_timer.scheduleAtFixedRate(_clockTask, 0, WFAService.CLOCK_RESOLUTION_MS);

    }

    @Override
    public void onCreate()
    {
	_observers = new HashSet<WorkflowObserver>();

	_preferences = PreferenceManager.getDefaultSharedPreferences(this);

	_tts = new TextToSpeech(this, this);

	_sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
	_soundBell = _sp.load(this, R.raw.bell, 1);
	_workflowManager = new WorkflowManager(this);

	Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
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

	Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show();
    }

    /** Callback from TextToSpeech service when initialized */
    @Override
    public void onInit(int status)
    {
	boolean result = true;
	if (status != TextToSpeech.SUCCESS)
	    result = false;

	if (result && _tts.setLanguage(Locale.US) == TextToSpeech.LANG_NOT_SUPPORTED)
	    result = false;

	_useTextToSpeech = result;
    }

    /** get the service handler */
    public Handler handler()
    {
	return _serviceHandler;
    }

    /** get shared preferences */
    public SharedPreferences preferences()
    {
	return _preferences;
    }

    /** get current workflow */
    public Workflow workflow()
    {
	return _currentWorkflow;
    }

    private void shutdown()
    {
	stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
	/* keep track on each bind to know when to shutdown service */
	_refCount++;
	return _serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
	_refCount--;

	/* Shutdown service if this was the last unbind and we are not
	 * currently running a workflow */
	if (_refCount == 0 && _timer == null)
	    shutdown();

	return false;
    }

}
