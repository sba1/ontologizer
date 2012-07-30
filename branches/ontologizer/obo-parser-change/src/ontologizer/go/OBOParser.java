package ontologizer.go;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.management.ValueExp;

import com.sun.corba.se.pept.transport.ContactInfo;

import ontologizer.association.AbstractByteLineScanner;
import ontologizer.types.ByteString;

import sonumina.collections.ReferencePool;

/*
 * I gratefully acknowledge the help of John Richter Day, who provided the
 * source of DAGEdit on which I based this parser for the Ontologizer and also
 * sent several useful suggestions by email. Much of the code in this class was
 * adapated verbatim from several classes in DAGEdit.
 * 
 * 
 * Of course, any errors in the present program are my own.
 */

/**
 * OBOParser parses the Gene Ontology OBO term definition file. Please see
 * www.geneontology.org for background on this file format.
 * 
 * @author Peter N. Robinson, Sebastian Bauer, Sebastian Koehler
 */
public class OBOParser
{
	private static Logger logger = Logger.getLogger(OBOParser.class.getCanonicalName());

	private enum Stanza
	{
		TERM,
		TYPEDEF
	}
	
	/** Flag to keep the definitions */
	public final static int PARSE_DEFINITIONS 	= 1 << 0;
	
	/** Flag to keep the xrefs */
	public final static int PARSE_XREFS 		= 1 << 1;
	
	/** Flag to keep the intersections */
	public final static int PARSE_INTERSECTIONS	= 1 << 2;

	/** Takes the id as name, if the name is not present */
	public final static int SETNAMEEQUALTOID	= 1 << 3;
	
	/** Ignore synonyms */
	public final static int IGNORE_SYNONYMS     = 1 << 4;
	
	/**
	 * Escaped characters such as \\ in the gene_ontology.obo file.
	 */
	private static final HashMap<Character, Character> escapeChars = new HashMap<Character, Character>();

	/**
	 * Reverse direction
	 */
	private static final HashMap<Character, Character> unescapeChars = new HashMap<Character, Character>();

	
	static
	{
		escapeChars.put(new Character(':'), new Character(':'));
		escapeChars.put(new Character('W'), new Character(' '));
		escapeChars.put(new Character('t'), new Character('\t'));
		escapeChars.put(new Character(','), new Character(','));
		escapeChars.put(new Character('"'), new Character('"'));
		escapeChars.put(new Character('n'), new Character('\n'));
		escapeChars.put(new Character('\\'), new Character('\\'));
		escapeChars.put(new Character('{'), new Character('{'));
		escapeChars.put(new Character('}'), new Character('}'));
		escapeChars.put(new Character('['), new Character('['));
		escapeChars.put(new Character(']'), new Character(']'));
		escapeChars.put(new Character('!'), new Character('!'));

		Iterator<Character> it = escapeChars.keySet().iterator();
		while (it.hasNext())
		{
			Character key = it.next();
			Character value = escapeChars.get(key);
			unescapeChars.put(value, key);
		}
	}

	/** Name and path of OBO file, e.g. gene_ontology.obo */
	private String filename;

	/** The current parse options */
	private int options;

	/** Format version of the gene_ontology.obo file */
	private String format_version;

	/** Date of the gene_ontology.obo file */
	private String date;

	/** Collection of all terms */
	private HashSet<Term> terms = new HashSet<Term>();

	/** Collection of subsets */
	private HashMap<String,Subset> subsets = new HashMap<String, Subset>();
	
	/** Statistics */
	private int numberOfRelations;
	
	/** Pool for prefixes. */
	private PrefixPool prefixPool = new PrefixPool();
	
	/** Pool for term ids */
	private ReferencePool<TermID> termIDPool = new ReferencePool<TermID>();

	/** All parsed namespaces */
	private HashMap<String,Namespace> namespaces = new HashMap<String,Namespace>(); 

	/** The Stanza currently being processed */
	private Stanza currentStanza;

	/** The id of the current Term in the stanza currently being parsed */
	private TermID currentID;

	/** The name of the GO Term currently being parsed */
	private String currentName;

	/** The namespace of the stanza currently being parsed */
	private Namespace currentNamespace;

	/** The definition of the stanza currently being parsed */
	private String currentDefintion;

	/** Is current term obsolete? */
	private boolean currentObsolete;

	/** The parents of the term of the stanza currently being parsed */
	private ArrayList<ParentTermID> currentParents = new ArrayList<ParentTermID>();

	/** The alternative ids of the term */
	private ArrayList<TermID> currentAlternatives = new ArrayList<TermID>();

	/** The equivalent ids of the term */
	private ArrayList<TermID> currentEquivalents = new ArrayList<TermID>();	

