package org.wikidata.wdtk.examples;

/*
 * #%L
 * Wikidata Toolkit Examples
 * %%
 * Copyright (C) 2014 - 2015 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class FixedPersonAttributeExtractor implements EntityDocumentProcessor {

    static final int MIN_ATTRS = 2;
    static final String MISSING = "NONE";

    int nprocessed = 0;
    String target_type = "Q5"; // person

    static final ImmutableMap<String, String> per_attr =
        new ImmutableMap.Builder<String,String>()
        //.put("P172", "ethnic_group")
        //.put("P140", "religion")
        .put("P21",  "sex_or_gender")
        //.put("P103", "native_language")
        //.put("P27",  "country_of_citizenship") // not really predictive
        .put("P19", "place_of_birth") // requires an additional hop to fetch country
        .put("P735", "given_name")
        //.put("P734", "family_name")
        .build();

    static final ImmutableSet<String> required_attrs =
        new ImmutableSet.Builder<String>()
        .add("P21")   // sex or gender
        .add("Q6256") // country (of birth)
        .add("Q5107") // continent (of birth)
        .add("P735")
        //.add("P734")
        .build();

    class PersonEntry {
        public ItemIdValue id;
        public String name;
        public SiteLink link;
        public List<MonolingualTextValue> aliases;
        public Map<String,String> attributes;

        public PersonEntry(ItemIdValue id,
                           String name,
                           List<MonolingualTextValue> aliases,
                           Map<String,String> attributes,
                           SiteLink link) {
            this.id = id;
            this.name = name;
            this.aliases = aliases;
            this.attributes = attributes;
            this.link = link;
        }

        @Override
        public String toString() {
            Joiner joiner = Joiner.on(" ");
            List<String> attrs = Lists.newArrayList();

            // add the per type
            attrs.add("P31"+"_"+target_type);

            //for(String key : this.attributes.keySet()) {
                // for(String val : this.attributes.get(key)) {
                //     attrs.add(key+"_"+val);
                // }
            //}

            // for(String key : FixedPersonAttributeExtractor.per_attr.keySet()) {
            //     if(this.attributes.containsKey(key)) {
            //         String val = this.attributes.get(key);
            //         attrs.add(key+"_"+val);
            //     } else {
            //         System.out.println("Shouldn't get here");
            //         System.exit(1);
            //         attrs.add(key+"_"+MISSING);
            //     }
            // }

            for(String key : attributes.keySet()) {
                String val = attributes.get(key);
                attrs.add(key+"_"+val);
            }

            List<String> symbols = Lists.newArrayList();
            for (int i = 0; i < this.name.length(); i++) {
                char c = this.name.charAt(i);
                if(c == ' ') {
                    symbols.add(Character.toString('_'));
                } else {
                    symbols.add(Character.toString(c));
                }
            }

            return joiner.join(attrs) + "\t" + joiner.join(symbols);
        }
    }

    ItemIdValue target_id_value;
    List<String> language_codes = Lists.newArrayList();
    ArrayList<PersonEntry> per_entries = Lists.newArrayList();

    // place_of_birth to country
    Map<String,String> place_country_continent = Maps.newHashMap();
    Map<String,String> place_country = Maps.newHashMap();
    Map<String,String> country_continent = Maps.newHashMap();
    WikibaseDataFetcher wbdf;

    // # attributes -> freq
    TreeMap<Integer, Integer> attr_counts = Maps.newTreeMap();
    TreeMap<String, Integer> uni_attr_counts = Maps.newTreeMap();

    public FixedPersonAttributeExtractor() {
        language_codes.add("en");
        target_id_value = Datamodel.makeWikidataItemIdValue(target_type);
        wbdf = new WikibaseDataFetcher();
    }

    public static void main(String[] args) throws IOException {
        ExampleHelpers.configureLogging();
        FixedPersonAttributeExtractor processor = new FixedPersonAttributeExtractor();
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

    public String getCountryContinent(String key) {

        //String key = ((ItemDocument) itemDocument).getLabels().get("en").getText();

        if(country_continent.containsKey(key)) {
            return country_continent.get(key);
        }

        ItemDocument itemDocument = null;
        try {
            EntityDocument ed = wbdf.getEntityDocument(key);
            if (ed == null || !(ed instanceof ItemDocument)) {
                return null;
            }
            itemDocument = (ItemDocument)ed;
        } catch(Exception e) {
            return null;
        }

        for (StatementGroup sg : itemDocument.getStatementGroups()) {
            String property_id = sg.getProperty().getId();
            if(property_id.equals("P30")) { // continent
                for(Statement s : sg.getStatements()) {
                    if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                        Value v = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                        if(v instanceof EntityIdValue) {
                            EntityIdValue idv = (EntityIdValue)v;
                            String id = idv.getId();
                            //System.out.println("\tContinent = " + id);
                            country_continent.put(key, id);
                            return id;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getPlaceCountry(String key) {

        //String key = ((ItemDocument) itemDocument).getLabels().get("en").getText();

        if(place_country.containsKey(key)) {
            //return country_continent.get(key);
            return place_country.get(key);
        }

        ItemDocument itemDocument = null;
        try {
            EntityDocument ed = wbdf.getEntityDocument(key);
            if (ed == null || !(ed instanceof ItemDocument)) {
                return null;
            }
            itemDocument = (ItemDocument)ed;
        } catch(Exception e) {
            return null;
        }

        for (StatementGroup sg : itemDocument.getStatementGroups()) {
            String property_id = sg.getProperty().getId();
            if(property_id.equals("P17")) { // country
                for(Statement s : sg.getStatements()) {
                    if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                        Value v = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                        if(v instanceof EntityIdValue) {
                            EntityIdValue idv = (EntityIdValue)v;
                            String id = idv.getId();
                            //System.out.println("\tCountry = " + id);
                            place_country.put(key, id);
                            return id;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void processItemDocument(ItemDocument itemDocument) {

        // Print status once in a while
        if (this.nprocessed % 100000 == 0) {
            printStatus();
        }
        this.nprocessed++;

        Map<String, String> attributes = Maps.newTreeMap();

        for (StatementGroup sg : itemDocument.getStatementGroups()) {
            EntityIdValue subject = sg.getSubject();
            switch (sg.getProperty().getId()) {
            case "P31": // P31 is "instance of"
                boolean match = matchSet(sg, Sets.newHashSet(target_id_value));
                if(!match) {
                    return; // not a person, move on to next item document
                }
                //break; // we've found a person!
            }
            String property_id = sg.getProperty().getId();
            for(String key : per_attr.keySet()) {
                if(property_id.equals(key)) {
                    List<String> values = Lists.newArrayList();

                    // Get all the values for this attribute
                    for(Statement s : sg.getStatements()) {
                        // Find the main claim and check if it has a value
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value v = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            // Check if the value is an ItemIdValue
                            if(v instanceof EntityIdValue) {
                                EntityIdValue idv = (EntityIdValue)v;

                                if(key == "P19") {
                                    //System.out.println("P19 type = " + idv.getEntityType());
                                    //System.out.println("P19 value = " + idv.getId());
                                    //System.out.println("*** Fetching data for one entity:");
                                    String country = getPlaceCountry(idv.getId());
                                    String continent = getCountryContinent(country);
                                    if(country != null && continent != null) {
                                        attributes.put("Q6256", country);
                                        attributes.put("Q5107", continent);
                                    }
                                } else {
                                    String id = idv.getId();
                                    values.add(id);
                                }
                            } else {
                                System.out.println("unexpected value type");
                                System.exit(1);
                            }
                        }
                    }

                    if(values.size() > 0) {
                        // if there are multiple values, just take the first
                        attributes.put(key, values.get(0));
                    } else {
                        continue;
                    }
                }
            }
        }

        int num_attributes = attributes.keySet().size();
        Integer count = attr_counts.get(num_attributes);
        if (count == null) {
            attr_counts.put(num_attributes, 1);
        } else {
            attr_counts.put(num_attributes, count + 1);
        }

        for(String key : attributes.keySet()) {
            count = uni_attr_counts.get(key);
            if (count == null) {
                uni_attr_counts.put(key, 1);
            } else {
                uni_attr_counts.put(key, count + 1);
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

        if(num_attributes >= MIN_ATTRS) {
            boolean has_all_attrs = true;
            for(String key : required_attrs) {
                if(!attributes.containsKey(key)) {
                    has_all_attrs = false;
                }
            }
            if(has_all_attrs) {
                per_entries.add(entry);
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
            int size = per_entries.size();
            System.out.println("number of PER = " + size);
            System.out.println("place_country hash size = " + place_country.size());
            System.out.println("country_continent hash size = " + country_continent.size());
            System.out.println("attr counts:");
            for(Integer key : attr_counts.keySet()) {
                Integer val = attr_counts.get(key);
                System.out.println("\t"+key+" : "+val);
            }
            System.out.println("uni attr counts:");
            for(String key : uni_attr_counts.keySet()) {
                Integer val = uni_attr_counts.get(key);
                System.out.println("\t"+key+" : "+val);
            }
        }
    }

    public void printPersonEntries(PrintStream out, ArrayList<PersonEntry> gaz) {
        for(PersonEntry entry : per_entries) {
            out.println(entry);
        }
    }

    public void writeFinalResults() {
        printStatus();
        for(String code : language_codes) {
            // Print the gazetteer
            try (PrintStream out = new PrintStream(ExampleHelpers.openExampleFileOuputStream(code + "_fixed_person_entries.txt"))) {
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
