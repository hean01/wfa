package com.github.hean01.workflowassistant;

import java.lang.Exception;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import android.util.Log;

/** A workflow task */
public class WorkflowTask
{
    private final static String TAG = "WorkflowTask";
    public enum State { READY, RUNNING, FINISHED };

    private String _name;
    private long _totalTime;
    private long _elapsedTime;
    private State _state;

    public WorkflowTask (String name, String length) throws ParseException
    {
	_name = name;
	_totalTime = parseTime(length);
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

    public long length()
    {
	return _totalTime;
    }

    public long timeLeft()
    {
	return (_totalTime - _elapsedTime);
    }

    /** Reset the internal state of Worflow task */
    public void reset()
    {
	_elapsedTime = 0;
	_state = State.READY;
    }

    private long parseTime(String time) throws ParseException
    {
	time = time.trim();
	String tc[] = time.split(":");
	if (tc.length != 3)
	    throw new ParseException("Format wanted HH:mm:ss", 0);

	int ts = Integer.parseInt(tc[0])*60*60;
	ts += Integer.parseInt(tc[1])*60;
	ts += Integer.parseInt(tc[2]);

	return ts*1000;
    }
};
