package com.github.hean01.workflowassistant;

/** A workflow task */
public class WorkflowTask
{
    public enum State { READY, RUNNING, FINISHED };

    private String _name;
    private int _totalTime;
    private int _elapsedTime;
    private State _state;

    public WorkflowTask (String name, int length)
    {
	_name = name;
	_totalTime = length;
	_state = State.READY;
    }

    public State getState()
    {
	return _state;
    }

    /** Clock the task if currently running */
    public void clock(int time)
    {
	if (_state == State.READY)
	    _state = State.RUNNING;
	
	_elapsedTime += time;

	if (_elapsedTime >= _totalTime)
	    _state = State.FINISHED;
    }

    /** get name of task */
    public String name()
    {
	return _name;
    }

    public int length()
    {
	return _totalTime;
    }

    public int timeLeft()
    {
	return (_totalTime - _elapsedTime);
    }

    /** Reset the internal state of Worflow task */
    public void reset()
    {
	_elapsedTime = 0;
	_state = State.READY;
    }    
};