	/** Synonyms, if any, for the Term currently being parsed */
	private ArrayList<String> currentSynonyms = new ArrayList<String>();
	
	/** Intersections, if any, for the Term currently being parsed */
	private ArrayList<String> currentIntersections = new ArrayList<String>();
	
	/** The subsets */
	private ArrayList<Subset> currentSubsets = new ArrayList<Subset>();

	/** The xrefs of the term */
	private ArrayList<TermXref> currentXrefs = new ArrayList<TermXref>();

	/**
	 * @param filename
	 *            path and name of the gene_ontology.obo file
	 */
	public OBOParser(String filename)
	{
		this.filename = filename;
	}

	/**
	 * Options can be combined via logical or. Valid options are:
	 * <ul>
	 * <li>PARSE_DEFINITIONS - to gather the definition entry.
	 * </ul>
	 * 
	 * @param filename
	 *            defines the path and name of the gene_ontology.obo file
	 * @param options
	 *            defines some options.
	 */
	public OBOParser(String filename, int options)
	{
		this.filename = filename;
		this.options = options;
	}

	public Set<Term> getTermMap()
	{
		return this.terms;
	}

	/**
	 * This puts the results of the parse of a single OBO stanza into one Term
	 * object and stores that in the HashSet terms.
	 */
	private void enterNewTerm()
	{
		if (currentStanza != null)
		{
			/* Ignore typedefs */
			if (currentStanza == Stanza.TYPEDEF)
				return;

			/* If no name is defined use the id as a name */
			if (currentName == null && currentID != null)
				currentName = currentID.toString();
			
			if (currentID == null || currentName == null)
			{
				logger.warning("Error parsing stanza: " + currentStanza.toString()+" currentID: "+currentID+", currentName: "+currentName);

				resetCurrentStanza();
				return;

			}

			/* Create a Term object and put it in the HashMap terms. */
			Term t = new Term(currentID, currentName, currentNamespace, currentParents);
			t.setObsolete(currentObsolete);
			t.setDefinition(currentDefintion);
			t.setAlternatives(currentAlternatives);
			t.setEquivalents(currentEquivalents);
			t.setSubsets(currentSubsets);
			t.setSynonyms(currentSynonyms);
			t.setIntersections(currentIntersections);
			t.setXrefs(currentXrefs);
			terms.add(t);

			/* Statistics */
			numberOfRelations += currentParents.size();
		}

		resetCurrentStanza();
	}

	private void resetCurrentStanza()
	{
		/* Now reset... */
		currentID = null;
		currentName = null;
		currentNamespace = null;
		currentDefintion = null;
		currentObsolete = false;
		currentParents.clear();
		currentAlternatives.clear();
		currentEquivalents.clear();
		currentSubsets.clear();
		currentSynonyms.clear();
		currentIntersections.clear();
		currentXrefs.clear();
	}

	
	/**
	 * The main parsing routine for the gene_ontology.obo file
	 * 
	 * @return A string giving details about the parsed obo file
	 * @throws OBOParserException 
	 * @throws IOException 
	 */
	public String doParse() throws IOException, OBOParserException
	{
		return doParse(null);
	}

