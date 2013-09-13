package com.github.hean01.workflowtimer;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

import android.util.Log;
import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.app.AlarmManager;
import android.widget.Toast;

public class WorkflowTimerService extends Service
{
    public static final int CLOCK_RESOLUTION_MS = 250;

    private WorkflowManager _workflowManager;

    /** Workflow manager */
    public static class WorkflowManager
    {
	private WorkflowTimerService _service;
	private ArrayList<Workflow> _workflows;

	public WorkflowManager(WorkflowTimerService service)
	{
	    _service = service;
	    initialize();
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
	public enum State { PENDING, RUNNING, FINISHED };

	private String _description;
	private int _totalTime;
	private int _elapsedTime;
	private State _state;

	public WorkflowTask (String description, int length)
	{
	    _description = description;
	    _totalTime = length;
	    _state = State.PENDING;
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
	    if (_state == State.RUNNING)
		_elapsedTime += time;

	    if (_elapsedTime >= _totalTime)
		_state = State.FINISHED;
	}

	/** Reset the internal state of Worflow task */
	public void reset()
	{
	    _elapsedTime = 0;
	    _state = State.PENDING;
	}

    };

    /** A workflow */
    public static class Workflow
    {
	public enum State { PENDING, RUNNING, FINISHED };

	private String _name;
	private String _description;
	private State _state;
	private ArrayList<WorkflowTask> _tasks;
	private ListIterator<WorkflowTask> _progressIterator;
	private WorkflowTask _currentTask;

	public Workflow(Context ctx, String xml)
	{
	    _name = ctx.getString(R.string.wft_unnamed);
	    _description = "";
	    _tasks = new ArrayList<WorkflowTask>();

	    initialize(xml);

	    _progressIterator = _tasks.listIterator();
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
	}

	/** Clock the workflow */
	public void clock(int time)
	{
	    _currentTask.clock(time);

	    if (_currentTask.getState() != WorkflowTask.State.FINISHED)
		return;

	    /* check if workflow has reached the end */
	    if (!_progressIterator.hasNext())
	    {
		_state = State.FINISHED;
		return;
	    }

	    /* go to next task in workflow */
	    _currentTask = _progressIterator.next();
	}

	/** Initialize workflow object from xml */
	protected void initialize(String xml)
	{
	    _name = "Demo workflow";
	    _description = "A internal test workflow for testing and development.";
	    _tasks.add(new WorkflowTask("Task 1", 5*1000));
	    _tasks.add(new WorkflowTask("Task 2", 10*1000));
	    _tasks.add(new WorkflowTask("Task 3", 5*1000));
	}
    };

    public class WorkflowTimerBinder extends Binder {
	WorkflowTimerService getService() {
	    return WorkflowTimerService.this;
	}
    };

    private final IBinder _Binder = new WorkflowTimerBinder();

    public void runWorkflow()
    {
	Toast.makeText(this, "Running!!!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate()
    {
	_workflowManager = new WorkflowManager(this);
	Toast.makeText(this, R.string.wft_service_started, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
	Toast.makeText(this, R.string.wft_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
	return _Binder;
    }
}
