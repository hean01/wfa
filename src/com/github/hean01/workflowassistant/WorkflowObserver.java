package com.github.hean01.workflowassistant;

interface WorkflowObserver
{
    /** Called when new task starts */
    public void onTask(WorkflowTask task);

    /** Called when task have changed */
    public void onChange(WorkflowTask task);
}
