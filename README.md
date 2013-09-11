Workflow Timer
==============

_Workflow Timer_ is an Android application to which assist you through
a timed workflow for an example a cardio trainer passes, film
developing processes etc.

While progressing through a workflow the individual steps are
visualized. You will see the last step finished, the current one
higlighted and the next 2 steps after that.

The current step progress will be show with a timer, name and the
steps total time to finish.

The progress will also use text-to-speech which guide you through the
workflow, telling what next step are and count down seconds before
next step.

A workflow is defined by a xml file with tasks like the following
example.

	<xml>
		<workflow name="Rodinal, Full stand, 2x acros 100">
			<description>8ml Fomadon R09, 1:125 (1L developer)</description>
			<steps>
				<step name="Fill water" time="30s"/>
				<step name="Presoak" time="3m30s" />
				<step name="Empty water" time="30s" />
				<step name="Fill developer" time="30s" />
				<step name="Agitate" time="30s" />
				<step name="Full stand" time="59m" />
				<step name="Empty developer" time="30s" />
				<step name="Fill water" time="30s" />
				<step name="Inverse 3 times" time="5s" />
				<step name="Empty water" time="30s" />
				<step name="Fill water" time="30s" />
				<step name="Inverse 3 times" time="5s" />
				<step name="Empty water" time="30s" />
				<step name="Fill water" time="30s" />
				<step name="Inverse 3 times" time="5s" />
				<step name="Empty water" time="30s" />
				<step name="Fill water" time="30s" />
				<step name="Inverse 3 times" time="5s" />
				<step name="Empty water" time="30s" />
				<step name="Fill water" time="30s" />
				<step name="Inverse 3 times" time="5s" />
				<step name="Empty water" time="30s" />
				<step name="Fill fix" time="30s" />
				<step name="Inverse 3 times" time="5s" />
				<step name="Fixing" time="3m25s" />
				<step name="Empty fix" time="30s" />
				<step name="Ilford wash"/>
			</steps>
		</workflow>
	</xml>

Import / Export of workflows should be easy so users could share their
workflows. On should be able to click a link to shared definiton and
the app should import it to list of workflows.
