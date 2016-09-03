package ontologizer;

import java.io.IOException;

import org.teavm.jso.dom.events.EventListener;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.association.IAssociationParserProgress;
import ontologizer.ontology.IOBOParserProgress;
import ontologizer.ontology.OBOParser;
import ontologizer.ontology.OBOParserException;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermContainer;

public class DatafilesLoader
{
	private String associationFilename;

	private TermContainer terms;
	private Ontology ontology;
	private AssociationContainer annotation;

	public static interface DownloadProgress
	{
		public void update(int current, int max, String title);
	}

	public static interface OBOProgress
	{
		public void update(int current, int max, int terms);
	}

	public static interface AssociationProgess
	{
		public void update(int current, int max);
	}

	public DatafilesLoader(String associationFilename)
	{
		this.associationFilename = associationFilename;
	}

	private void parseObo(final OBOProgress oboProgress, ByteArrayParserInput input)
	{
		OBOParser oboParser = new OBOParser(input);
		try
		{
			oboParser.doParse(new IOBOParserProgress()
			{
				private int max;

				@Override
				public void init(int max)
				{
					this.max = max;

					oboProgress.update(0, max, 0);
				}

				@Override
				public void update(int current, int terms)
				{
					oboProgress.update(current, max, terms);
				}
			});
			terms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			ontology = Ontology.create(terms);
		} catch (IOException | OBOParserException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Parse the given input as association file (usually in GAF).
	 *
	 * @param associationProgess progress interface
	 * @param input the input to be parsed
	 * @return true if successful, otherwise false (e.g., if no ontology is available)
	 */
	private boolean parseAssoc(final AssociationProgess associationProgess, ByteArrayParserInput input)
	{
		if (terms == null || ontology == null)
		{
			return false;
		}

		try
		{
			AssociationParser ap = new AssociationParser(input, terms, null,
				new IAssociationParserProgress()
				{
					private int max;

					@Override
					public void update(int current)
					{
						associationProgess.update(current, max);
					}

					@Override
					public void init(int max)
					{
						this.max = max;
						associationProgess.update(0, max);
					}

					@Override
					public void warning(String message)
					{
						/* Ignore warnings for now */
					}
				});
			annotation = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	class ProgressForwarder implements EventListener<ProgressEvent>
	{
		private DownloadProgress downloadProgress;
		private String name;

		public ProgressForwarder(DownloadProgress progress, String name)
		{
			this.downloadProgress = progress;
			this.name = name;
		}

		@Override
		public void handleEvent(ProgressEvent ev)
		{
			if (ev.isLengthComputable())
			{
				downloadProgress.update(ev.getLoaded(), ev.getTotal(), name);
			} else
			{
				downloadProgress.update(0, 1, name);
			}
		}
	}

	public void load(Runnable done, final DownloadProgress downloadProgress, final OBOProgress oboProgess, final AssociationProgess associationProgess)
	{
		/* Load obo file */
		final String oboName = "go-basic.obo.gz";
		final ArrayBufferHttpRequest oboRequest = ArrayBufferHttpRequest.create("GET",oboName);
		oboRequest.addEventListener("progress", new ProgressForwarder(downloadProgress, oboName));
		oboRequest.onComplete(() ->
		{
			parseObo(oboProgess, new ByteArrayParserInput(oboRequest.getResponseBytes()));

			/* Load associations */
			final ArrayBufferHttpRequest assocRequest = ArrayBufferHttpRequest.create("GET", associationFilename);
			assocRequest.addEventListener("progress", new ProgressForwarder(downloadProgress, associationFilename));
			assocRequest.onComplete(() ->
			{
				parseAssoc(associationProgess, new ByteArrayParserInput(assocRequest.getResponseBytes()));

				done.run();
			});
			assocRequest.send();
		});
		oboRequest.send();
	}

	public Ontology getOntology()
	{
		return ontology;
	}

	public AssociationContainer getAnnotation()
	{
		return annotation;
	}
}
