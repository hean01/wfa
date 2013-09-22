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
import android.os.Message;
import android.content.Intent;
import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.SoundPool;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WFAService extends Service implements TextToSpeech.OnInitListener, WorkflowObserver
{
    public static final String TAG = "WFAService";
    public static final int CLOCK_RESOLUTION_MS = 1000;

    private static final int NOTIFYID_ONGOING_WORKFLOW = 1;

    private int _refCount;
    private WorkflowManager _workflowManager;
    private Workflow _currentWorkflow;
    private static Timer _timer;
    private boolean _useTextToSpeech = false;
    private TextToSpeech _tts;
    private SoundPool _sp;
    private int _soundBell;
    private SharedPreferences _preferences;
    private Notification _notification;

    private RemoteViews _notificationView;

    private Set<WorkflowObserver> _observers;

    public class WFAServiceBinder extends Binder {
	WFAService getService() {
	    return WFAService.this;
	}
    };

    private final IBinder _serviceBinder = new WFAServiceBinder();

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

		WFAService.this.say("Workflow is completed.");

		/* cancel notification */
		NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFYID_ONGOING_WORKFLOW);

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
	if (_useTextToSpeech && _preferences.getBoolean("tts_feedback", false))
	    _tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
	else
	    Log.i(TAG, message);
    }
    /** Sound a bell */
    private void bell()
    {
	if (_preferences.getBoolean("bell_feedback", false))
	    _sp.play(_soundBell, 1.0f, 1.0f, 0, 0, 1.0f);
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

    public void runWorkflow(int index)
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

	if (_workflowManager.size() <= index)
	{
	    Toast.makeText(this, "Workflow index out of range", Toast.LENGTH_SHORT).show();
	    return;
	}

	/* start new workflow */
	_currentWorkflow = _workflowManager.get(index);
	_currentWorkflow.reset();

	/* add observers */
	for (Iterator<WorkflowObserver> it = _observers.iterator(); it.hasNext();)
	{
	    WorkflowObserver observer = it.next();
	    _currentWorkflow.addObserver(observer);
	}


	say("Starting workflow with task:" + _currentWorkflow.task().name());

	/* add status bar notification */
	NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	nm.notify(NOTIFYID_ONGOING_WORKFLOW, _notification);

	_timer = new Timer();
	_timer.scheduleAtFixedRate(_clockTask, 0, WFAService.CLOCK_RESOLUTION_MS);

    }

    @Override
    public void onCreate()
    {
	_observers = new HashSet<WorkflowObserver>();

	_preferences = PreferenceManager.getDefaultSharedPreferences(this);

	_tts = new TextToSpeech(this, this);

	_workflowManager = new WorkflowManager(this);

	/* setup notification to show when service is running a workflow */
	_notificationView = new RemoteViews(getPackageName(), R.layout.notification);
	_notificationView.setTextViewText(R.id.notification_title, "");

	_notification = new Notification();
	_notification.tickerText = "Ongoing workflow";
	_notification.icon = R.drawable.icon;
	_notification.contentIntent = PendingIntent.getActivity(this, 0,
								new Intent(this, WFAProgressActivity.class), 0);
	_notification.contentView = _notificationView;
	_notification.flags |= (Notification.FLAG_ONGOING_EVENT |
				Notification.FLAG_NO_CLEAR |
				Notification.FLAG_FOREGROUND_SERVICE);

	/* setup sound playback */
	_sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
	_soundBell = _sp.load(this, R.raw.bell, 1);

	/* add the service as an workflow observer */
	addWorkflowObserver(this);

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

	NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	nm.cancel(NOTIFYID_ONGOING_WORKFLOW);

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

    /** get workflow manager */
    public WorkflowManager manager()
    {
	return _workflowManager;
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


    public void onTask(WorkflowTask task)
    {
	_notificationView.setTextViewText(R.id.notification_title, _currentWorkflow.name());
	_notificationView.setTextViewText(R.id.notification_content, task.name());
	NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
	nm.notify(NOTIFYID_ONGOING_WORKFLOW, _notification);

	/* Announce task */
	bell();
	say(task.name());
    }

    public void onChange(WorkflowTask task)
    {
	WorkflowTask nextTask = _currentWorkflow.nextTask();

	if (nextTask != null)
	{
	    /* Announce next task if near end of current */
	    if ((task.length() >= (30*1000)) && (task.timeLeft() == (10*1000)) &&
		_preferences.getBoolean("announce_next_task",false))
	    {
		bell();
		say(nextTask.name() + " starts in 10 seconds.");
	    }

	    /* countdown for upcoming task */
	    if (task.timeLeft() <= 5*1000 && task.timeLeft() >= 1000 && (task.timeLeft() % 1000) == 0)
	    {
		if (_preferences.getBoolean("countdown", false))
		    say(""+(task.timeLeft()/1000));
	    }

	}
    }
}