	/**
	 * The main parsing routine for the gene_ontology.obo file
	 *
	 * @param progress
	 * @return A string giving details about the parsed obo file
	 * @throws OBOParserException 
	 * @throws IOException 
	 */
	public String doParse(final IOBOParserProgress progress) throws IOException, OBOParserException
	{
		long startMillis = System.currentTimeMillis();
		
//		BufferedReader reader;
		FileInputStream fis = new FileInputStream(filename);
		InputStream is;
		
		try
		{
			is = new GZIPInputStream(fis);
		} catch (IOException exp)
		{
			fis.close();
			is = fis = new FileInputStream(filename);
		}

		final FileChannel fc = fis.getChannel();

		if (progress != null)
			progress.init((int)fc.size());

		class OBOByteLineScanner extends AbstractByteLineScanner
		{
			private int linenum;
			private long millis = 0;
			public int currentTerm = 0;
			
			private byte [] multilineBuf;
			
			private byte [] line;
			private int start;
			private int len;

			private byte [] temp;

			public OBOParserException exception;
			
			/* Stanza types */
			private final byte [] TERM_KEYWORD = new ByteString("term").toString().getBytes();
			private final byte [] TYPEDEF_KEYWORD = new ByteString("typedef").toString().getBytes();
			
			/* Supported header types */
			private final byte [] FORMAT_VERSION_KEYWORD = new ByteString("format-version").toString().getBytes();
			private final byte [] DATE_KEYWORD = new ByteString("date").toString().getBytes();
			private final byte [] SUBSETDEF_KEYWORD = new ByteString("subsetdef").toString().getBytes();
			
			/* Supported term types */
			private final byte [] ID_KEYWORD = new ByteString("id").toString().getBytes();
			private final byte [] NAME_KEYWORD = new ByteString("name").toString().getBytes();
			private final byte [] IS_A_KEYWORD = new ByteString("is_a").toString().getBytes();
			private final byte [] RELATIONSHIP_KEYWORD = new ByteString("relationship").toString().getBytes();
			private final byte [] SYNONYM_KEYWORD = new ByteString("synonym").toString().getBytes();
			private final byte [] DEF_KEYWORD = new ByteString("def").toString().getBytes();
			private final byte [] NAMESPACE_KEYWORD = new ByteString("namespace").toString().getBytes();
			private final byte [] ALT_ID_KEYWORD = new ByteString("alt_id").toString().getBytes();
			private final byte [] EQUIVALENT_TO_KEYWORD = new ByteString("equivalent_to").toString().getBytes();
			private final byte [] XREF_KEYWORD = new ByteString("xref").toString().getBytes();
			
			/* Supported relationship types */
			private final byte [] PART_OF_KEYWORD = new ByteString("part_of").toString().getBytes();
			private final byte [] REGULATES_KEYWORD = new ByteString("regulates").toString().getBytes();
			private final byte [] NEGATIVELY_REGULATES_KEYWORD = new ByteString("negatively_regulates").toString().getBytes();
			private final byte [] POSITIVELY_REGULATES_KEYWORD = new ByteString("positively_regulates").toString().getBytes();
			
			public OBOByteLineScanner(InputStream is)
			{
				super(is);
			}

			/**
			 * Issue a progress report.
			 */
			private void updateProgress()
			{
				if (progress != null)
				{
					try {
						long newMillis = System.currentTimeMillis();
						if (newMillis - millis > 250)
						{
							progress.update((int)fc.position(),currentTerm);
							millis = newMillis;
						}
					} catch (IOException e) {
					}
				}
			}
			
			/**
			 * Expands the multiline buf with the given buf.
			 * 
			 * @param buf
			 * @param start
			 * @param len
			 */
			private void expandMultilibeBuf(byte [] buf, int start, int len)
			{
				int oldlen;
				if (multilineBuf != null)
					oldlen = multilineBuf.length;
				else oldlen = 0;

				byte [] newMultilineBuf = new byte[oldlen + len];

				if (oldlen != 0)
					System.arraycopy(multilineBuf, 0, newMultilineBuf, 0, oldlen);
				System.arraycopy(buf, start, newMultilineBuf, oldlen, len);
				multilineBuf = newMultilineBuf;
			}
			
			/**
			 * Returns the current line content as string.
			 * 
			 * @return
			 */
			private String getLineContens()
			{
				return new ByteString(line,start,start+len).toString();
			}
			
			/**
			 * Compares buf vs cmp.
			 * @param buf
			 * @param start where to start in buf
			 * @param len where to end in buf
			 * @param cmp is assumed to be lower case
			 * @return
			 */
			private boolean equalsIgnoreCase(final byte [] buf, int start, int len, byte [] cmp)
			{
				if (cmp.length != len) return false;

				for (int i=0;i<len;i++)
				{
					if (cmp[i] != Character.toLowerCase(buf[start+i]))
						return false;
				}
				return true;
			}
			
			@Override
			public boolean newLine(byte[] buf, int start, int len)
			{
				linenum++;
				updateProgress();

				if (len == 0)
					return true;

				if (buf[start+len-1] == '\\')
				{
					expandMultilibeBuf(buf, start, len-1);
					return true;
				}

				if (multilineBuf != null)
				{
					expandMultilibeBuf(buf, start, len);
					buf = multilineBuf;
					start = 0;
					len = multilineBuf.length;
				}
				multilineBuf = null;
				
				/* Skip any comments */
				if (buf[start] == '!')
					return true;

				/* Trim line ending */
				while (len != 0 && Character.isWhitespace(buf[start + len-1]))
						len--;
				if (len == 0)
					return true;

				/* Bring the line info into our context */
				this.line = buf;
				this.start = start;
				this.len = len;

				/* If the line starts with "[", we are at a new [Term] or [Typedef] */
				if (buf[start] == '[')
				{
					enterNewTerm();
					currentTerm++;

					if (buf[start + len - 1] != ']')
					{
						exception = new OBOParserException("Unclosed stanza", getLineContens(), linenum);
						return false;
					}

					start++;
					len-=2;
					
					if (equalsIgnoreCase(line, start, len, TERM_KEYWORD)) currentStanza = Stanza.TERM;
					else if (equalsIgnoreCase(line, start, len, TYPEDEF_KEYWORD)) currentStanza = Stanza.TYPEDEF;
					else
					{
						exception = new OBOParserException("Unknown stanza type", getLineContens(), linenum);
						return false;
					}
					currentTerm++;
				} else
				{
					/* Find colon */
					int keyEnd = -1;
					int valueStart = -1;
					for (int i=start;i<start+len;i++)
					{
						if (buf[i] == ':')
						{
							keyEnd = i;
							break;
						}
					}

					/* Ignore these lines without key: value format */
					if (keyEnd == -1)
						return true;
					
					/* Find start of the value */
					for (int i=keyEnd+1;i<start+len;i++)
					{
						if (!Character.isWhitespace(buf[i]))
						{
							valueStart = i;
							break;
						}
					}
					
					/* Ignore any lines without a proper value */
					if (valueStart == -1)
						return true;

					int keyStart = start;
					int keyLen = keyEnd - start;
					int valueLen = start + len - valueStart;

					if (currentStanza == null) readHeaderValue(line, keyStart, keyLen, valueStart, valueLen);
					else if (currentStanza == Stanza.TERM) readTermValue(line, keyStart, keyLen, valueStart, valueLen);
				}
				return true;
			}

			/**
			 * Parse key/value as header.
			 * 
			 * @param buf
			 * @param keyStart
			 * @param keyLen
			 * @param valueStart
			 * @param valueLen
			 */
			private void readHeaderValue(byte[] buf, int keyStart, int keyLen, int valueStart, int valueLen)
			{
				if (equalsIgnoreCase(buf, keyStart, keyLen, FORMAT_VERSION_KEYWORD))
				{
					format_version = new String(buf, valueStart, valueLen);
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, DATE_KEYWORD))
				{
					date = new String(buf, valueStart, valueLen);
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, SUBSETDEF_KEYWORD))
				{
					Subset s = Subset.createFromString(new String(buf, valueStart, valueLen));
					if (!subsets.containsKey(s.getName()))
						subsets.put(s.getName(),s);
				}
			}
			
