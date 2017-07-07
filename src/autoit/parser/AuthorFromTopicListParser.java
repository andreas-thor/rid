package parser;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class AuthorFromTopicListParser {

	
	
	// set of all RIDs
	public HashSet<String> allAttributeNames = new HashSet<String>();
	
	// mapRID2Authors = [ RID -> [ attribute -> value ]] ; attributes are, e.g., name, institution, etc.
	public HashMap<String, HashMap<String, String>> mapRID2Authors = new HashMap<String, HashMap<String, String>>();
	
	// mapTopic2RID = [ Topic -> [ pos -> RID ]] ; pos is the position in the search result (starting with 1)
	public HashMap<String, HashMap<Integer, String>> mapTopic2RID = new HashMap<String, HashMap<Integer, String>>();
	
	
	/**
	 * 
	 * @param args [0]=directory
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		System.out.println("Directory is " + args[0]);
		new AuthorFromTopicListParser(args[0]);
	}
	
	
	public AuthorFromTopicListParser (String dir) throws Exception {
	   	
		super();
        
		
        /* parse RID details */
		ZipFile zipRID = new ZipFile(dir + "rid.zip", Charset.forName("UTF-8"));
		zipRID
        	.stream()
        	.filter (entry -> !entry.isDirectory())
        	.filter (entry -> entry.getName().endsWith(".html"))
        	.forEach(entry -> {
        		try { 
        			parseRIDDetail (zipRID.getInputStream(entry));
        		} catch (Exception e) {
					System.out.println("Exception for " + entry.getName());
					System.out.println(e.toString());
				}});  
		zipRID.close();
		

		
		/* parse all RIDs from topic search*/
		ZipFile zipAuthor = new ZipFile(dir + "authors_by_topic.zip", Charset.forName("UTF-8"));
        zipAuthor
        	.stream()
        	.filter (entry -> !entry.isDirectory())
        	.filter (entry -> entry.getName().endsWith(".txt"))
        	.forEach(entry -> {
        		try { 
        			String[] filePart = entry.getName().toString().split("\\.");
					if (filePart[1].equals("topic")) {
						parseTopicSearchResult(filePart[0], zipAuthor.getInputStream(entry));
					}
        		} catch (Exception e) {
					System.out.println("Exception for " + entry.getName());
					System.out.println(e.toString());
				}});  
        zipAuthor.close();		

        

		
		
		addGender();
		
		
		/* write topic search to CSV */
		// Filename is ".csv.import" so that is has to be imported explicitly into Excel because UTF-8 must be set (standard is ISO)
//		CSVWriter csv1 = new CSVWriter (new OutputStreamWriter(new FileOutputStream(dir + "topic2authors.csv.import"), "UTF-8"));
//		csv1.writeNext(new String[]{"Topic", "Pos", "ResearcherID"});
//		mapTopic2RID.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(topicEntry -> { 
//			topicEntry.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(row -> {
//				csv1.writeNext(new String[]{topicEntry.getKey(), String.valueOf(row.getKey()), row.getValue()});
//			});
//		});
//		csv1.close();
		
		
		
		
		/* generate authors csv */
		String[] attr = Stream.concat(Arrays.stream(new String[]{"ResearcherID", "Name Detail", "Name List", "Gender"}), allAttributeNames.stream().sorted()).distinct().toArray(String[]::new);
		CSVWriter csv2 = new CSVWriter (new OutputStreamWriter(new FileOutputStream(dir + "authors.csv.import"), "UTF-8"));
		csv2.writeNext(attr); 
		mapRID2Authors.forEach((id, map) -> {
			csv2.writeNext(Arrays.stream(attr).map(key -> map.get(key) == null ? "" : map.get(key) ).toArray(String[]::new));
		});
		csv2.close();
		
		
		

	}
	
	
	

	/**
	 * 
	 * @param authorAttributes author attributes
	 * @param addNew true -> author with (so far) unknown RID will be added; false -> update of attributes for existing authors only
	 */
	private void addAuthor (HashMap<String, String> authorAttributes, boolean addNew) {
		if (authorAttributes.get("ResearcherID") != null) {
			if (mapRID2Authors.get(authorAttributes.get("ResearcherID")) == null) {
				if (!addNew) return;
				mapRID2Authors.put(authorAttributes.get("ResearcherID"), new HashMap<String, String>());
			} 
			mapRID2Authors.get(authorAttributes.get("ResearcherID")).putAll(authorAttributes);
			allAttributeNames.addAll(authorAttributes.keySet());
		}
	}
	
	
	
	private void parseTopicSearchResult (String topic, InputStream content) throws Exception {

		String[] lines = new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8)).lines().toArray(String[]::new);
		
		if (mapTopic2RID.get(topic) == null) {
			mapTopic2RID.put(topic, new HashMap<Integer, String>());
			System.out.println("parseTopicSearchResult: Topic " + topic + ": " + lines[13]);	
		}
		
		for (int i=18; i<lines.length-1; i+=2) {
			String[] col = (lines[i] + lines[i+1]).split("\t");
			if (col[0].matches("^\\d+.*")) {
				
				HashMap<String, String> result = new HashMap<String, String>();
				result.put("Name List", col[1].trim());
				result.put("Institution(s)", col[2].trim());
				result.put("Country/Territory", col[3].trim());
				result.put("ResearcherID", col[4].trim());
				result.put("Keywords", col[5].trim());
				result.put("Other Names", col[6].trim());
				
				addAuthor(result, false);	// false = we do not include new authors here; true = we do include
//				addAuthor(result, col[4].trim().equals("A-9872-2008") || col[4].trim().equals("G-9978-2015"));
				
				mapTopic2RID.get(topic).put(Integer.valueOf(col[0].substring(0, col[0].indexOf('.'))), col[4].trim());
			}
		}
		
	}
	
	
	private void parseRIDDetail (InputStream content) throws Exception {

		HashMap<String, String> authorAttributes = new HashMap<String, String>();
		
		Document doc = Jsoup.parse(content, "UTF-8", "");
		Element name = doc.select("div.profileName").first();
		if (name==null) throw new Exception("div.profileName not found");
		
		
		authorAttributes.put("Name Detail", name.text().trim());
		
		for (String h: new String[]{ "table.profileTable tr", "table.profileTableInst tr", "table.profileTableDesc tr"}) {
			Elements rows = doc.select(h);
			for (Element r: rows) {
				
				String key=null, value=null;
				
				for (Node n: r.childNodes()) {
					
					if (n.attr("class").equals("profileFieldLabel")) {
						key = n.childNode(0).toString().trim();
						key = key.length()>2 ? key.substring(0,  key.length()-1) : null;
					}
					if (n.attr("class").equals("profileDataCells")) {
						value = Jsoup.parse(n.childNode(0).toString()).text();
					}
					
				}
				
				if ((key!=null) && (value!=null)) {
					authorAttributes.put(key, value);
				}
			}
		}
		
		addAuthor(authorAttributes, true);	// false = we do not include new authors here; true = we do include
		
	}
	
	
	private void addGender (/*String dataFile*/) throws IOException {
		
		
		System.out.print("Reading gender file ... ");
		CSVReader csv = new CSVReader (new InputStreamReader(getClass().getResourceAsStream("gender_name_dictionary/wgnd_ctry.csv")));
		
		HashMap<String, String> mapFirstName2Gender = new HashMap<String, String>();
		Iterator<String[]> it = csv.iterator();
		it.next();
		String lastName = "";
		int count[] = new int[3];
		while (it.hasNext()) {
			String[] line = it.next();
			
			if (!lastName.equals(line[0])) { 
				if (!lastName.equals("")) {

					String gender = "?";
					if (count[0]>count[1]) gender = "M??";  
					if (count[0]<count[1]) gender = "F??";  
					if ((count[0] >0) && (count[1]==0)) gender = "M?";  
					if ((count[0]==0) && (count[1] >0)) gender = "F?";  
					if ((count[0] >0) && (count[1]==0) && (count[2]==0)) gender = "M"; 
					if ((count[0]==0) && (count[1] >0) && (count[2]==0)) gender = "F";  
					mapFirstName2Gender.put(lastName, gender);		
				}
				count = new int[]{ 0, 0, 0};
			}

			switch (line[2]) { 
				case "M": count[0]++; break;
				case "F": count[1]++; break;
				case "?": count[2]++; break;
			}
			
			lastName = line[0];
		}
		
		csv.close();
		System.out.print("done.");

		
		int found[] = { 0, 0};
		
		
		mapRID2Authors.forEach((id, attributes) -> {
			found[1]++;
//			String name = attributes.get("Name");
			for (String name: new String[]{attributes.get("Name List"), attributes.get("Name Detail")}) {
			
				if ((name != null) && (name.length()>0)) {
					String split[] = Arrays.stream(name.toUpperCase().trim().split(" ")).map(s -> s.trim()).filter(s -> s.length()>1).filter(s -> '.' != s.charAt(s.length()-1)).toArray(String[]::new);
					
					
					if (split.length==0) continue;	// next in "for (String:name)" loop
					String checkName = split[split.length-1];
					int pos = split.length-1;
					do {
						// check --> break
						String gender = mapFirstName2Gender.get(checkName);
						if (gender != null) {
							mapRID2Authors.get(id).put("Gender", gender);
							found[0]++;
							return;		// next in "mapRID2Authors.forEach" loop
						}
						
						pos--;
						if (pos<0) break;	
						checkName = split[pos] + " " + checkName;
					} while (true);
					
						
					
					
				}
			}
			
		});
		
		allAttributeNames.add("Gender");
		System.out.println(String.format("gender found for %d of %d authors.", found[0], found[1]));
		
		
	}


}
