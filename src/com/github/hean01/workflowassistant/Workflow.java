package com.github.hean01.workflowassistant;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.content.SharedPreferences;

/** A workflow */
public class Workflow
{
    public enum State { UNINITIALIZED, READY, RUNNING, FINISHED };

    private String _name;
    private String _description;
    private State _state;
    private ArrayList<WorkflowTask> _tasks;
    private ListIterator<WorkflowTask> _progressIterator;
    private WorkflowTask _currentTask;
    private Handler _serviceHandler;
    private SharedPreferences _preferences;
    private Set<WorkflowObserver> _observers;

    public Workflow(WFAService service)
    {
	_state = State.UNINITIALIZED;
	_name = service.getString(R.string.unnamed);
	_description = "";
	_tasks = new ArrayList<WorkflowTask>();
	_observers = new HashSet<WorkflowObserver>();
	_serviceHandler = service.handler();
	_preferences = service.preferences();
    }

    public void name(String name)
    {
	_name = name;
    }

    public String name()
    {
	return _name;
    }

    public void description(String description)
    {
	_description = description;
    }

    public void addTask(WorkflowTask task)
    {
	_tasks.add(task);
    }

    /* finalizes the workflow */
    public void finalize()
    {
	_progressIterator = _tasks.listIterator();
	reset();
    }

    /** Check if Workflow has reached end */
    public boolean isFinished()
    {
	return (_state == State.FINISHED);
    }

    public WorkflowTask task()
    {
	return _currentTask;
    }

    public int indexOfCurrentTask()
    {
	return _tasks.indexOf(_currentTask);
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

    /** Get array of tasks */
    public ArrayList<WorkflowTask> tasks()
    {
	return _tasks;
    }

    /** Clock the workflow */
    public void clock(int time)
    {
	if (_state == State.READY)
	{
	    _state = State.RUNNING;
	    notifyOnNewTask();
	}

	_currentTask.clock(time);
	notifyOnTaskChange();

	/* notify about next task if near end of current task */
	int nidx = _progressIterator.nextIndex();
	if (nidx != _tasks.size())
	{
	    if ((_currentTask.length() >= (30*1000)) && (_currentTask.timeLeft() == (10*1000)))
	    {
		WorkflowTask task = _tasks.get(nidx);

		if (_preferences.getBoolean("announce_next_task", false))
		{
		    _serviceHandler.sendEmptyMessage(WFAService.MSG_PLAY_BELL);
		    Message msg = _serviceHandler.obtainMessage(WFAService.MSG_SAY,
								task.name() + " starts in 10 seconds.");
		    _serviceHandler.sendMessage(msg);
		}
	    }
	}

	/* task countdown if not last task in workflow */
	if (nidx != _tasks.size())
	{
	    if (_currentTask.timeLeft() <= 5*1000 &&
		_currentTask.timeLeft() >= 1000 &&
		(_currentTask.timeLeft() % 1000) == 0)
	    {
		if (_preferences.getBoolean("countdown", false))
		    _serviceHandler.sendMessage(_serviceHandler.obtainMessage(WFAService.MSG_SAY,
									      ""+(_currentTask.timeLeft()/1000)));
	    }

	    /* announce next task */
	    else if(_currentTask.timeLeft() == 0 &&
		    _currentTask.getState() != WorkflowTask.State.FINISHED)
	    {
		_serviceHandler.sendEmptyMessage(WFAService.MSG_PLAY_BELL);
		WorkflowTask task = _tasks.get(nidx);
		Message msg = _serviceHandler.obtainMessage(WFAService.MSG_SAY, task.name());
		_serviceHandler.sendMessage(msg);
	    }
	}

	if (_currentTask.getState() != WorkflowTask.State.FINISHED)
	    return;

	/* check if workflow has reached the end */
	if (!_progressIterator.hasNext())
	{
	    _serviceHandler.sendMessage(_serviceHandler.obtainMessage(WFAService.MSG_SAY,
								      "Reached end of workflow."));
	    _state = State.FINISHED;
	    return;
	}

	/* go to next task in workflow */
	_currentTask = _progressIterator.next();
	notifyOnNewTask();
    }

    /** add an observer of workflow */
    public void addObserver(WorkflowObserver observer)
    {
	_observers.add(observer);
    }

    /** remove an observer of workflow */
    public void removeObserver(WorkflowObserver observer)
    {
	_observers.remove(observer);
    }

    /** notify observers of onTask() */
    public void notifyOnNewTask()
    {
	for (Iterator<WorkflowObserver> it = _observers.iterator(); it.hasNext();)
	{
	    WorkflowObserver observer = it.next();
	    observer.onTask(_currentTask);
	}
    }

    /** notify observers of onChange() */
    public void notifyOnTaskChange()
    {
	for (Iterator<WorkflowObserver> it = _observers.iterator(); it.hasNext();)
	{
	    WorkflowObserver observer = it.next();
	    observer.onChange(_currentTask);
	}
    }
};


