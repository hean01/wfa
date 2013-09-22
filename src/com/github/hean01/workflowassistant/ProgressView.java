package com.github.hean01.workflowassistant;

import java.util.Iterator;
import java.util.Date;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.view.Gravity;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Typeface;
public class ProgressView extends LinearLayout
{

    private int _currentTaskIndex;

    /** Workflow task item */
    public static class WorkflowTaskView extends LinearLayout
    {
	TextView _time;
	TextView _name;
	WorkflowTask _task;
	Date _date;

	public WorkflowTaskView(Context context, WorkflowTask task) {
	    super(context);
	    _task = task;

	    setOrientation(VERTICAL);
	    setBackgroundColor(0xff308020);
	    setPadding(10,10,10,10);
	    _time = new TextView(context);
	    _time.setTypeface(Typeface.DEFAULT_BOLD);
	    _time.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
	    _time.setGravity(Gravity.CENTER);
	    _time.setShadowLayer(6,2,2,0xff000000);
	    _time.setText("00:00:00");
	    addView(_time);

	    _date = new Date(task.length());

	    String desc = new String();
	    if (_date.getHours() != 0)
		desc += String.format(" %dh", _date.getHours());

	    if (_date.getMinutes() != 0)
		desc += String.format(" %dm", _date.getMinutes());

	    if (_date.getSeconds() != 0)
		desc += String.format(" %ds", _date.getSeconds());

	    desc = desc.trim();
	    if (!desc.isEmpty())
		desc += "  -  ";

	    desc += _task.name();

	    _name = new TextView(context);
	    _name.setText(desc);
	    _time.setTypeface(Typeface.DEFAULT);
	    _name.setGravity(Gravity.CENTER);
	    _name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
	    _name.setShadowLayer(4,1,1,0xff000000);
	    addView(_name);

	    /* update view of task */
	    update();
	}

	public void update()
	{
	    long s = _task.timeLeft()/1000;
	    long m = s/60;
	    long h = m/60;
	    m -= h*60;
	    s -= m*60;
	    String time = new String();
	    time = time.format("%02d:%02d:%02d", h, m, s);
	    _time.setText(time);

	    if (_task.getState() == WorkflowTask.State.FINISHED)
	    {
		_name.setTextColor(0x40e0e0e0);
		_time.setTextColor(0x40e0e0e0);
		setBackgroundColor(0x40308020);
	    }
	    else if (_task.getState() == WorkflowTask.State.RUNNING)
	    {
		_name.setTextColor(0xffe0e0e0);
		_time.setTextColor(0xffe0e0e0);
		setBackgroundColor(0xff308030);
	    }
	    else
	    {
		_name.setTextColor(0x7fe0e0e0);
		_time.setTextColor(0x7fe0e0e0);
		setBackgroundColor(0x7f308020);
	    }

	}
    }

    public ProgressView(Context context, Workflow workflow)
    {
	super(context);
	setOrientation(VERTICAL);

	for (Iterator<WorkflowTask> it = workflow.tasks().iterator(); it.hasNext();)
	{
	    WorkflowTask task = it.next();
	    addView(new WorkflowTaskView(context,task));
	}

	_currentTaskIndex = workflow.indexOfCurrentTask();
    }

    public void nextTask()
    {
	WorkflowTaskView tv = (WorkflowTaskView)getChildAt(_currentTaskIndex);
	scrollBy(0, tv.getMeasuredHeight());

	_currentTaskIndex++;
	update();
    }

    public void update()
    {
	WorkflowTaskView tv = (WorkflowTaskView)getChildAt(_currentTaskIndex);
	tv.update();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh)
    {
	WorkflowTaskView tv = (WorkflowTaskView)getChildAt(_currentTaskIndex);
	int offs = tv.getMeasuredHeight() * _currentTaskIndex;
	scrollTo(0, -((h/2) - (tv.getMeasuredHeight()/2)));
	scrollBy(0, offs);
    }

}
