package ontologizer;

import java.io.IOException;

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
				});
			annotation = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public void load(Runnable done, final OBOProgress oboProgess, final AssociationProgess associationProgess)
	{
		/* Load obo file */
		final String oboName = "go-basic.obo.gz";
		final ArrayBufferHttpRequest oboRequest = ArrayBufferHttpRequest.create("GET",oboName);
		oboRequest.onComplete(() ->
		{
			parseObo(oboProgess, new ByteArrayParserInput(oboRequest.getResponseBytes()));

			/* Load associations */
			final ArrayBufferHttpRequest assocRequest = ArrayBufferHttpRequest.create("GET", associationFilename);
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
