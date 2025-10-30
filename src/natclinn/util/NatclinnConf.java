package natclinn.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public final class NatclinnConf { 
	
	private static Properties prop = null;
	private static String filename = "natclinn.properties";
	
	public static String preferredLanguage = null;
	public static String preferredLanguages = null;
	public static String labelProperties = null;
	public static String[] listPreferredLanguages = null;
	public static String[] listLabelProperties = null;
	
	public static String dskForWindows = null;
	public static String mainfolder = null;
	public static String mainFolderNatclinn = null;
	
	public static String fileNameListOntologiesForInitTDB = null;
	public static String fileNameListOntologies = null;
	public static String fileNameListPrimitives = null;
	public static String fileNameListRules = null;
	public static String fileNameListQueries = null;
	public static String fileNameParameters = null;
	public static String fileNameResultsQueries = null;
	
	public static String folderForOntologies = null;
	public static String folderForData = null;
	public static String folderForRules = null;
	public static String folderForQueries = null;
	public static String folderForResults = null;
	public static String folderForScriptR = null;
	public static String fileNameScriptRSf = null;
	
	public static String folderForTDB = null;
	public static String fileNameTDBdatabase = null;
	
	//query prefix
	public static String ncl = null;
	public static String afo = null;
	public static String res = null;
	public static String skos = null;
	public static String skosXL = null;
	public static String vs = null;
	public static String dc = null;
	public static String foaf = null;
	public static String xsd = null;
	public static String owl = null;
	public static String dcterms = null;
	public static String rdfs = null;
	public static String rdf = null;
	public static String geo = null;
	public static String geof = null;
	public static String sf = null;
	public static String sosa = null;
	public static String ssn = null;
	public static String bfo = null;
	public static String om = null;
	public static String agrov = null;
	public static String afv = null;
	public static String wpo = null;
	public static String ncbi = null;
	public static String time = null;
	public static String spatialF = null;
	public static String spatial = null;
	public static String wgs = null;
	public static String sio = null;
	public static String uom = null;

	public static String OFF_API_BASE_URL = null;
	public static String OFF_API_TIMEOUT = null;
	public static String OFF_API_CONNECT_TIMEOUT = null;
	public static String OFF_API_READ_TIMEOUT = null;
	public static String OFF_API_USER_AGENT = null;
	public static String OFF_API_MAX_RETRIES = null;
	public static String OFF_API_RETRY_DELAY_MS = null;
	public static String OFF_API_RATE_LIMIT_MS = null;
	public static String OFF_API_CACHE_EXPIRY_HOURS = null;

	public static String CIQUAL_API_BASE_URL = null;
	public static String CIQUAL_API_TIMEOUT = null;
	public static String CIQUAL_API_CONNECT_TIMEOUT = null;
	public static String CIQUAL_API_READ_TIMEOUT = null;
	public static String CIQUAL_API_USER_AGENT = null;
	public static String CIQUAL_API_MAX_RETRIES = null;
	public static String CIQUAL_API_RETRY_DELAY_MS = null;
	public static String CIQUAL_API_RATE_LIMIT_MS = null;
	public static String CIQUAL_API_CACHE_EXPIRY_HOURS = null;


	
	public static String queryPrefix = null;
	
	public static Logger natclinnTDB_logger;

	/**
	 * Constructor
	*/
	public NatclinnConf() {
		natclinnTDB_logger = LogManager.getLogger(getClass());
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = getClass().getClassLoader().getResourceAsStream(NatclinnConf.filename);
			if (input == null) {
				natclinnTDB_logger.error("No file " + NatclinnConf.filename);
			} else {
				// natclinnTDB_logger.info("Loading file " + NatclinnConf.filename);
			}
			prop.load(input);
			
			preferredLanguage = prop.getProperty("preferredLanguage");
			preferredLanguages = prop.getProperty("preferredLanguages");
			labelProperties = prop.getProperty("labelProperties");
			listPreferredLanguages =  preferredLanguages.split(",", 0);
			listLabelProperties = labelProperties.split(",", 0);
			
			Path pathRoot = Paths.get("/");
			Path pathC = Paths.get("C:/");
			if (pathRoot.toAbsolutePath().startsWith(pathC)) {
				mainfolder = prop.getProperty("dskForWindows") + prop.getProperty("mainfolder");
				Path pathmainfolder = Paths.get(mainfolder).toAbsolutePath().normalize();
				mainfolder = pathmainfolder.toString();
			}
			else {
				mainfolder = prop.getProperty("mainfolder");
				Path pathmainfolder = Paths.get(mainfolder).toAbsolutePath().normalize();
				mainfolder = pathmainfolder.toString();
			}
				
			if (pathRoot.toAbsolutePath().startsWith(pathC)) {
				mainFolderNatclinn = prop.getProperty("dskForWindows") + prop.getProperty("mainFolderNatclinn");
				Path pathmainfolder = Paths.get(mainFolderNatclinn).toAbsolutePath().normalize();
				mainFolderNatclinn = pathmainfolder.toString();
			}
			else {
				mainFolderNatclinn = prop.getProperty("mainFolderNatclinn");
				Path pathmainfolder = Paths.get(mainFolderNatclinn).toAbsolutePath().normalize();
				mainFolderNatclinn = pathmainfolder.toString();
			}
			
			fileNameListOntologiesForInitTDB = prop.getProperty("fileNameListOntologiesForInitTDB");	
			fileNameListOntologies = prop.getProperty("fileNameListOntologies");
			fileNameListPrimitives = prop.getProperty("fileNameListPrimitives");
			fileNameListRules = prop.getProperty("fileNameListRules");
			fileNameListQueries = prop.getProperty("fileNameListQueries");
			fileNameParameters = prop.getProperty("fileNameParameters");
			fileNameResultsQueries = prop.getProperty("fileNameResultsQueries");
			
			folderForOntologies = prop.getProperty("folderForOntologies");
			Path pathfolderForOntologies = Paths.get(mainFolderNatclinn + folderForOntologies);
			folderForOntologies = pathfolderForOntologies.toString();
			
			folderForData = prop.getProperty("folderForData");
			Path pathfolderForData = Paths.get(mainFolderNatclinn + folderForData);
			folderForData = pathfolderForData.toString();

			folderForRules = prop.getProperty("folderForRules");
			Path pathfolderForRules = Paths.get(mainFolderNatclinn + folderForRules);
			folderForRules = pathfolderForRules.toString();
			
			folderForQueries = prop.getProperty("folderForQueries");
			Path pathfolderForQueries = Paths.get(mainFolderNatclinn + folderForQueries);
			folderForQueries = pathfolderForQueries.toString();
			
			folderForResults = prop.getProperty("folderForResults");
			Path pathfolderForResults = Paths.get(mainFolderNatclinn + folderForResults);
			folderForResults = pathfolderForResults.toString();
			
			folderForScriptR = prop.getProperty("folderForScriptR");
			Path pathfolderForScriptR = Paths.get(mainFolderNatclinn + folderForScriptR);
			folderForScriptR = pathfolderForScriptR.toString();
			
			fileNameScriptRSf = prop.getProperty("fileNameScriptRSf");	
			
			folderForTDB = prop.getProperty("folderForTDB");
			Path pathfolderForTDB = Paths.get(mainFolderNatclinn + folderForTDB);
			folderForTDB = pathfolderForTDB.toString();
			fileNameTDBdatabase = prop.getProperty("fileNameTDBdatabase");
			
			ncl = prop.getProperty("ncl");
			afo = prop.getProperty("afo");
			res = prop.getProperty("res");
			skos = prop.getProperty("skos");
			skosXL = prop.getProperty("skosXL");
			vs = prop.getProperty("vs");
			dc = prop.getProperty("dc");
			foaf = prop.getProperty("foaf");
			xsd = prop.getProperty("xsd");
			owl = prop.getProperty("owl");
			dcterms = prop.getProperty("dcterms");
			rdfs = prop.getProperty("rdfs");
			rdf = prop.getProperty("rdf");
			geo = prop.getProperty("geo");
			geof = prop.getProperty("geof");
			sf = prop.getProperty("sf");
			sosa = prop.getProperty("sosa");
			ssn = prop.getProperty("ssn");
			bfo = prop.getProperty("bfo");
			om = prop.getProperty("om");
			agrov = prop.getProperty("agrov");
			afv = prop.getProperty("afv");
			wpo = prop.getProperty("wpo");
			ncbi = prop.getProperty("ncbi");
			time = prop.getProperty("time");
			spatialF = prop.getProperty("spatialF");
			spatial = prop.getProperty("spatial");
			wgs = prop.getProperty("wgs");
			sio = prop.getProperty("sio");
			uom = prop.getProperty("uom");

			OFF_API_BASE_URL = prop.getProperty("OFF_API_BASE_URL");
			OFF_API_TIMEOUT = prop.getProperty("OFF_API_TIMEOUT");
			OFF_API_CONNECT_TIMEOUT = prop.getProperty("OFF_API_CONNECT_TIMEOUT");
			OFF_API_READ_TIMEOUT = prop.getProperty("OFF_API_READ_TIMEOUT");
			OFF_API_USER_AGENT = prop.getProperty("OFF_API_USER_AGENT");
			OFF_API_MAX_RETRIES = prop.getProperty("OFF_API_MAX_RETRIES");
			OFF_API_RETRY_DELAY_MS = prop.getProperty("OFF_API_RETRY_DELAY_MS");
			OFF_API_RATE_LIMIT_MS = prop.getProperty("OFF_API_RATE_LIMIT_MS");
			OFF_API_CACHE_EXPIRY_HOURS = prop.getProperty("OFF_API_CACHE_EXPIRY_HOURS");

			CIQUAL_API_BASE_URL = prop.getProperty("CIQUAL_API_BASE_URL");
			CIQUAL_API_TIMEOUT = prop.getProperty("CIQUAL_API_TIMEOUT");
			CIQUAL_API_CONNECT_TIMEOUT = prop.getProperty("CIQUAL_API_CONNECT_TIMEOUT");
			CIQUAL_API_READ_TIMEOUT = prop.getProperty("CIQUAL_API_READ_TIMEOUT");
			CIQUAL_API_USER_AGENT = prop.getProperty("CIQUAL_API_USER_AGENT");
			CIQUAL_API_MAX_RETRIES = prop.getProperty("CIQUAL_API_MAX_RETRIES");
			CIQUAL_API_RETRY_DELAY_MS = prop.getProperty("CIQUAL_API_RETRY_DELAY_MS");
			CIQUAL_API_RATE_LIMIT_MS = prop.getProperty("CIQUAL_API_RATE_LIMIT_MS");
			CIQUAL_API_CACHE_EXPIRY_HOURS = prop.getProperty("CIQUAL_API_CACHE_EXPIRY_HOURS");
			
			// Prefix pour les query
			
			queryPrefix =  
					"prefix ncl: <" + ncl + ">\n" +
					"prefix afo: <" + afo + ">\n" +	
					"prefix afv: <" + afv + ">\n" +			
					"prefix res: <" + res + ">\n" +
					"prefix skos: <" + skos + ">\n" +
					"prefix skosXL: <" + skosXL + ">\n" +
					"prefix vs: <" + vs + ">\n" +
					"prefix dc: <" + dc + ">\n" +
					"prefix foaf: <" + foaf + ">\n" +
					"prefix xsd: <" + xsd + ">\n" +
					"prefix owl: <" + owl + ">\n" +
					"prefix dcterms: <" + dcterms + ">\n" +
					"prefix rdfs: <" + rdfs + ">\n" +
					"prefix rdf: <" + rdf + ">\n" +
					"prefix geo: <" + geo + ">\n" +
					"prefix geof: <" + geof + ">\n" +
					"prefix sf: <" + sf + ">\n" +
					"prefix sosa: <" + sosa + ">\n" +
					"prefix ssn: <" + ssn + ">\n" +
					"prefix om: <" + om + ">\n" +
					"prefix time: <" + time + ">\n" +
					"prefix spatialF: <" + spatialF + ">\n" +
					"prefix spatial: <" + spatial + ">\n" +
					"prefix wgs: <" + wgs + ">\n" +
					"prefix sio: <" + sio + ">\n" +
					"prefix uom: <" + uom + ">\n" +
					"prefix bfo: <" + bfo + ">\n" ;
							
			NatclinnConf.prop = prop;
			if (input != null) {
				input.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getMyProperty(String key) {
		return (NatclinnConf.prop.getProperty(key));
	}
	
	/**
	 * Update property
	 */
	public boolean setMyProperty(String key, String value) {
		boolean success = false;
		try {
			NatclinnConf.prop.setProperty(key, value);
			URL resource = getClass().getClassLoader().getResource(NatclinnConf.filename);
			BufferedWriter out = new BufferedWriter(new FileWriter(Paths.get(resource.toURI()).toFile()));
			NatclinnConf.prop.store(out, null);
			out.close();
			success = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}
	
}
