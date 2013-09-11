package com.github.hean01.workflowtimer;

import android.app.Activity;
import android.os.Bundle;

public class WorkflowTimerActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

	/** Bind wft service */

	/** If service is already running a wft show progress */

	/** else show wtf manager */
        setContentView(R.layout.manager);
    }
}
