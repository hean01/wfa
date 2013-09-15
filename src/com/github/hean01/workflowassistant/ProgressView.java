package com.github.hean01.workflowassistant;

import android.util.AttributeSet;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.content.Context;

public class ProgressView extends View
{
    private Paint _textPaint;
    private Paint _backgroundPaint;
    private String _time = new String();
    WorkflowTask _currentTask;

    public ProgressView(Context context)
    {
	super(context);

	_textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	_textPaint.setColor(0xe0ffffff);

    }

    public void newTask(WorkflowTask task)
    {
	_currentTask = task;
    }

    public void update()
    {
	if (_currentTask == null)
	    return;

	int h,m,s;

	s = _currentTask.timeLeft()/1000;
	m = s / 60;
	h = m / 60;
	m -= h*60;
	s -= m*60;

	_time = _time.format("%2d:%2d:%2d", h, m, s);

	invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
	super.onDraw(canvas);

	if (_currentTask == null)
	    return;

	/* draw timer */
	_textPaint.setTextSize(80);
	canvas.drawText(_time, 0, 120, _textPaint);

	_textPaint.setTextSize(40);
	canvas.drawText(_currentTask.name(), 0, 200, _textPaint);

    }    
}
