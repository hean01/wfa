package com.github.hean01.workflowtimer;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ListIterator;
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

    /** Workflow manager */
    public static class WorkflowManager
    {
	private WorkflowTimerService _service;
	private ArrayList<Workflow> _workflows;

	public WorkflowManager(WorkflowTimerService service)
	{
	    _service = service;
	    _workflows = new ArrayList<Workflow>();
	    initialize();
	}

	/** get workflow at index */
	public Workflow get(int index)
	{
	    return _workflows.get(index);
	}

	/** initialize manager with stored workflows */
	public void initialize()
	{
	    File[] files = _service.getFilesDir().listFiles();
	    for (File file : files)
	    {
		if (!file.isFile())
		    continue;

		if (!file.getName().endsWith(".wft"))
		    continue;

		load(file);
	    }

	    /* dummy */
	    load(null);
	}

	/** load workflow file and add to manager */
	public void load(File f)
	{
	    _workflows.add(new Workflow(_service, ""));
	}
    };

    /** A workflow task */
    public static class WorkflowTask
    {
	public enum State { READY, RUNNING, FINISHED };

	private String _description;
	private int _totalTime;
	private int _elapsedTime;
	private State _state;

	public WorkflowTask (String description, int length)
	{
	    _description = description;
	    _totalTime = length;
	    _state = State.READY;
	}

	public State getState()
	{
	    return _state;
	}

	/** Get seconds left for task */
	public int getSecondsLeft()
	{
	    return (_totalTime - _elapsedTime) / 1000;
	}

	/** Clock the task if currently running */
	public void clock(int time)
	{
	    if (_state == State.READY)
		_state = State.RUNNING;

	    if (_elapsedTime >= _totalTime)
	    {
		_state = State.FINISHED;
		return;
	    }

	    _elapsedTime += time;
	}

	/** Reset the internal state of Worflow task */
	public void reset()
	{
	    _elapsedTime = 0;
	    _state = State.READY;
	}

    };

    /** A workflow */
    public static class Workflow
    {
	public enum State { UNINITIALIZED, READY, RUNNING, FINISHED };

	private String _name;
	private String _description;
	private State _state;
	private ArrayList<WorkflowTask> _tasks;
	private ListIterator<WorkflowTask> _progressIterator;
	private WorkflowTask _currentTask;
	private Handler _serviceHandler;

	public Workflow(WorkflowTimerService service, String xml)
	{
	    _state = State.UNINITIALIZED;
	    _name = service.getString(R.string.wft_unnamed);
	    _description = "";
	    _tasks = new ArrayList<WorkflowTask>();
	    _serviceHandler = service.handler();

	    initialize(xml);
	}

	/** Check if Workflow has reached end */
	public boolean isFinished()
	{
	    return (_state == State.FINISHED);
	}

	/** Reset the workflow */
	public void reset()
	{
	    ListIterator<WorkflowTask> it = _tasks.listIterator();
	    while(it.hasNext())
		it.next().reset();

	    _progressIterator = _tasks.listIterator();
	    _currentTask = _progressIterator.next();

	    _state = State.READY;
	}

	/** Clock the workflow */
	public void clock(int time)
	{
	    if (_state == State.READY)
		_state = State.RUNNING;

	    _currentTask.clock(time);

	    if (_currentTask.getState() != WorkflowTask.State.FINISHED)
		return;

	    /* check if workflow has reached the end */
	    if (!_progressIterator.hasNext())
	    {
		_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Reached end of workflow."));
		_state = State.FINISHED;
		return;
	    }

	    /* go to next task in workflow */
	    _serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Next task in workflow"));
	    _currentTask = _progressIterator.next();
	}

	/** Initialize workflow object from xml */
	protected void initialize(String xml)
	{
	    /* use javax.xml.parsers.DocumentBuilderFactory */
	    _name = "Demo workflow";
	    _description = "A internal test workflow for testing and development.";
	    _tasks.add(new WorkflowTask("Task 1", 5*1000));
	    _tasks.add(new WorkflowTask("Task 2", 10*1000));
	    _tasks.add(new WorkflowTask("Task 3", 5*1000));

	    _progressIterator = _tasks.listIterator();

	    reset();
	}
    };

    public class WorkflowTimerBinder extends Binder {
	WorkflowTimerService getService() {
	    return WorkflowTimerService.this;
	}
    };

    private final IBinder _Binder = new WorkflowTimerBinder();

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

		_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Workflow is finished."));
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

	_serviceHandler.sendMessage(_serviceHandler.obtainMessage(MSG_SAY, "Starting workflow."));

	/* start new workflow */
	_currentWorkflow = _workflowManager.get(0);
	_currentWorkflow.reset();

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
	if (status != TextToSpeech.SUCCESS)
	    return;

	if (_tts.setLanguage(Locale.US) == TextToSpeech.LANG_NOT_SUPPORTED)
	    return;

	_useAudioFeedback = true;
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
	return _Binder;
    }
}