			/**
			 * Reads the term id stored in the buf at the given locations.
			 * 
			 * @param buf
			 * @param valueStart
			 * @param valueLen
			 * @return
			 */
			private TermID readTermID(byte[] buf, int valueStart, int valueLen)
			{
				return termIDPool.map(new TermID(new ByteString(buf,valueStart,valueStart+valueLen),prefixPool));
			}

			/**
			 * Finds teh first occurrence of c in buf starting from start but
			 * not exceeding len.
			 * 
			 * @return -1 if not found.
			 */
			private int findUnescaped(final byte [] buf, int start, int len, char c)
			{
				while (len > 0)
				{
					if (buf[start] == '\\')
					{
						start+=2;
						len-=2;
						continue;
					}
					
					if (buf[start] == c)
						return start;
					start++;
					len--;
				}
				return -1;
			}


			/**
			 * Finds teh first occurrence of c1 or c2 in buf starting from start but
			 * not exceeding len.
			 * 
			 * @return -1 if not found.
			 */
			private int findUnescaped(final byte [] buf, int start, int len, char c1, char c2)
			{
				while (len != 0)
				{
					if (buf[start] == c1|| buf[start] == c2)
						return start;
					start++;
					len--;
				}
				return -1;
			}

			/**
			 * Finds teh first occurrence of c1, c2, or c3 in buf starting from start but
			 * not exceeding len.
			 * 
			 * @return -1 if not found.
			 */
			private int findUnescaped(final byte [] buf, int start, int len, char c1, char c2, char c3)
			{
				while (len != 0)
				{
					if (buf[start] == c1|| buf[start] == c2)
						return start;
					start++;
					len--;
				}
				return -1;
			}

			/**
			 * Skip spaces starting at start not more than len.
			 * 
			 * @param buf
			 * @param start
			 * @param len
			 * @return -1 if no space could be found.
			 */
			private int skipSpaces(final byte [] buf, int start, int len)
			{
				while (len != 0)
				{
					if (buf[start] != ' ' && buf[start] != '\t')
						return start;
					start++;
					len--;
				}
				return -1;
			}
			
