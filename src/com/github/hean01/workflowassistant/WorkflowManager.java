package com.github.hean01.workflowassistant;

import java.util.ArrayList;
import java.util.ListIterator;
import java.io.File;


/** Workflow manager */
public class WorkflowManager
{
    private WFAService _service;
    private ArrayList<Workflow> _workflows;

    public WorkflowManager(WFAService service)
    {
	_service = service;
	_workflows = new ArrayList<Workflow>();
	initialize();
    }

    /** get workflow at index */
    public Workflow get(int index)
    {
	return _workflows.get(index);
    }

    /** initialize manager with stored workflows */
    public void initialize()
    {
	File[] files = _service.getFilesDir().listFiles();
	for (File file : files)
	{
	    if (!file.isFile())
		continue;

	    if (!file.getName().endsWith(".wfa"))
		continue;

	    load(file);
	}

	/* dummy */
	load(null);
    }

    /** load workflow file and add to manager */
    public void load(File f)
    {
	_workflows.add(new Workflow(_service, ""));
    }
};
