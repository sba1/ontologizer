package ontologizer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLHeadElement;
import org.teavm.jso.dom.xml.Text;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.TermForTermCalculation;
import ontologizer.ontology.OBOParser;
import ontologizer.ontology.OBOParserException;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermContainer;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Bonferroni;
import ontologizer.types.ByteString;

abstract class Column implements JSObject
{
	@JSProperty
	public native void setField(String field);

	@JSProperty
	public native void setTitle(String field);

	@JSBody(script="return {field: id, title: title}", params = {"id", "title"})
	public static native JSObject createColumn(String id, String title);
}

abstract class Row implements JSObject
{
	public Row setColumn(String col, String value)
	{
		setProperty(col, value);
		return this;
	}

	@JSIndexer
	private native void setProperty(String prop, String value);

	@JSBody(script="return {}", params = {})
	public static native Row createRow();
}

abstract class HTMLBootstrapTableElement implements HTMLElement
{
	@JSProperty
	public abstract String getSummary();

	public void bootstrapTable(Column...col)
	{
		bootstrapTable_(this, col);
	}

	public void showLoading()
	{
		showLoading_(this);
	}

	public void hideLoading()
	{
		hideLoading_(this);
	}

	public void removeAll()
	{
		removeAll_(this);
	}

	@JSBody(script="$(this).bootstrapTable('append', data)", params="data")
	public native void append(JSObject data);

	@JSBody(script="$(obj).bootstrapTable({	columns: col})", params = { "obj", "col"})
	private static native void bootstrapTable_(HTMLBootstrapTableElement obj, Column... col);

	@JSBody(script="$(obj).bootstrapTable('showLoading')", params = { "obj" })
	private static native void showLoading_(HTMLBootstrapTableElement obj);

	@JSBody(script="$(obj).bootstrapTable('hideLoading')", params = { "obj" })
	private static native void hideLoading_(HTMLBootstrapTableElement obj);

	@JSBody(script="$(obj).bootstrapTable('removeAll')", params = { "obj" })
	private static native void removeAll_(HTMLBootstrapTableElement obj);
}

abstract class ArrayBufferHttpRequest extends XMLHttpRequest
{
	/**
	 * Return the response as a byte array.
	 *
	 * @return the byte array of the result
	 */
	public byte [] getResponseBytes()
	{
		return getResponseBytes_(this);
	}

	@JSBody(script="return new Int8Array(request.response)", params={"request"})
	private static native byte [] getResponseBytes_(ArrayBufferHttpRequest request);

	public static ArrayBufferHttpRequest create()
	{
		XMLHttpRequest req = XMLHttpRequest.create();
		req.setResponseType("arraybuffer");
		return req.cast();
	}
}

/**
 * Main class of the Ontologizer Web client.
 *
 * @author Sebastian Bauer
 */
public class OntologizerClient
{
	private static final String COL_ID = "id";
	private static final String COL_NAME = "name";
	private static final String COL_PVAL = "pval";

	private static HTMLDocument document = Window.current().getDocument();

	private static Text studySetText;
	private static HTMLButtonElement allGenesButton;
	private static HTMLBootstrapTableElement resultsTable;

	private static Ontology ontology;
	private static AssociationContainer annotation;

	public static void studySetChanged(String studySet)
	{
		String [] lines = studySet.split("\n");
		studySetText.setNodeValue(lines.length + " items");
	}

	public static void main(String[] args) throws IOException, OBOParserException
	{
		/* Title */
		HTMLHeadElement head = document.createElement("h1").cast();
		head.setInnerHTML("Ontologizer");
		HTMLBodyElement body = document.getBody();
		body.appendChild(head);

		/* Study set text area */
		final HTMLTextAreaElement studySet = document.createElement("textarea").cast();
		body.appendChild(studySet);
		studySet.appendChild(document.createTextNode("text"));

		studySet.listenKeyPress(evt -> studySetChanged(studySet.getValue()));

		studySetText = document.createTextNode("");
		body.appendChild(studySetText);
		HTMLElement input = document.createElement("form").cast();
		body.appendChild(input);
		allGenesButton = document.createElement("button").withText("All Genes").cast();
		allGenesButton.setType("button");
		allGenesButton.listenClick(ev ->
		{
			if (annotation == null) return;

			StringBuilder allGenes = new StringBuilder();
			for (ByteString gene : annotation.getAllAnnotatedGenes())
			{
				allGenes.append(gene.toString());
				allGenes.append("\n");
			}
			studySet.setInnerHTML(allGenes.toString());
		});
		input.appendChild(allGenesButton);
		HTMLButtonElement ontologizeButton = document.createElement("button").withText("Ontologize").cast();
		ontologizeButton.setType("button");
		ontologizeButton.listenClick(ev ->
		{
			if (annotation == null) return;

			TermForTermCalculation calculation = new TermForTermCalculation();
			PopulationSet population = new PopulationSet();
			population.addGenes(annotation.getAllAnnotatedGenes());
			StudySet study = new StudySet();
			for (String s : studySet.getValue().split("\n"))
				study.addGene(new ByteString(s), "");
			EnrichedGOTermsResult result = calculation.calculateStudySet(ontology, annotation, population, study, new Bonferroni());
			System.out.println(result.getSize() + " terms");

			int i = 0;
			AbstractGOTermProperties [] results = new AbstractGOTermProperties[result.getSize()];
			for (AbstractGOTermProperties p : result)
				results[i++] = p;
			Arrays.sort(results, Comparator.comparingDouble(p -> p.p));

			if (resultsTable == null)
			{
				Column idColumn = Column.createColumn(COL_ID, "GO ID").cast();
				Column nameColumn = Column.createColumn(COL_NAME, "Name").cast();
				Column pvalColumn = Column.createColumn(COL_PVAL, "P value").cast();

				resultsTable = document.createElement("table").cast();
				resultsTable.bootstrapTable(idColumn,nameColumn,pvalColumn);
				resultsTable.hideLoading();
				body.appendChild(resultsTable);
			}

			i = 0;
			for (AbstractGOTermProperties p : results)
			{
				if (i++ > 30) break;

				resultsTable.append(Row.createRow().
						setColumn(COL_ID, p.goTerm.getIDAsString()).
						setColumn(COL_NAME, p.goTerm.getName()).
						setColumn(COL_PVAL, p.p_adjusted + ""));
			}
		});
		input.appendChild(ontologizeButton);

		/* Load obo file */
		final ArrayBufferHttpRequest oboRequest = ArrayBufferHttpRequest.create();
		oboRequest.open("GET", "gene_ontology.1_2.obo.gz");
		oboRequest.onComplete(() ->
		{
			OBOParser oboParser = new OBOParser(new ByteArrayParserInput(oboRequest.getResponseBytes()));
			try
			{
				oboParser.doParse();
				final TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
				ontology = Ontology.create(goTerms);
				System.out.println(ontology.getNumberOfTerms() + " terms");

				/* Load associations */
				final ArrayBufferHttpRequest assocRequest = ArrayBufferHttpRequest.create();
				assocRequest.open("GET", "gene_association.sgd.gz");
				assocRequest.onComplete(() ->
				{
					byte[] assocBuf = Utils.getByteResult(assocRequest);
					try
					{
						AssociationParser ap = new AssociationParser(new ByteArrayParserInput(assocBuf), goTerms);
						annotation = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());
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
}