			/**
			 * Parse key/value as term value.
			 * 
			 * @param buf
			 * @param keyStart
			 * @param keyLen
			 * @param valueStart
			 * @param valueLen
			 */
			private void readTermValue(byte[] buf, int keyStart, int keyLen, int valueStart, int valueLen)
			{
				if (equalsIgnoreCase(buf, keyStart, keyLen, ID_KEYWORD))
				{
					currentID = readTermID(buf, valueStart, valueLen);
					if ((options & SETNAMEEQUALTOID) != 0)
						currentName = currentName.toString(); 
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, NAME_KEYWORD))
				{
					currentName = new String(buf, valueStart, valueLen);
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, IS_A_KEYWORD))
				{
					currentParents.add(new ParentTermID(readTermID(buf, valueStart, valueLen),TermRelation.IS_A));
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, RELATIONSHIP_KEYWORD))
				{
					TermRelation type;
					
					int typeStart = valueStart;
					int typeEnd = findUnescaped(buf, valueStart, valueLen, ' ');
					if (typeEnd== -1) return;
					
					int idStart = skipSpaces(buf, typeEnd, valueStart + valueLen - typeEnd);
					if (idStart == -1) return;
					int idEnd = findUnescaped(buf, idStart, valueStart + valueLen - idStart, '[', ' ', '!');
					if (idEnd == -1) idEnd = valueStart + valueLen;
					
					if (equalsIgnoreCase(buf,typeStart, typeEnd - typeStart,PART_OF_KEYWORD)) type = TermRelation.PART_OF_A;
					else if (equalsIgnoreCase(buf,typeStart, typeEnd - typeStart,REGULATES_KEYWORD)) type = TermRelation.REGULATES;
					else if (equalsIgnoreCase(buf,typeStart, typeEnd - typeStart,NEGATIVELY_REGULATES_KEYWORD)) type = TermRelation.POSITIVELY_REGULATES;
					else if (equalsIgnoreCase(buf,typeStart, typeEnd - typeStart,POSITIVELY_REGULATES_KEYWORD)) type = TermRelation.NEGATIVELY_REGULATES;
					else type = TermRelation.UNKOWN;

					currentParents.add(new ParentTermID(readTermID(buf,idStart,idEnd - idStart + 1),type));
				} else if ((options & IGNORE_SYNONYMS) == 0 && equalsIgnoreCase(buf, keyStart, keyLen, SYNONYM_KEYWORD))
				{
					int synonymStart = findUnescaped(buf, valueStart, valueLen, '\"');
					if (synonymStart == -1) return;
					synonymStart++;
					int synonymEnd = findUnescaped(buf, synonymStart, valueStart + valueLen - synonymStart, '\"');
					if (synonymEnd == -1) return;

					currentSynonyms.add(new ByteString(buf,synonymStart,synonymEnd).toString());
				} else if ((options & PARSE_DEFINITIONS) != 0 && equalsIgnoreCase(buf, keyStart, keyLen, DEF_KEYWORD))
				{
					/* TODO: Refactor with the above */
					int defStart = findUnescaped(buf, valueStart, valueLen, '\"');
					if (defStart == -1) return;
					defStart++;
					int defEnd = findUnescaped(buf, defStart, valueStart + valueLen - defStart, '\"');
					if (defEnd == -1) return;

					if (temp == null || temp.length < defEnd - defStart + 1)
						temp = new byte[defEnd - defStart + 1];
					int len = 0;
					for (int i=defStart;i<defEnd;i++)
					{
						if (buf[i]=='\\')
							continue;
						temp[len++] = buf[i];
					}
					currentDefintion = new String(temp, 0, len);
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, NAMESPACE_KEYWORD))
				{
					String newNamespace = new String(buf,valueStart, valueLen);
					Namespace namespace = namespaces.get(newNamespace);
					if (namespace == null)
					{
						namespace = new Namespace(newNamespace);
						namespaces.put(newNamespace,namespace);
					}
					
					currentNamespace = namespace;
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, EQUIVALENT_TO_KEYWORD))
				{
					currentEquivalents.add(readTermID(buf, valueStart, valueLen));
				} else if (equalsIgnoreCase(buf, keyStart, keyLen, ALT_ID_KEYWORD))
				{
					currentAlternatives.add(readTermID(buf, valueStart, valueLen));
				} else if (((options & PARSE_XREFS) !=0) && equalsIgnoreCase(buf, keyStart, keyLen, XREF_KEYWORD))
				{
					int dbStart = valueStart;
					int dbEnd = findUnescaped(buf, valueStart, valueLen, ':');
					if (dbEnd == -1) return;
					int idStart = skipSpaces(buf, dbEnd + 1, valueStart + valueLen - dbEnd);
					if (idStart == -1) return;
					int idEnd = valueStart + valueLen;

					TermXref xref = new TermXref(new String(buf,dbStart,dbEnd-dbStart), new String(buf,idStart,idEnd-idStart));
					currentXrefs.add(xref);
				}
			}

		}

		OBOByteLineScanner obls = new OBOByteLineScanner(is);
		obls.scan();
		enterNewTerm(); /* Get very last stanza after loop! */
		if (progress != null)
			progress.update((int)fc.size(),obls.currentTerm);

		if (obls.exception != null)
			throw obls.exception;
