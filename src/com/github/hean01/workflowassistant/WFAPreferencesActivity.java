package com.github.hean01.workflowassistant;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class WFAPreferencesActivity extends PreferenceActivity
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
