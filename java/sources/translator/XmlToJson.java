/**
 * 
 */
package translator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import cds.savot.model.InfoSet;
import cds.savot.model.SavotInfo;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotVOTable;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import metabase.DataTreePath;
import metabase.TapNode;
import net.sf.saxon.lib.FeatureKeys;
import resources.RootClass;
import tapaccess.TapException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;


/**
 * Utility in charge of translating VOTable received from TAP node n JSONB files
 * comprehensible by the AJAX client.
 * This utility has no connection with the application resources
 * @author laurent
 * @version $Id$
 * 
 * 04/2012; Set MAX_ROWS field with 10000 as value
 * 05/2012: Display arrays of atomics
 * 10/1012: Support per-table access for vizier
 */
public class XmlToJson  extends RootClass {

	/**
	 * Replace in a style sheet the vosi name space with the name space given in parameter
	 * @param baseDir      Working directory
	 * @param service      Either availability, capabilities or tables
	 * @param nsDefinition Name space to use
	 * @throws Exception   If something goes wrong
	 */
	public static void setVosiNS(String baseDir , String service, NameSpaceDefinition nsDefinition) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(styleDir + service + ".xsl"));
		BufferedWriter out = new BufferedWriter(new FileWriter(baseDir + service + ".xsl"));
		String str;
		boolean found = false;
		String nsName = nsDefinition.getNsName();
		String nsDeclaration = nsDefinition.getNsDeclaration();
		if( nsName != null && nsName.length() == 0 ){
			nsName = null;
		}
		while ((str = in.readLine()) != null) {
			if( !found && nsDefinition != null && nsDeclaration != null) {
				Matcher m = NSDefPattern.matcher(str);
				if (m.matches()) {
					str = str.replace( m.group(1), nsDeclaration) ;
					found = true;
				}
			}
			if( nsName != null  ) {
				str = str.replaceAll("vosi\\:", nsName + ":");
			}
			out.write(str + "\n");
		}
		in.close();
		out.close();
	}
	/**
	 * Replace in a style sheet the detected name space with the name space given in parameter
	 * @param baseDir      Working directory
	 * @param service      Either availability, capabilities or tables
	 * @param nsDefinition Name space to use
	 * @throws Exception   If something goes wrong
	 */
	public static void setNS(String baseDir , String service, NameSpaceDefinition nsDefinition) throws Exception {

		BufferedReader in = new BufferedReader(new FileReader(styleDir + service + ".xsl"));
		BufferedWriter out = new BufferedWriter(new FileWriter(baseDir + service + ".xsl"));
		String str;
		boolean found = false;
		while ((str = in.readLine()) != null) {
			if( !found && nsDefinition != null ) {
				Matcher m = NSPattern.matcher(str);
				if (m.matches()) {
					str = str.replace( m.group(1), nsDefinition.getNsDeclaration()) ;
					found = true;
				}
			}
			out.write(str + "\n");
		}
		in.close();
		out.close();
	}

	/**
	 * Translate the XML file service.xml into service.json by using the style sheet service.xsl
	 * @param baseDir      Working directory
	 * @param service      Either availability, capabilities or tables
	 * @param nsDefinition Name space to use
	 * @throws Exception   If something goes wrong
	 */
	public static void translate(String baseDir , String service, NameSpaceDefinition nsDefinition) throws Exception {
		logger.debug("Translate " +  service + ".xml with "  + service + ".xsl in " + baseDir);
		setVosiNS(baseDir, service, nsDefinition);
		applyStyle(baseDir + service + ".xml", baseDir + service + ".json", baseDir + service + ".xsl");
	}

	/**
	 * Translate the XML file service.xml into service.json by using the style sheet style.xsl
	 * The table attributes are extracted from the file tablesFile. The .xml suffix is implicit.
	 * @param baseDir      Working directory
	 * @param service      Either availability, capabilities or tables
	 * @param style        Style sheet name 
	 * @param nsDefinition Name space to use
	 * @throws Exception   If something goes wrong
	 */
	public static void translate(String baseDir , String service, String style, NameSpaceDefinition nsDefinition) throws Exception {
		logger.debug("Translate " +  service + ".xml with "  + style + ".xsl");
		setVosiNS(baseDir, style, nsDefinition);
		applyStyle(baseDir + service + ".xml", baseDir + style + ".json", baseDir + style + ".xsl");
	}

	/**
	 * Builds a JSON file describing the table tableName in a format 
	 * comprehensible by JQuery datatable widget
	 * @param baseDir      Working directory
	 * @param tablesFile  Name of the file containing all table metadata
	 * @param tableName  Name of the table
	 * @param nsDefinition Name space to use
	 * @throws Exception If something goes wrong
	 */
	public static void translateTableMetaData(String baseDir , String tablesFile, DataTreePath dataTreePath, NameSpaceDefinition nsDefinition) throws Exception {
		setVosiNS(baseDir, "table1.0", nsDefinition);
		String tableFileName = dataTreePath.getEncodedFileName();
		String filename = baseDir + "table1.0.xsl";
		Scanner s = new Scanner(new File(filename));
		PrintWriter fw = new PrintWriter(new File( baseDir + tableFileName + ".xsl"));
		while( s.hasNextLine() ) {
			String b = s.nextLine();
			String s2  = b.replaceAll("TABLENAME", dataTreePath.getTable()).replaceAll("SCHEMA", dataTreePath.getSchema());
			fw.println(s2);
		}
		s.close();
		fw.close();
		applyStyle(baseDir  + tablesFile + ".xml", baseDir + tableFileName + ".json", baseDir + tableFileName + ".xsl");
		File out = new File(baseDir + tableFileName + ".json");
		/*
		 * If the translation failed, try with the no schema style (astrogrid)
		 */
		if( !out.exists() || out.length() == 0 ){
			logger.info("Translate table description with a noschema style (astrogrid)");
			setVosiNS(baseDir, "table_noschema", nsDefinition);
			String styleName = baseDir + "table_noschema.xsl";
			s = new Scanner(new File(styleName));
			fw = new PrintWriter(new File( baseDir + tableFileName + ".xsl"));
			while( s.hasNextLine() ) {
				String s2 = s.nextLine().replaceAll("TABLENAME", dataTreePath.getTable()).replaceAll("SCHEMA", dataTreePath.getSchema());
				fw.println(s2);
			}
			s.close();
			fw.close();
			applyStyle(baseDir  + tablesFile + ".xml", baseDir + tableFileName + ".json", baseDir + tableFileName + ".xsl");
		}

	}

	/**
	 * Builds a JSON file describing the table tableName in a format 
	 * comprehensible by JQuery datatable widget
	 * The XML table description is supposed to be in a file names  tableName.xml
	 * @param baseDir      Working directory
	 * @param dataTreePath  Descriptor of tyhe data node
	 * @param nsDefinition Name space to use
	 * @param vosiLevel    1.0: format similar to /tables but with just one table, 1.1 see VOSI 1.1
	 * @throws Exception If something goes wrong
	 */
	public static void translateTableMetaData(String baseDir , DataTreePath dataTreePath, NameSpaceDefinition nsDefinition, String vosiLevel) throws Exception {
		setVosiNS(baseDir, "table" + vosiLevel, nsDefinition);
		String filename = baseDir + "table" + vosiLevel + ".xsl";
		String tableFileName = dataTreePath.getEncodedFileName();
		Scanner s = new Scanner(new File(filename));
		PrintWriter fw = new PrintWriter(new File( baseDir + tableFileName + ".xsl"));
		while( s.hasNextLine() ) {
			fw.println(s.nextLine().replaceAll("TABLENAME", dataTreePath.getTable()).replaceAll("SCHEMA", dataTreePath.getSchema()));
		}
		s.close();
		fw.close();
		applyStyle(baseDir  + tableFileName + ".xml", baseDir + tableFileName + ".json", baseDir + tableFileName + ".xsl");
	}

	/**
	 * Builds a JSON file describing the table tableName to setup query form.
	 * The table attributes are extracted from the file tablesFile. The .xml suffix is implicit.
	 * If the regular styme fails, we try the noschema style of the Astrogrid nodes
	 * @param baseDir      Working directory
	 * @param tablesFile  Name of the file containing all table metadata
	 * @param dataTreePath  descriptor of the data tree path (schema.table)
	 * @param nsDefinition Name space to use
	 * @throws Exception If something goes wrong
	 */
	public static void translateTableAttributes(String baseDir, String tablesFile, DataTreePath dataTreePath, NameSpaceDefinition nsDefinition) throws Exception {
		setVosiNS(baseDir, "table_att", nsDefinition);
		String filename = baseDir + "table_att.xsl";
		Scanner s = new Scanner(new File(filename));
		String tableFileName = dataTreePath.getEncodedFileName();
		String tablePrefix =  baseDir + tableFileName + "_att";
		PrintWriter fw = new PrintWriter(new File( tablePrefix + ".xsl"));
		//Must use the full path because one can have tables with the name but in different schemas
		String qs = dataTreePath.getSchema() + "." + dataTreePath.getTable();
		String sn = dataTreePath.getSchema();
		while( s.hasNextLine() ) {
			String b = s.nextLine();
			String s2 = b.replaceAll("TABLENAME", qs).replaceAll("SCHEMA", sn);
			fw.println(s2);
		}
		s.close();
		fw.close();
		applyStyle(baseDir  + tablesFile + ".xml", tablePrefix + ".json", tablePrefix + ".xsl");
		File out = new File(tablePrefix + ".json");
		/*
		 * If the translation failed, try with the no schema style (astrogrid)
		 */
		if( !out.exists() || out.length() == 0 ){
			logger.info("Translate table att with a noschema style (astrogrid)");
			qs = dataTreePath.getTable();
			sn = dataTreePath.getSchema();
			setVosiNS(baseDir, "table_att_noschema", nsDefinition);
			String styleName = baseDir + "table_att_noschema.xsl";
			s = new Scanner(new File(styleName));
			fw = new PrintWriter(new File( tablePrefix + ".xsl"));
			while( s.hasNextLine() ) {
				String s2 = s.nextLine().replaceAll("TABLENAME", qs).replaceAll("SCHEMA", sn);
				fw.println(s2);
			}
			s.close();
			fw.close();
			applyStyle(baseDir  + tablesFile + ".xml", tablePrefix + ".json", tablePrefix + ".xsl");
		}
	}

	/**
	 * Builds a JSON file describing the table tableName to setup query form.
	 * The XML table description is supposed to be in a file names  tableName_att.xml
	 * @param baseDir      Working directory
	 * @param dataTreePath  descriptor of the data tree path (schema.table)
	 * @param nsDefinition Name space to use
	 * @throws Exception If something goes wrong
	 */
	public static void translateTableAttributes(String baseDir , DataTreePath dataTreePath, NameSpaceDefinition nsDefinition) throws Exception {
		setVosiNS(baseDir, "table_att", nsDefinition);
		String filename = baseDir + "table_att.xsl";
		Scanner s = new Scanner(new File(filename));
		String tableFileName = dataTreePath.getEncodedFileName();
		PrintWriter fw = new PrintWriter(new File( baseDir + tableFileName + "_att.xsl"));
		String qs = dataTreePath.getTable();
		while( s.hasNextLine() ) {
			fw.println(s.nextLine().replaceAll("TABLENAME", qs).replaceAll("SCHEMA", dataTreePath.getSchema()));
		}
		s.close();
		fw.close();
		applyStyle(baseDir  + tableFileName + "_att.xml", baseDir + tableFileName + "_att.json", baseDir + tableFileName + "_att.xsl");
	}

	/**
	 * Translate a TAP query response in a JSON file with a format 
	 * comprehensible by JQuery datatable widget
	 * @param inputFile   XML file to translate
	 * @param outputFile  Output JSON file
	 * @throws Exception  If something goes wrong
	 */
	@SuppressWarnings("unchecked")
	public static void translateResultTable(String inputFile, String outputFile  ) throws Exception {
		StarTableFactory stf = new StarTableFactory();
		logger.info("Translate " +inputFile);

		try {
			StarTable table = stf.makeStarTable(inputFile); 
			JSONObject retour = new JSONObject();
			int nSrc = (int) table.getRowCount();
			int nCol = table.getColumnCount();

			ColumnInfo[] ci = Tables.getColumnInfos(table);
			JSONArray aoColumns = new JSONArray();
			for (int i=0; i < nCol; i++) {
				JSONObject aoColumn = new JSONObject();
				aoColumn.put("sTitle", ci[i].getName());
				aoColumns.add(aoColumn);
			}
			retour.put("aoColumns", aoColumns);

			JSONArray aaData = new JSONArray();
			for( int r=0 ; r<nSrc ; r++ ) {
				if( r >= MAX_ROWS ) {
					logger.warn("JSON result truncated to " +  MAX_ROWS);
					break;
				}
				//				if( (r%100) == 0 ) {
				//					long mb = 1024*1024;
				//			        Runtime runtime = Runtime.getRuntime();
				//					long max = runtime.maxMemory()/mb;
				//					long tot = runtime.totalMemory()/mb;
				//					long free = runtime.freeMemory()/mb;
				//					System.out.println( "      -- " + max  + " " +  tot  + " " +  free);
				//					if( max == tot ) {
				//						double rt = (double)free/(double)max;
				//						System.out.println("RT " + rt);
				//						if( rt < 0.5 ) {
				//							logger.warn("Result truncated to " + r + " rows to avoid memory overflow");
				//							throw new TapException("No enough memory space to parse " + inputFile);
				//						}
				//					}		
				//				}
				Object[] o = table.getRow(r);
				JSONArray rowData = new JSONArray();
				for (int i = 0; i < nCol; i++) {
					Object obj = o[i];

					if(obj == null ) {
						rowData.add("null");
						/*
						 * Array annotation removed from server because of CSIRO for which all data are typed as array
						 */
					} else 	if(obj.getClass().isArray()) {
						int arraylLength = Array.getLength(obj);
						if( arraylLength > 1 ) {
							String v = "";
							for( int p=0 ; p<arraylLength ; p++ ){
								Object cell = Array.get(obj, p);
								if( cell.getClass().isArray()) {
									v += "[";
									for( int q=0 ; q<Array.getLength(cell) ; q++ ){
										v += Array.get(cell, i) + " ";
									}
									v += "] "; 
								} else {
									v += cell + " ";
								}
							}
							//rowData.add("Array[" + v + "]");	
							rowData.add(v);	
						} else if( arraylLength == 1 ){
							Object cell = Array.get(obj, 0);
							rowData.add( cell.toString() ) ;
						} else {
							rowData.add("");				
						}
					} else {
						rowData.add(obj.toString());				
					}
				}
				aaData.add(rowData);
			}
			retour.put("aaData", aaData);

			FileWriter fw = new FileWriter(outputFile);
			fw.write(retour.toJSONString());
			fw.close();
			/*
			 * If an error occurs while StartTable building, we suppose that the VO table contains 
			 * some info about the issue
			 */
		} catch (OutOfMemoryError m) {	
			throw new TapException("No enough memory space to parse " + inputFile);

		} catch (Exception e) {
			logger.error("Can't translate result file " + inputFile);
			SavotPullParser  sp = new SavotPullParser(inputFile, SavotPullEngine.FULL);
			SavotVOTable sv = sp.getVOTable(); 
			long rc =  sp.getResourceCount();
			for (int l=0 ; l<rc ;) {		    	 
				SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
				InfoSet is = currentResource.getInfos();
				String msg = "Info returned by the server:\n";;
				for( int i=0 ; i<is.getItemCount() ; i++ ) {
					msg += ((SavotInfo)is.getItemAt(i)).getValue() + " " + ((SavotInfo)is.getItemAt(i)).getContent() + "\n";
				}
				throw new TapException(msg);
			}         
			throw new TapException("No resource in VOTable " + inputFile);
		}
	}

	/**
	 * Translate a TAP query response joining key and key_columns tables into a set of JSON files.
	 * There is one JSON file per source table. These files are used by the client to handle queries with JOINs
	 * The input file is supposed to have ' columns: source_table, target_table, source_column, target_columns
	 * @param inputFile   XML file to translate
	 * @param outputDir   Output dir
	 * @throws Exception  If something goes wrong
	 */
	@SuppressWarnings("unchecked")
	public static void translateJoinKeysTable(String inputFile, String outputDir  ) throws Exception {
		//Map<String, Collection<JSONObject>> map = new LinkedHashMap<String, Collection<JSONObject>>();
		JoinKeyMap map = new JoinKeyMap();
		StarTableFactory stf = new StarTableFactory();
		logger.info("Translate " +inputFile);
		try {
			StarTable table = stf.makeStarTable(inputFile); 
			int nSrc = (int) table.getRowCount();
			int nCol = table.getColumnCount();

			if( nCol != 4 ) {
				throw new TapException("Key join table must have 4 columns");				
			}
			/*
			 * Stores join links in a map 
			 */
			for( int r=0 ; r<nSrc ; r++ ) {
				map.addJoin(table.getRow(r));
				//				Object[] o =table.getRow(r);
				//				String source_table = o[0].toString();
				//
				//				JSONObject jso = new JSONObject();
				//				jso.put("target_table", o[1].toString());
				//				jso.put("source_column", o[2].toString());
				//				jso.put("target_column", o[3].toString());
				//				Collection<JSONObject> set;
				//				if( (set = map.get(source_table)) == null) {
				//					set =  new ArrayList<JSONObject>();
				//					map.put(source_table,set);
				//				}
				//				set.add(jso);
			}
			map.buildReverseJoins();
			/*
			 * Builds individual JSON files for each table
			 */
			for( Entry<String, Collection<JSONObject>> e: map.entrySet() ){
				String source_table = e.getKey().toString(); 
				Collection<JSONObject> targets = e.getValue();

				JSONObject jso = new JSONObject();
				JSONArray jsa  = new JSONArray();
				jso.put("source_table", source_table);
				jso.put("targets", jsa);

				for( JSONObject target:targets) {
					jsa.add(target);
				}
				DataTreePath dataTreePath = new DataTreePath(source_table);
				String file = 	outputDir + File.separator + dataTreePath.getEncodedFileName() + "_joinkeys"	;
				//	logger.info("Write joinkey file " + file);
				FileWriter fw = new FileWriter(file+ ".json");
				fw.write(jso.toJSONString());
				fw.close();	
				TapNode.setDataTreePathInJsonResponse(file, dataTreePath);
			}
			/*
			 * If an error occurs while StartTable building, we suppose that the VO table contains 
			 * some info about the issue
			 */
		} catch (Exception e) {
			SavotPullParser  sp = new SavotPullParser(inputFile, SavotPullEngine.FULL);
			SavotVOTable sv = sp.getVOTable(); 
			String msg = e.getMessage() + "\n";;
			for (int l = 0; l<sp.getResourceCount(); l++) {		    	 
				SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
				InfoSet is = currentResource.getInfos();
				for( int i=0 ; i<is.getItemCount() ; i++ ) {
					msg += ((SavotInfo)is.getItemAt(i)).getContent() + "\n";
				}
			}   
			throw new TapException(msg);
		}
	}


	/**
	 * Apply a style sheet to an XML file
	 * The translation used the saxon engine in order to work with a generic JSON translator
	 * courtesy of {@link http://www.saxonica.com/download/download_page.xml}
	 * @param inputFile   File path to translate
	 * @param outputFile  Translated file path
	 * @param styleSheet  Applied style sheet
	 * @throws Exception  If something goes wrong
	 */
	public static void applyStyle(String inputFile, String outputFile, String styleSheet) throws Exception{
		logger.debug("Apply style to " + inputFile);
		logger.debug("   Style sheet " + styleSheet);
		TransformerFactory tfactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
		tfactory.setErrorListener(new EcouteurDErreurs());

		tfactory.setAttribute(FeatureKeys.COMPILE_WITH_TRACING, true);
		//		File moduleFile = new File
		//		(net.sf.saxon.TransformerFactoryImpl.class.getProtectionDomain()
		//				.getCodeSource().getLocation().toURI());

		StreamSource ss = new StreamSource(new File(styleSheet));
		// Create a transformer for the stylesheet.
		Transformer transformer = tfactory.newTransformer(ss);
		transformer.transform(new StreamSource(new File(inputFile)),
				new StreamResult(new File(outputFile)));	
	}



}