//		
//		String line;
//		int linenum;
//
//		for (linenum = 1; (line = reader.readLine()) != null; linenum++)
//		{
//			/* Progress support, call only every quarter second */
//			if (progress != null)
//			{
//				long newMillis = System.currentTimeMillis();
//				if (newMillis - millis > 250)
//				{
//					progress.update((int)fc.position(),currentTerm);
//					millis = newMillis;
//				}
//			}
//
//			line = stripSpecialCharacters(line);
//			if (line.length() == 0)
//				continue;
//
//			/*
//			 * The following takes care of multiline entries (lines
//			 * terminated with "\")
//			 */
//			while (line.charAt(line.length() - 1) == '\\'
//					&& line.charAt(line.length() - 2) != '\\')
//			{
//				String str = reader.readLine();
//				linenum++;
//				if (str == null)
//					throw new OBOParserException("Unexpected end of file", line, linenum);
//				line = line.substring(0, line.length() - 1) + str;
//			}
//			// When we get here we have one complete tag : value pair
//			if (line.charAt(0) == '!')
//				continue; /* skip "!" comments */
//			// If the line starts with "[", we are at a new [Term] or
//			// [Typedef]
//			if (line.charAt(0) == '[')
//			{
//				// If we get here, all info for a term from the previous
//				// stanza should be ready to be entered.
//				
//				enterNewTerm();
//				currentTerm++;
//				if (line.charAt(line.length() - 1) != ']')
//					throw new OBOParserException("Unclosed stanza \"" + line + "\"", line, linenum);
//
//				String stanzaname = line.substring(1, line.length() - 1);
//				if (stanzaname.length() < 1)
//					throw new OBOParserException("Empty stanza", line, linenum);
//				
//				if (stanzaname.equalsIgnoreCase("term"))
//					currentStanza = Stanza.TERM;
//				else if (stanzaname.equalsIgnoreCase("typedef"))
//					currentStanza = Stanza.TYPEDEF;
//				else throw new IllegalArgumentException("Unknown stanza type: \""+stanzaname+"\" at line " + linenum);
//			} else
//			{
//				try
//				{
//					SOPair pair;
//					try
//					{
//						pair = unescape(line, ':', 0, true);
//					} catch (OBOParserException ex)
//					{
//						ex.linenum = linenum;
//						throw ex;
//					}
//
//					String name = pair.str;
//					int lineEnd = findUnescaped(line, '!', 0, line.length());
//					if (lineEnd == -1)
//						lineEnd = line.length();
//					int trailingStartIndex = -1;
//					for (int i = lineEnd - 1; i >= 0; i--)
//					{
//						if (Character.isWhitespace(line.charAt(i)))
//							continue;
//						else
//							break;
//					}
//					int stopIndex = trailingStartIndex;
//					if (stopIndex == -1)
//						stopIndex = lineEnd;
//					String value = line.substring(pair.index + 1, stopIndex);
//					if (value.length() == 0)
//						throw new OBOParserException("Tag found with no value", line, linenum);
//
//					if (currentStanza == null)
//						readHeaderValue(name, value);
//					else
//						readTagValue(name, value);
//				} catch (IllegalArgumentException iae)
//				{
//					logger.severe("Unable to parse line at " + linenum + " " + line);
//					throw iae;
//				} catch (OBOParserException oae)
//				{
//					oae.line = line;
//					oae.linenum = linenum;
//					throw oae;
//				}
//			}
//		}
//
//		enterNewTerm(); /* Get very last stanza after loop! */
//		if (progress != null)
//			progress.update((int)fc.size(),currentTerm);
//		reader.close();

		fis.close();

		logger.info("Got " + terms.size() + " terms and " + numberOfRelations + " relations in " + (System.currentTimeMillis() - startMillis) + " ms");
		return this.getParseDiagnostics();
	}

	/** Remove non-Latin characters */
	public static String stripSpecialCharacters(final String s)
	{
		StringBuilder out = null;
		int length = s.length();
		int i;

		for (i = 0; i < length; i++)
		{
			char c = s.charAt(i);
			if (c >= 128)
			{
				out = new StringBuilder(i + 32);
				if (i!=0)
					out.append(s.substring(0, i)); /* omits the current character */
				break;
			}
		}
		
		/* No buffer allocated? So there are no non-latin characters inside s */
		if (out == null) return s;

		for (;i < length; i++)
		{
			char c = s.charAt(i);
			if (c < 128)
				out.append(c);
		}
		return out.toString();
	}

	/** This static class stores a pair or <String,int> values */
	public static class SOPair
	{
		public String str = null;

		public int index = -1;

		public SOPair(String str, int index)
		{
			this.str = str;
			this.index = index;
		}
	}

