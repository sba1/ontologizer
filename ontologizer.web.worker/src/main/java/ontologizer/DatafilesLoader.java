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

	public void load(Runnable done, final OBOProgress oboProgess, final AssociationProgess associationProgess)
	{
		/* Load obo file */
		final ArrayBufferHttpRequest oboRequest = ArrayBufferHttpRequest.create();
		oboRequest.open("GET", "gene_ontology.1_2.obo.gz");
		oboRequest.onComplete(() ->
		{
			OBOParser oboParser = new OBOParser(new ByteArrayParserInput(oboRequest.getResponseBytes()));
			try
			{
				oboParser.doParse(new IOBOParserProgress()
				{
					private int max;

					@Override
					public void init(int max)
					{
						this.max = max;

						oboProgess.update(0, max, 0);
					}

					@Override
					public void update(int current, int terms)
					{
						oboProgess.update(current,  max, terms);
					}
				});
				final TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
				ontology = Ontology.create(goTerms);

				/* Load associations */
				final ArrayBufferHttpRequest assocRequest = ArrayBufferHttpRequest.create();
				assocRequest.open("GET", associationFilename);
				assocRequest.onComplete(() ->
				{
					byte[] assocBuf = Utils.getByteResult(assocRequest);
					try
					{
						AssociationParser ap = new AssociationParser(new ByteArrayParserInput(assocBuf), goTerms, null, new IAssociationParserProgress()
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

						done.run();

						System.out.println(annotation.getAllAnnotatedGenes().size() + " annotated genes");
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				});
				assocRequest.send();
			} catch (IOException | OBOParserException e)
			{
				e.printStackTrace();
			}
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
