package test;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import metabase.TapNode;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import registry.RegistryMark;
import registry.ShortNameBuilder;
import resources.RootClass;
import session.NodeCookie;
import tapaccess.TapAccess;
import translator.XmlToJson;

/**
 * @author laurent
 * @version $Id: AsyncJobTest.java 159 2012-10-10 11:52:54Z laurent.mistahl $
 *
 */
public class ExploreTapRegistry  extends RootClass {


	private static void usage() {
		logger.error("USAGE: AsyncJobTest [url] [query]");
		System.exit(1);
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String xml = System.getProperty("java.io.tmpdir") + "/job.xml";
		String json = System.getProperty("java.io.tmpdir") + "/job.json";
		String node = "http://dc.zah.uni-heidelberg.de/__system__/tap/run/tap/";
		String query = "SELECT ivoid, access_url, res_title\n"
		+ "FROM rr.capability \n"
		+ "  NATURAL JOIN rr.interface\n"
		+ "  NATURAL JOIN rr.resource\n"
//		+ "  NATURAL JOIN rr.table_column\n"
//		+ "  NATURAL JOIN rr.res_table\n"
		+ "WHERE standard_id='ivo://ivoa.net/std/tap' AND intf_type = 'vs:paramhttp' ";
			NodeCookie cookie=new NodeCookie();
		String outFile = TapAccess.runSyncJob(node, query, xml, cookie, null);
		XmlToJson.translateResultTable(xml, json);
		
		BufferedReader br = new BufferedReader(new FileReader(json));
		JSONParser p = new JSONParser();
		JSONObject jsonObject = (JSONObject) p.parse(br);
		JSONArray array = (JSONArray) jsonObject.get("aaData");
		ArrayList<String> results = new ArrayList<String>();
		for( int i=0 ; i<array.size() ; i++) {
			JSONArray sa = (JSONArray) array.get(i);
			System.out.println("");
			String ivoid = (String)sa.get(0);
			String url = (String)sa.get(1);
			String key = ShortNameBuilder.getShortName(ivoid, url);
			System.out.print(key + "\t"+ url );
			String description = (String)sa.get(2);
			TapNode tn=null;
			try {
				String result = url + " " + ivoid + " (" + key + ") ";
				RegistryMark rm = new RegistryMark(key, ivoid, url, description, false, true);
				tn = new TapNode(rm, "/tmp/meta");
				result +=  (tn.supportSyncMode())? "   SYNC  ": "   NOSYNC";
				result +=  (tn.supportAsyncMode())? "   ASYNC  ": "   NOASYNC";
				result += description;
				results.add(result);
			} catch (Exception e) {
				e.printStackTrace();		
				//delete(new File( MetaBaseDir + key));
			}
		}
		for( String s: results) {
			System.out.println(s);
		}

	}


}