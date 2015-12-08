#! /usr/bin/env bash

mvn clean && mvn compile && mvn exec:java -Dexec.mainClass="org.wikidata.wdtk.examples.FixedPersonAttributeExtractor"

#eof
