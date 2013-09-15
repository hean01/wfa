package com.github.hean01.workflowtimer;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class WorkflowTimerPreferenceActivity extends PreferenceActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.preferences); 
    }

    @Override
    protected void onDestroy()
    {
	super.onDestroy();
    }
}
