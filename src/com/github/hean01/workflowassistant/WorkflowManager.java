package com.github.hean01.workflowassistant;

import java.util.ArrayList;
import java.util.ListIterator;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.util.Log;
import android.content.res.AssetManager;

/** Workflow manager */
public class WorkflowManager extends DefaultHandler
{
    private static final String TAG = "WorkflowManager";
    private WFAService _service;
    private ArrayList<Workflow> _workflows;
    private Workflow _tempWorkflow;
    private WorkflowTask _tempWorkflowTask;
    private String _tempValue;

    public WorkflowManager(WFAService service)
    {
	_service = service;
	_workflows = new ArrayList<Workflow>();
	initialize();
    }

    /** get workflow count */
    public int size()
    {
	return _workflows.size();
    }

    /** get workflow at index */
    public Workflow get(int index)
    {
	return _workflows.get(index);
    }

    public Workflow[] workflows()
    {
	return _workflows.toArray(new Workflow[0]);
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

	    try {
		load(new FileInputStream(file));
	    } catch(FileNotFoundException e) {
	    }
	}

	/* No workflows available */
	if (_workflows.size() == 0)
	{
	    try {
		Log.w(TAG,"LOADING DEMO!");
		load(_service.getAssets().open("demo1.wfa"));
		load(_service.getAssets().open("demo2.wfa"));
	    } catch (IOException e) {
	    }
	}
    }

    /** load workflow file and add to manager */
    public void load(InputStream data)
    {
	try {
	    XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
	    xmlReader.setContentHandler(this);
	    xmlReader.parse(new InputSource(data));
	} catch (IOException e) {
	    Log.w(TAG, "parse() failed: IOException");
	} catch (SAXException e) {
	    Log.w(TAG, "parse() failed: ParserConfigurationException");
	} catch (ParserConfigurationException e) {
	    Log.w(TAG, "parse() failed: SAXException");
	}
    }

    public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException
    {
	if (qName.equalsIgnoreCase("workflow"))
        {
	    _tempWorkflow = new Workflow(_service);
	    _tempWorkflow.name(attr.getValue("name"));
	}
	else if (qName.equalsIgnoreCase("task"))
	{
	    try {
		_tempWorkflowTask = new WorkflowTask(attr.getValue("name"), attr.getValue("time"));
	    } catch (ParseException e) {
		throw new SAXException();
	    }
	}
    }

    public void characters(char[] ch, int start, int length) throws SAXException
    {
	_tempValue = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException
    {
	if (qName.equalsIgnoreCase("workflow"))
	    _workflows.add(_tempWorkflow);
	if (qName.equalsIgnoreCase("task"))
	    _tempWorkflow.addTask(_tempWorkflowTask);
	else if (qName.equalsIgnoreCase("description"))
	    _tempWorkflow.description(_tempValue);
    }

};
