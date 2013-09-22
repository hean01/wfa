package com.github.hean01.workflowassistant;

import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.view.Gravity;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout;

public class ProgressView extends LinearLayout
{

    private int _currentTaskIndex;

    /** Workflow task item */
    public static class WorkflowTaskView extends LinearLayout
    {
	TextView _time;
	TextView _name;
	WorkflowTask _task;

	public WorkflowTaskView(Context context, WorkflowTask task) {
	    super(context);
	    _task = task;

	    setOrientation(VERTICAL);
	    setBackgroundColor(0xff308020);
	    setPadding(10,10,10,10);
	    _time = new TextView(context);
	    _time.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
	    _time.setGravity(Gravity.CENTER);
	    _time.setShadowLayer(6,2,2,0xff000000);
	    _time.setText("00:00:00");
	    addView(_time);

	    _name = new TextView(context);
	    _name.setText(_task.name());
	    _name.setGravity(Gravity.CENTER);
	    _name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
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
	_currentTaskIndex++;
	update();
    }

    public void update()
    {
	WorkflowTaskView tv = (WorkflowTaskView)getChildAt(_currentTaskIndex);
	tv.update();
    }
}
