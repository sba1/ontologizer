package ontologizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.teavm.jso.JSProperty;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLHeadElement;
import org.teavm.jso.dom.xml.Text;
import org.teavm.jso.typedarrays.Uint8Array;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.go.IParserInput;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Ontology;
import ontologizer.go.TermContainer;

/**
 * Main class of the Ontologizer Web client.
 *
 * @author Sebastian Bauer
 */
public class OntologizerClient
{
	private static HTMLDocument document = Window.current().getDocument();

	private static Text studySetText;

	private static Ontology ontology;
	private static AssociationContainer annotation;

	/**
	 * Simple TextArea type.
	 *
	 * @author Sebastian Bauer
	 */
	public interface HTMLTextAreaElement extends HTMLElement
	{
	    @JSProperty
	    String getValue();
	}

	public static void studySetChanged(String studySet)
	{
		String [] lines = studySet.split("\n");
		studySetText.setNodeValue(lines.length + " items");
	}

	private static byte [] getByteResult(final XMLHttpRequest oboRequest)
	{
		Uint8Array array = Uint8Array.create(oboRequest.getResponse().cast());
		byte [] buf = new byte[array.getByteLength()];
		for (int i=0; i < buf.length; i++)
		{
			buf[i] = (byte)array.get(i);
		}
		return buf;
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

		final class ByteArrayParserInput implements IParserInput
		{
			private int size;
			private ByteArrayInputStream bais;
			private InputStream is;

			public ByteArrayParserInput(byte [] buf)
			{
				size = buf.length;
				bais = new ByteArrayInputStream(buf);

				if (buf.length >= 2 && buf[0] == (byte)0x1f && buf[1] == (byte)0x8b)
				{
					try
					{
						is = new GZIPInputStream(bais);
					} catch (IOException e)
					{
						is = bais;
					}
				} else
				{
					is = bais;
				}
			}

			@Override
			public InputStream inputStream()
			{
				return is;
			}

			@Override
			public void close()
			{
			}

			@Override
			public int getSize()
			{
				return size;
			}

			@Override
			public int getPosition()
			{
				return size - bais.available();
			}

			@Override
			public String getFilename()
			{
				return "";
			}
		}

		/* Load obo file */
		final XMLHttpRequest oboRequest = XMLHttpRequest.create();
		oboRequest.open("GET", "gene_ontology.1_2.obo.gz");
		oboRequest.setResponseType("arraybuffer");
		oboRequest.onComplete(() ->
		{
			byte[] oboBuf = getByteResult(oboRequest);

			OBOParser oboParser = new OBOParser(new ByteArrayParserInput(oboBuf));
			try
			{
				oboParser.doParse();
				final TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
				ontology = new Ontology(goTerms);
				System.out.println(ontology.getNumberOfTerms() + " terms");

				/* Load associations */
				final XMLHttpRequest assocRequest = XMLHttpRequest.create();
				assocRequest.open("GET", "gene_association.sgd.gz");
				assocRequest.setResponseType("arraybuffer");
				assocRequest.onComplete(() ->
				{
					byte[] assocBuf = getByteResult(assocRequest);
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
