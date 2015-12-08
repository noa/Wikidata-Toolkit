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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;

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

public class TypeHistogram implements EntityDocumentProcessor {

    int nprocessed = 0;
    TreeMap<String, Integer> histogram = Maps.newTreeMap();

    //public LinkedHashMap sortHashMapByValuesD(HashMap passedMap) {
    public LinkedHashMap<String, Integer> sortHashMapByValuesD(TreeMap<String, Integer> passedMap) {
        List mapKeys = new ArrayList(passedMap.keySet());
        List mapValues = new ArrayList(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<String,Integer> sortedMap = new LinkedHashMap<String,Integer>();

        Iterator valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Object val = valueIt.next();
            Iterator keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Object key = keyIt.next();
                String comp1 = passedMap.get(key).toString();
                String comp2 = val.toString();

                if (comp1.equals(comp2)){
                    //passedMap.remove(key);
                    mapKeys.remove(key);
                    sortedMap.put((String)key, (Integer)val);
                    break;
                }
            }
        }
        return sortedMap;
    }

    public TypeHistogram() {
        // nada
    }

    public static void main(String[] args) throws IOException {
        ExampleHelpers.configureLogging();
        TypeHistogram processor = new TypeHistogram();
        ExampleHelpers.processEntitiesFromWikidataDump(processor);
        processor.writeFinalResults();
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
                {
                    // Iterate over all statements
                    for (Statement s : sg.getStatements()) {
                        // Find the main claim and check if it has a value
                        if (s.getClaim().getMainSnak() instanceof ValueSnak) {
                            Value v = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
                            if(v instanceof ItemIdValue) {
                                EntityIdValue ev = (ItemIdValue)v;
                                String type = ev.getId();
                                //String title = ev.getTitle();
                                Integer count = histogram.get(type);
                                if(count == null) {
                                    histogram.put(type, 1);
                                } else {
                                    histogram.put(type, count + 1);
                                }
                            }
                        }
                    }
                    break;
                }
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
        LinkedHashMap<String, Integer> smap = sortHashMapByValuesD(histogram);
        for(String key : smap.keySet()) {
            System.out.println(key + " " + smap.get(key));
        }
    }

    public void writeFinalResults() {
        printStatus();

        // Print the gazetteer
        try (PrintStream out = new PrintStream(ExampleHelpers.openExampleFileOuputStream("type_histogram.txt"))) {
            LinkedHashMap<String, Integer> smap = sortHashMapByValuesD(histogram);
            for(String key : smap.keySet()) {
                out.println(key + " " + smap.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
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
