package org.monarchinitiative.boom.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.HashBiMap;

/**
 * Collect and merge prefix mappings for CurieMaps.
 * See also: https://github.com/prefixcommons/curie-util
 * 
 * @author yy20716 (HyeongSik Kim)
 */
public class CurieMapGenerator {
	private static Logger LOG = Logger.getLogger(CurieMapGenerator.class);
	private String monarchURL = "https://archive.monarchinitiative.org/latest/ttl/";
	private String COMMONPREFIXURL = "http://prefix.cc/popular/all.file.csv";
	private String OBOPREFIXYAMLURL = "http://obofoundry.org/registry/ontologies.yml";
	private String tempDir = System.getProperty("java.io.tmpdir");
	private String curieMapSerFile = tempDir + File.separatorChar + "curiemap.ser";
	private Map<String, String> curieMap = HashBiMap.create();

	private static CurieMapGenerator  instance = null;
	protected CurieMapGenerator () {
		/* Exists only to defeat instantiation. */
	}
	public static CurieMapGenerator getInstance() {
		if(instance == null) {
			instance = new CurieMapGenerator ();
			instance.generate();
		}
			
		return instance;
	}

	public Map<String, String>getCurieMap() {
		return curieMap;
	}

	public boolean generate() {
		try {
			curieMap.clear();

			File f = new File(curieMapSerFile);
			if(f.exists() && !f.isDirectory()) {
				LOG.info("It seems that we already have a pre-built curieMap file.");
				deserializeMap();
				return true;
			}

			LOG.info("Begin collecting prefix mappings...");
			scrapFromMonarchDatasets(curieMap);
			scrapFromOboFoundary(curieMap);
			scrapFromPrefixCC(curieMap);
			LOG.info("Collecting prefix mappings is finished...: # of the collected prefixes: " + curieMap.size());
			serializeMap();

			return true;
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}

		return false;
	}

	private void serializeMap() throws IOException {
		ObjectOutputStream oos = null;
		FileOutputStream fout = null;

		try{
			FileUtils.deleteQuietly(new File(curieMapSerFile));
			fout = new FileOutputStream(curieMapSerFile, true);
			oos = new ObjectOutputStream(fout);
			oos.writeObject(curieMap);
		} catch (Exception e) {
			LOG.error(e.getMessage());
		} finally {
			if(oos  != null){
				oos.close();
			} 
		}
	}

	@SuppressWarnings("unchecked")
	private void deserializeMap() throws IOException {
		ObjectInputStream objectinputstream = null;
		try {
			FileInputStream streamIn = new FileInputStream(curieMapSerFile);
			objectinputstream = new ObjectInputStream(streamIn);
			curieMap = (Map<String, String>) objectinputstream.readObject();
		} catch (Exception e) {
			LOG.error(e.getMessage());
		} finally {
			if(objectinputstream != null){
				objectinputstream .close();
			} 
		}
	}

	/**
	 * Scrap 1MB from each turtle file listed in Monarch repository to extract and build prefix mappings for CurieUtil 
	 * @param curieMap
	 */
	private void scrapFromMonarchDatasets(Map<String, String> curieMap) {
		try {
			/* Looking for @prefix lines in the header of ttl files */
			String regex = "^@prefix\\s*([A-Za-z\\-]+):\\s*<(.*)>";
			int maxDownload = 1024*1024*1;

			Pattern regexPattern = Pattern.compile(regex, Pattern.MULTILINE);
			Document doc = Jsoup.connect(monarchURL).get();

			for (Element file : doc.select("a")) {
				URL website = new URL(monarchURL + file.attr("href"));
				byte[] outBytes = new byte[maxDownload];
				InputStream stream = website.openStream();
				IOUtils.read(stream, outBytes, 0, maxDownload);
				String ttlDumpStr = new String(outBytes);
				Matcher match = regexPattern.matcher(ttlDumpStr);

				while (match.find()) {
					try {
						String key = match.group(1).trim();
						String value = match.group(2).trim();
						curieMap.put(key, value);
						// LOG.info("key: " + key + " value: " + value);
					} catch (IllegalArgumentException e) {}
				}
			}

			// LOG.info("");
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}

	/**
	 * Scrap YAML file from OBOFoundary to build prefix (bi-)mappings for CurieUtil.
	 * @param curieMap
	 */
	@SuppressWarnings({ "rawtypes", "unchecked"})
	private void scrapFromOboFoundary(Map<String, String> curieMap) {
		try {
			File oboJSONMappingFile = new File(tempDir + File.separatorChar + "ontologies.yaml");
			FileUtils.deleteQuietly(oboJSONMappingFile);
			oboJSONMappingFile.deleteOnExit();
			FileUtils.copyURLToFile(new URL(OBOPREFIXYAMLURL), oboJSONMappingFile);
			InputStream inputStream = new FileInputStream(oboJSONMappingFile.getAbsolutePath());

			Yaml yaml = new Yaml();
			Map<String, ArrayList> yamlParsers = (Map<String, ArrayList>) yaml.load(inputStream);
			ArrayList ontologyList = (ArrayList) yamlParsers.get("ontologies");

			for (int i = 0; i < ontologyList.size(); i++) {
				Map ontologyMetaInfo = (Map<String, ArrayList>) ontologyList.get(i);
				String url = (String) ontologyMetaInfo.get("ontology_purl");

				if (url == null) continue;

				int dotLastIndex =url.lastIndexOf(".");
				int slashLastIndex =url.lastIndexOf("/");
				String purl_head = url.substring(slashLastIndex+1, dotLastIndex);
				String purl_body = url.substring(0, slashLastIndex + 1) + purl_head.toUpperCase();

				try {
					curieMap.put(purl_head , purl_body);
				} catch (IllegalArgumentException e) {}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}

	/**
	 * 	Scrap CSV file from Prefix.cc to build prefix (bi-)mappings for CurieUtil.
	 * @param curieMap
	 */
	@SuppressWarnings("resource")
	private void scrapFromPrefixCC(Map<String, String> curieMap) {
		try {
			File commonJSONMappingFile = new File(tempDir + File.separatorChar + "all.file.csv");
			FileUtils.deleteQuietly(commonJSONMappingFile);
			commonJSONMappingFile.deleteOnExit();
			FileUtils.copyURLToFile(new URL(COMMONPREFIXURL), commonJSONMappingFile);

			String line = "";
			String cvsSplitBy = ",";
			BufferedReader br = new BufferedReader(new FileReader(commonJSONMappingFile));
			while ((line = br.readLine()) != null) {
				String[] lineArr = line.split(cvsSplitBy);
				try {
					curieMap.put(lineArr[0], lineArr[1]);
				} catch (IllegalArgumentException e) {}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}
}