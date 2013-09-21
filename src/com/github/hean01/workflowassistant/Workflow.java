package com.github.hean01.workflowassistant;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import android.util.Log;
import android.os.Message;

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
    private Set<WorkflowObserver> _observers;

    public Workflow(WFAService service)
    {
	_state = State.UNINITIALIZED;
	_name = service.getString(R.string.unnamed);
	_description = "";
	_tasks = new ArrayList<WorkflowTask>();
	_observers = new HashSet<WorkflowObserver>();
    }

    /** set name of workflow */
    public void name(String name)
    {
	_name = name;
    }

    /** get name of workflow */
    public String name()
    {
	return _name;
    }

    /** set description of workflow */
    public void description(String description)
    {
	_description = description;
    }

    /** add a task to workflow */
    public void addTask(WorkflowTask task)
    {
	_tasks.add(task);
    }

    /** finalizes the workflow */
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

    /** get next task in workflow */
    public WorkflowTask nextTask()
    {
	int nidx = _progressIterator.nextIndex();
	if (nidx != _tasks.size())
	    return _tasks.get(nidx);

	return null;
    }

    /** get index of current task in workflow */
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

	/* clock task and notify observers on change */
	_currentTask.clock(time);
	notifyOnTaskChange();

	/* is task finished */
	if (_currentTask.getState() != WorkflowTask.State.FINISHED)
	    return;

	/* is workflow finished */
	if (!_progressIterator.hasNext())
	{
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