//	/**
//	 * @param name
//	 *            The tag of a Stanza in the header of the OBO file
//	 * @param value
//	 *            The value of the stanza This function is used to record the
//	 *            version and date of the gene_ontology.obo file.
//	 */
//
//	private void readHeaderValue(String name, String value)
//	{
//		value = value.trim();
//		if (name.equals("format-version"))
//		{
//			this.format_version = value;
//		} else if (name.equals("date"))
//		{
//			this.date = value;
//		} else if (name.equals("subsetdef"))
//		{
//			Subset s = Subset.createFromString(value);
//			if (!subsets.containsKey(s.getName()))
//			{
//				subsets.put(s.getName(),s);
//			}
//		}
//	}
//
//	protected void readTagValue(String name, String value) throws OBOParserException, IOException
//	{
//		value = value.trim();
//		
//		if (name.equals("import"))
//		{
//			if (currentStanza != null)
//			{
//				throw new OBOParserException("import tags may only occur in the header");
//			}
//			return;
//		} else if (name.equals("id"))
//		{
//			readID(value);
//			
//			if ((options & SETNAMEEQUALTOID) != 0){
//				readName(value);
//			}
//			
//		} else if (name.equals("name"))
//		{
//			readName(unescape(value));
//		} else if (name.equals("is_a"))
//		{
//			readISA(unescape(value));
//		} else if (name.equals("relationship"))
//		{
//			int typeIndex = findUnescaped(value, ' ', 0, value.length());
//			String type = value.substring(0, typeIndex).trim();
//			if (typeIndex == -1)
//				throw new OBOParserException("No id specified for" + " relationship");
//			int endoffset = findUnescaped(value, '[',
//					typeIndex + type.length(), value.length());
//			String id;
//			if (endoffset == -1)
//				id = value.substring(typeIndex + 1, value.length()).trim();
//			else
//			{
//				id = value.substring(typeIndex + 1, endoffset).trim();
//			}
//
//			if (id.length() == 0)
//				throw new OBOParserException("Empty id specified for relationship");
//			readRelationship(type,id);
//		} else if (name.equals("is_obsolete"))
//		{
//			currentObsolete = value.equalsIgnoreCase("true");
//		}
//		else if (name.equals("synonym") && (options & IGNORE_SYNONYMS) == 0) {
//			readSynonym(value);
//		}
//		else if (name.equals("namespace"))
//		{
//			readNamespace(value);
//		} else if (name.equals("alt_id"))
//		{
//			readAlternative(value);
//		}else if (name.equals("equivalent_to"))
//		{
//			readEquivalent(value);
//		} else if (name.equals("subset"))
//		{
//			readSubset(value);
//		}
//		else if (name.equals("intersection_of") && (options & PARSE_INTERSECTIONS) != 0)
//		{
//			currentIntersections.add(value);
//		}
//		else if (name.equals("def") && (options & PARSE_DEFINITIONS) != 0)
//		{
//			if (value.startsWith("\""))
//				currentDefintion = unescape(value, '\"', 1, value.length(),false).str;
//		}
//		else if (name.equals("xref") && (options & PARSE_XREFS) != 0)
//		{
//			readXref(value);
//		}
//		/*
//			 * else if (name.equals("comment")) { return; } else if
//			 * (name.equals("domain")) { return; } else if
//			 * (name.equals("range")) { return; } else if
//			 * (name.equals("xref_analog")) { return; } else if
//			 * (name.equals("xref_unk")) { return; } else if
//			 * (name.equals("subset")) { return; } else if
//			 * (name.equals("synonym")) { return; } else if
//			 * (name.equals("related_synonym")) { return; } else if
//			 * (name.equals("exact_synonym")) { return; } else if
//			 * (name.equals("narrow_synonym")) { return;
//			 *  } else if (name.equals("broad_synonym")) { return; } else if
//			 * (name.equals("relationship")) { return; }
//			 */
//	}
	private void readXref(String value)
	{
		try
		{
			if ( ! value.contains(":")){
				logger.info("ignoring xref: "+value);
				return;
			}
			String[] xrefSplit 	= value.split(":");
			String dbName		= xrefSplit[0];
			TermXref xref 		= new TermXref(dbName, xrefSplit[1]);
			currentXrefs.add(xref);
		} catch (IllegalArgumentException e)
		{
			logger.warning("Unable to parse xref from : \""+value+"\"");
		}
	}
	
	private void readAlternative(String value)
	{
		try
		{
			currentAlternatives.add(termIDPool.map(new TermID(value,prefixPool)));
		} catch (IllegalArgumentException e)
		{
			logger.warning("Unable to parse alternative ID: \""+value+"\"");
		}

	}
	
	private void readEquivalent(String value)
	{
		try
		{
			currentEquivalents.add(termIDPool.map(new TermID(value,prefixPool)));
		} catch (IllegalArgumentException e)
		{
			logger.warning("Unable to parse equivalent ID: \""+value+"\"");
		}

	}
	
	private void readSubset(String value)
	{
		Subset subset = subsets.get(value);
		if (subset != null)
			currentSubsets.add(subset);
		else
			logger.warning("Subset \"" + value + "\" wasn't defined in the header of the file. Ignored.");
	}

	private static String unescape(String str) throws OBOParserException
	{
		return unescape(str, '\0', 0, str.length(), false).str;
	}

	private static SOPair unescape(String str, char toChar, int startindex,	boolean mustFindChar) throws OBOParserException
	{
		return unescape(str, toChar, startindex, str.length(), mustFindChar);
	}

	private static SOPair unescape(String str, char toChar, int startindex,
			int endindex, boolean mustFindChar) throws OBOParserException
	{
		StringBuilder out = new StringBuilder();
		int endValue = -1;
		for (int i = startindex; i < endindex; i++)
		{
			char c = str.charAt(i);
			if (c == '\\')
			{
				i++;
				c = str.charAt(i);
				Character mapchar = (Character) escapeChars.get(new Character(c));
				if (mapchar == null)
					throw new OBOParserException("Unrecognized escape character \""
							+ c + "\" found.", null, -1);
				out.append(mapchar);
			} else if (c == toChar)
			{
				endValue = i;
				break;
			} else
			{
				out.append(c);
			}
		}
		if (endValue == -1 && mustFindChar)
		{
			throw new OBOParserException("Expected to read a \"" + toChar + "\" but did not find one", str, -1);
		}
		return new SOPair(out.toString(), endValue);
	}

	@SuppressWarnings("unused")
	private static int findUnescaped(String str, char toChar)
	{
		return findUnescaped(str, toChar, 0, str.length());
	}

	private static int findUnescaped(String str, char toChar, int startindex,
			int endindex)
	{
		for (int i = startindex; i < endindex; i++)
		{
			char c = str.charAt(i);
			if (c == '\\')
			{
				i++;
				continue;
			} else if (c == toChar)
			{
				return i;
			}
		}
		return -1;
	}

