#! /usr/bin/env bash

./gaz2conll.py --gaz results/wikidatawiki-20150720/en_gazetteer.txt \
               --counts results/wikidatawiki-20150720/en_gazetteer_counts.txt \
               --output-prefix wikidata \
               --min-count 1000 \
               --exclude event \
               --exclude cathedral \
               --exclude rock_band \
               --exclude settlement \
               --exclude non_profit \
               --exclude company \
               --exclude organization \
               --train-min 100 \
               --train-max 800 \
               --train-incr 100 \
               --dev 100 \
               --test 100

# eof
