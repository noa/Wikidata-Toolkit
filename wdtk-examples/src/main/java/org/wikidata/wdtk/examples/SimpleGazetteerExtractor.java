package org.wikidata.wdtk.examples;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SimpleGazetteerExtractor implements EntityDocumentProcessor {

	int nprocessed = 0;
	
	static final ImmutableMap<String, String> type_label_map =
			new ImmutableMap.Builder<String,String>()
			.put("Q1970365", "nat_hist_museaum")
			.put("Q33506", "museum")
			.put("Q13372", "university")
			.put("Q11707", "restaurant")
			.put("Q2977", "cathedral")
			.put("Q16970", "church")
			.put("Q16917", "hospital")
			.put("Q1248784", "airport")
			.put("Q5741069", "rock_band")
			.put("Q215380", "band")
			.put("Q5", "person") 
			.put("Q6256", "country") 
			.put("Q486972", "settlement")
			.put("Q515", "city")
			.put("Q107390", "fed_state")
			.put("Q7275", "state")
			.put("Q82794", "region")
			.put("Q5107", "continent")
			.put("Q23397", "lake")
			.put("Q8502", "mountain")
			.put("Q2319498", "landmark")
			.put("Q5003624", "memorial")
			.put("Q1187960", "monument")
			.put("Q12280", "bridge")
			.put("Q9430", "ocean")
			.put("Q165", "sea")
			.put("Q11303", "skyscraper")
			.put("Q171809","county")
			.put("Q34442","road")
			.put("Q41176","building")
			.put("Q1802963","mansion")
			.put("Q8514","desert")
			.put("Q624232","us_landmark")
			.put("Q811979","structure")
			.put("Q43229","organization")
			.put("Q891723","public_company")
			.put("Q161726","multinat")
			.put("Q327333","gov_agency")
			.put("Q783794","company")
			.put("Q14638071","special_org")
			.put("Q31855","research_inst")
			.put("Q66344","central_bank")
			.put("Q22687","bank")
			.put("Q11691","stoch_exchange")
			.put("Q11032","newspaper")
			.put("Q192283","news_agency")
			.put("Q48204","assoc")
			.put("Q431603","advocacy")
			.put("Q46970","airline")
			.put("Q2401749","telecom")
			.put("Q375928","lower_chamber")
			.put("Q637846","upper_house")
			.put("Q7278","political_party")
			.put("Q245065", "intergov_org")
			.put("Q1346006", "sport_gov")
			.put("Q2085381", "publisher")
			.put("Q79913", "ngo")
			.put("Q163740", "non_profit")
			.put("Q12973014", "sports_team")
			.put("Q1616075", "tv_station")
			.put("Q190752", "supreme_court")
			.put("Q41487", "court")
			.put("Q3220391", "social_network")
			.put("Q1197685", "holiday")
			.put("Q132241", "festival")
			.put("Q1656682", "event")
			.put("Q103495", "world_war")
			.put("Q198", "war")
			.put("Q182832", "concert")
			.put("Q15056993", "aircraft_family")
			.put("Q11446", "ship")
			.put("Q786820", "auto_manufactorer")
			.put("Q15142889", "weapon_family")
			.build();
	
	class GazetteerEntry {
		public ItemIdValue id;
		public String name;
		public SiteLink link;
		public List<MonolingualTextValue> aliases;
		
		public GazetteerEntry(ItemIdValue id, String name,  List<MonolingualTextValue> aliases, SiteLink link) {
			this.id = id;
			this.name = name;
			this.aliases = aliases;
			this.link = link;
		}
	}
	
	Map<String, Long> inlinks = Maps.newHashMap(); 
	List<String> language_codes = Lists.newArrayList();
	HashMap<String, ArrayList<GazetteerEntry>> gaz_entries = Maps.newHashMap();
	Set<ItemIdValue> type_set = Sets.newHashSet();

	public SimpleGazetteerExtractor() {
		language_codes.add("en");
		
		int ntypes = 0;
		for(String id : SimpleGazetteerExtractor.type_label_map.keySet()) {
			type_set.add( Datamodel.makeWikidataItemIdValue(id) );
			ntypes ++;
		}
		System.out.println(ntypes + " types");
		//System.exit(1);
	}
	
	/**
	 * Main method. Processes the whole dump using this processor and writes the
	 * results to a file. To change which dump file to use and whether to run in
	 * offline mode, modify the settings in {@link ExampleHelpers}.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ExampleHelpers.configureLogging();
		SimpleGazetteerExtractor processor = new SimpleGazetteerExtractor();
		ExampleHelpers.processEntitiesFromWikidataDump(processor);
		processor.writeFinalResults();
	}
	
	public boolean matchSet(StatementGroup statementGroup, Set<ItemIdValue> set) {
		for(ItemIdValue id : set) {
			if(containsValue(statementGroup, id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		
		// Print status once in a while
		if (this.nprocessed % 100000 == 0) {
			printStatus();
		}
		this.nprocessed++;
		
		Map<String, Boolean> type_keep = Maps.newHashMap();
		for (StatementGroup sg : itemDocument.getStatementGroups()) {		
			EntityIdValue subject = sg.getSubject();
			if(inlinks.containsKey(subject.getId())) {
				inlinks.put(subject.getId(), inlinks.get(subject.getId())+1);
			} else {
				inlinks.put(subject.getId(), new Long(1));
			}
			
			switch (sg.getProperty().getId()) {
			case "P31": // P31 is "instance of"
				for(ItemIdValue t : type_set) {
					boolean match = matchSet(sg, Sets.newHashSet(t));
					type_keep.put(t.getId(), match);
				}

				break;
			}
		}
		
		for(String type : type_keep.keySet()) {
			
			if (!type_keep.get(type)) continue;
			
			Map<String, MonolingualTextValue> labels = itemDocument.getLabels();
			if ( !labels.containsKey("en") ) continue;
			String name = labels.get("en").getText();
			SiteLink link = itemDocument.getSiteLinks().get("enwiki");
			if (link == null) continue;
		
			GazetteerEntry entry = new GazetteerEntry(itemDocument.getItemId(), name, itemDocument.getAliases().get("en"), link);
		
			if(gaz_entries.containsKey(type)){ 
				gaz_entries.get(type).add(entry);
			} else {
				ArrayList<GazetteerEntry> es = Lists.newArrayList(entry);
				gaz_entries.put(type, es);
			}
		}
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		// TODO Auto-generated method stub
		
	}	
	
	/**
	 * Prints the current status to the system output.
	 */
	private void printStatus() {
		for(String code : Arrays.asList("en")) {
			int total = 0;
			for(String key : gaz_entries.keySet()) {
				int size = gaz_entries.get(key).size();
				String label = SimpleGazetteerExtractor.type_label_map.get(key);
				assert( label != null );
				if(label == null) {
					System.out.println("Missing key: " + key);
					System.exit(1);
				}
				System.out.println(code + " " + key + " " + label + " " + size);
				total += size;
			}
			System.out.println("total entries = " + total);
		}
	}
	
	public void printGaz(PrintStream out, ArrayList<GazetteerEntry> gaz, String label) {
		for(GazetteerEntry entry : gaz) {
			long ninlinks = 0;
			if(inlinks.containsKey(entry.id.getId())) {
				ninlinks = inlinks.get(entry.id.getId());
			}
			out.print(entry.id.getId() + "\t" + ninlinks + "\t" + label + "\t" + entry.name);
			if(entry.aliases != null) {
				for(MonolingualTextValue a : entry.aliases) {
					out.print("\t" + a.getText());
				}
			}
			out.println("");
		}
	}
	
	public void writeFinalResults() {
		printStatus();

		for(String code : language_codes) {
			try (PrintStream out = new PrintStream(ExampleHelpers.openExampleFileOuputStream(code + "_gazetteer.txt"))) {
				
				for(String key : gaz_entries.keySet()) {
					printGaz(out, gaz_entries.get(key), SimpleGazetteerExtractor.type_label_map.get(key));
				}
		
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Checks if the given group of statements contains the given value as the
	 * value of a main snak of some statement.
	 *
	 * @param statementGroup
	 *            the statement group to scan
	 * @param value
	 *            the value to scan for
	 * @return true if value was found
	 */
	private boolean containsValue(StatementGroup statementGroup, Value value) {
		// Iterate over all statements
		for (Statement s : statementGroup.getStatements()) {
			// Find the main claim and check if it has a value
			if (s.getClaim().getMainSnak() instanceof ValueSnak) {
				Value v = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
				// Check if the value is an ItemIdValue
				if (value.equals(v)) {
					return true;
				}
			}
		}
		return false;
	}

}