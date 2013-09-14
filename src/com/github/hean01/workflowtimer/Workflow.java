package com.github.hean01.workflowtimer;

import java.util.ArrayList;
import java.util.ListIterator;

import android.util.Log;
import android.os.Handler;
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

    public WorkflowTask task()
    {
	return _currentTask;
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
	{
	    _state = State.RUNNING;
	    _serviceHandler.sendMessage(_serviceHandler.obtainMessage(WorkflowTimerService.MSG_SAY,
								      _currentTask.name() + " started."));
	}

	_currentTask.clock(time);

	/* notify about next task if near end of current task */
	int nidx = _progressIterator.nextIndex();
	if (nidx != _tasks.size())
	{
	    if ((_currentTask.length() >= (30*1000)) && (_currentTask.timeLeft() == (10*1000)))
	    {
		WorkflowTask task = _tasks.get(nidx);
		Message msg = _serviceHandler.obtainMessage(WorkflowTimerService.MSG_SAY,
							    task.name() + " starts in 10 seconds.");
		_serviceHandler.sendMessage(msg);
	    }
	}

	/* task countdown if not last task in workflow */
	if (nidx != _tasks.size())
	{
	    if (_currentTask.timeLeft() <= 5*1000 &&
		_currentTask.timeLeft() >= 1000 &&
		(_currentTask.timeLeft() % 1000) == 0)
		{
		    _serviceHandler.sendMessage(_serviceHandler.obtainMessage(WorkflowTimerService.MSG_SAY,
								      ""+(_currentTask.timeLeft()/1000)));
		}
	}

	if (_currentTask.getState() != WorkflowTask.State.FINISHED)
	    return;

	/* check if workflow has reached the end */
	if (!_progressIterator.hasNext())
	{
	    _serviceHandler.sendMessage(_serviceHandler.obtainMessage(WorkflowTimerService.MSG_SAY,
								      "Reached end of workflow."));
	    _state = State.FINISHED;
	    return;
	}

	/* go to next task in workflow */
	_currentTask = _progressIterator.next();
    }

    /** Initialize workflow object from xml */
    protected void initialize(String xml)
    {
	/* use javax.xml.parsers.DocumentBuilderFactory */
	_name = "Demo workflow";
	_description = "A internal test workflow for testing and development.";
	_tasks.add(new WorkflowTask("Task 1", 10*1000));
	_tasks.add(new WorkflowTask("Task 2", 30*1000));
	_tasks.add(new WorkflowTask("Task 3", 10*1000));

	_progressIterator = _tasks.listIterator();

	reset();
    }
};


