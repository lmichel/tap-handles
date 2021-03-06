package test;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONArray;

import registry.RegistryMark;
import resources.RootClass;
import session.Goodies;
import session.NodeCookie;
import tapaccess.JobUtils;
import tapaccess.TapAccess;


public class GoodiesTester extends RootClass {

	public static void testVizier() throws Exception {
		String base = "WebContent/" + WEB_USER_GOODIES_DIR + "/";
		
		Goodies g = new Goodies(base);
		g.processUserList("list_vizier.pos");		
		JSONArray jso;
		jso = g.getJsonContent();
		System.out.println(jso.toJSONString());
		
		String baseUrlN = "http://tapvizier.u-strasbg.fr/TAPVizieR/tap/";
		String urlList = "http://saada.unistra.fr/taphandle/sample/";
		RegistryMark rm = new RegistryMark("vizier", " ", baseUrlN, "test", false, true);
		String urlServ = rm.getFullUrl();
		String inpFileName = "uploadsample.xml";
		String tableName= "\"J/AJ/144/129/refs\"";//"vcds1.\"B/cfht/obscore\"";
		String query = "SELECT TOP 1 * FROM " + tableName + " NATURAL JOIN TAP_UPLOAD.taphandlesample";
		String nodeKey = "testAsync/";		
		String localdir = "/home/michel/Desktop/";		
		//upload(urlServ, inpFileName, urlList, tableName, query);
		uploadAsync(urlServ, nodeKey, query, urlList + inpFileName, localdir, tableName);
	}
	
	public static void testSimbad() throws Exception {
		String base = "WebContent/" + WEB_USER_GOODIES_DIR + "/";
		
		Goodies g = new Goodies(base);
		g.processUserList("list_vizier.pos");		
		JSONArray jso;
		jso = g.getJsonContent();
		System.out.println(jso.toJSONString());
		
		String baseUrlN = "http://simbad.u-strasbg.fr/simbad/sim-tap/";
		String urlList = "http://saada.unistra.fr/taphandle/sample/";
		RegistryMark rm = new RegistryMark("vizier", " ", baseUrlN, "test", false, true);
		String urlServ = rm.getFullUrl();
		String inpFileName = "uploadsample.xml";
		String tableName= "\"J/AJ/144/129/refs\"";//"vcds1.\"B/cfht/obscore\"";
		String query = "SELECT  TOP 100  * FROM public.basic, TAP_UPLOAD.taphandlesample " 
				+ " WHERE      CONTAINS(POINT('ICRS', ra, dec), CIRCLE('ICRS', TAP_UPLOAD.taphandlesample.s_ra, TAP_UPLOAD.taphandlesample.s_dec, 0.016666666666666666)) = 1 ";
		String nodeKey = "testAsync/";		
		String localdir = "/home/michel/Desktop/";		
		//upload(urlServ, inpFileName, urlList, tableName, query);
		uploadAsync(urlServ, nodeKey, query, urlList + inpFileName, localdir, tableName);
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		testSimbad();
		//testVizier();
		
		System.exit(0);
	}
	

	public static void upload(String urlServ, String inpFileName, String urlList, String tableName, String query){
		
		String fileName ="list_pos";
		try {
			TapAccess.runSyncJob(urlServ
				, query
				, "taphandlesample," +  urlList + inpFileName
				, "/tmp/meta/" + "test_vizier.xml"
				, new NodeCookie()
		        , null);
		} catch(Exception e) {
			e.printStackTrace();
			logger.warn(urlServ + " does not support upload (" + e.toString() + ")");
		}
	
	}
	
	
	public static void uploadAsync(String urlServ, String nodeKey, String query, String listUrl, String localdir, String tableName){
		
		String url = urlServ;
		String baseDirectory = localdir;
		String treepath = tableName;
		String statusFileName = baseDirectory + nodeKey + File.separator + "status.xml";
		try {
			validWorkingDirectory(baseDirectory + nodeKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		NodeCookie cookie=new NodeCookie();		
		NodeCookie nodeCookie = new NodeCookie();
		String jobID;
		try {
			jobID = TapAccess.createAsyncJob(url
					, query
					,  "taphandlesample," + listUrl
					, statusFileName
					, nodeCookie
					, null);
			String outputDir = JobUtils.setupJobDir(nodeKey
					, baseDirectory + nodeKey + File.separator + "job_" + jobID + File.separator
			        , statusFileName, treepath);		
			nodeCookie.saveCookie(JobUtils.setupJobDir(nodeKey, outputDir, statusFileName, treepath));
		
			
			
			System.out.println("Create: " + jobID + " " + cookie);
			System.out.println("Run: " + TapAccess.runAsyncJob(urlServ, jobID,  outputDir + "status.xml", cookie)+ " " + cookie);
			String phase = "";
			do {
				phase = TapAccess.getAsyncJobPhase(urlServ, jobID,  outputDir + "phase.xml", cookie);
				Thread.sleep(1000);
				
			} while( phase.equals("EXECUTING"));
			System.out.println("Phase: " + phase+ " " + cookie);
			String[] resultURLs = TapAccess.getAsyncJobResults(urlServ
					, jobID
					, outputDir + "status.xml"
					, cookie);
			
			for( String r: resultURLs) {
					logger.debug("Download " + r);
					TapAccess.getAsyncJobResultFile(r
							, outputDir
							, VOTABLE_JOB_RESULT
							, cookie);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