//	public void readID(String value)
//	{
//		currentID = value;
//	}
//
//	public void readName(String value)
//	{
//		currentName = value;
//	}
//
//	public void readISA(String value)
//	{
//		if (currentStanza == Stanza.TERM)
//			currentParents.add(new ParentTermID(termIDPool.map(new TermID(value,prefixPool)),TermRelation.IS_A));
//	}

	private void readRelationship(String type, String id)
	{
		if (currentStanza == Stanza.TERM)
		{
			TermRelation tr = TermRelation.UNKOWN;
	
			if (type.equals("part_of"))
				tr = TermRelation.PART_OF_A;
			else if (type.equals("regulates"))
				tr = TermRelation.REGULATES;
			else if (type.equals("negatively_regulates"))
				tr = TermRelation.NEGATIVELY_REGULATES;
			else if (type.equals("positively_regulates"))
				tr = TermRelation.POSITIVELY_REGULATES;

			currentParents.add(new ParentTermID(termIDPool.map(new TermID(id,prefixPool)),tr));
		}
	}

	private void readNamespace(String value)
	{
		/* Check if we know the namespace. If not create one */
		Namespace namespace = namespaces.get(value);
		if (namespace == null)
		{
			namespace = new Namespace(value);
			namespaces.put(value,namespace);
		}
		
		currentNamespace = namespace;
	}
	
	
	/**
	 * This reads a synonym from the OBO File. Note that these lines have the form
	 * synonym "NAME" [COMMENT/REF]
	 * Since the Phen-Ontology now uses the synonym field to record different ways that
	 * omim.txt expresses the same feature, we essentially want to remove the quotation remarks
	 * and can disregard the [COMMENT/REF]. This may change later, so do not depend on this.
	 * @param value
	 */
	private void readSynonym(String value)
	{
		int a,b;
		a=0;
		while (value.charAt(a) == '\"')
			a++;

		/* Ignore mis-formated entries */
		if (a != 1)
		{
			logger.info("Ignoring badly formatted synonym \"" + value + "\"");
			return;
		}
		
		b=1;
		while (value.charAt(b) != '\"') b++;
		this.currentSynonyms.add(new String(value.substring(1,b).trim()));
	}

	public String getFormatVersion()
	{
		return format_version;
	}

	public String getDate()
	{
		return date;
	}

	/**
	 * Gives some diagnostics about the parsed obo file
	 * 
	 * @return A String telling you something about the parsed obo file
	 */
	private String getParseDiagnostics()
	{
		StringBuilder diag = new StringBuilder();

		diag.append("Details of parsed obo file:\n");
		diag.append("  filename:\t\t" + this.filename + "\n");
		diag.append("  date:\t\t\t" + this.date + "\n");
		diag.append("  format:\t\t" + this.format_version + "\n");
		diag.append("  term definitions:\t" + this.terms.size());
		
		return diag.toString();
	}
}