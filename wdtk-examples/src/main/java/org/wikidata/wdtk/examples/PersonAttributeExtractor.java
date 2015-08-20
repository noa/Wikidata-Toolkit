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

public class PersonAttributeExtractor implements EntityDocumentProcessor {
    int nprocessed = 0;

    // static final ImmutableMap<String, String> type_label_map =
    //     new ImmutableMap.Builder<String,String>()
    //     .put("Q5", "person")
    //     .build();
    String target_type = "Q5"; // person

    static final ImmutableMap<String, String> per_attr_label_map =
        new ImmutableMap.Builder<String,String>()
        .put("P172", "ethnic_group")
        .put("P140", "religion")
        .put("P410", "military_rank")
        .put("P21",  "sex_or_gender")
        .put("P27",  "country_of_citizenship")
        .put("P103", "native_language")
        .build();

    // these are entity relations -- different than the attributes above
    //.put("P25","mother")
    //.put("P22","father")

    class PersonEntry {
        public ItemIdValue id;
        public String name;
        public SiteLink link;
        public List<MonolingualTextValue> aliases;
        public Map<String,List<String>> attributes;

        public PersonEntry(ItemIdValue id,
                           String name,
                           List<MonolingualTextValue> aliases,
                           Map<String,List<String>> attributes,
                           SiteLink link) {
            this.id = id;
            this.name = name;
            this.aliases = aliases;
            this.attributes = attributes;
            this.link = link;
        }
    }

    ItemIdValue target_id_value;
    List<String> language_codes = Lists.newArrayList();
    ArrayList<PersonEntry> per_entries = Lists.newArrayList();
    HashMap<String, Integer> attr_counts = Maps.newHashMap();

    public PersonAttributeExtractor() {
        language_codes.add("en");
        target_id_value = Datamodel.makeWikidataItemIdValue(target_type);
    }

    public static void main(String[] args) throws IOException {
        ExampleHelpers.configureLogging();
        PersonAttributeExtractor processor = new PersonAttributeExtractor();
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

        Map<String, List<String>> attributes = Maps.newHashMap();

        for (StatementGroup sg : itemDocument.getStatementGroups()) {
            EntityIdValue subject = sg.getSubject();
            switch (sg.getProperty().getId()) {
            case "P31": // P31 is "instance of"
                boolean match = matchSet(sg, Sets.newHashSet(target_id_value));
                if(!match) {
                    return;
                }
                //break; // we've found a person!
            }
            String property_id = sg.getProperty().getId();
            for(String key : per_attr_label_map.keySet()) {
                if(property_id == key) {
                    //attributes.put();
                	for(Statement s : sg.getStatements()) {
                        // Find the main claim and check if it has a value
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value v = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            // Check if the value is an ItemIdValue
                            
                        }
                	}
                }
            }
        }

        Map<String, MonolingualTextValue> labels = itemDocument.getLabels();

        // If this entry doesn't have an English name, move on
        if ( !labels.containsKey("en") ) return;
        String name = labels.get("en").getText();
        SiteLink link = itemDocument.getSiteLinks().get("enwiki");
        if (link == null) return;
        PersonEntry entry = new PersonEntry(itemDocument.getItemId(),
                                            name,
                                            itemDocument.getAliases().get("en"),
                                            attributes,
                                            link);
        per_entries.add(entry);
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
            int size = per_entries.size();
            System.out.println("number of PER = " + size);
        }
    }

    public void printPersonEntries(PrintStream out, ArrayList<PersonEntry> gaz) {
        for(PersonEntry entry : per_entries) {
            out.print(entry.id.getId() + "\t" + entry.name);
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
            // Print the gazetteer
            try (PrintStream out = new PrintStream(ExampleHelpers.openExampleFileOuputStream(code + "_person_entries.txt"))) {
                
                printPersonEntries(out, per_entries);

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
