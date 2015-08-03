#! /usr/bin/env bash

./gaz2conll.py --gaz results/wikidatawiki-20150720/en_gazetteer.txt \
               --counts results/wikidatawiki-20150720/en_gazetteer_counts.txt \
               --output-prefix datasets/wikidata \
               --min-count 3000 \
               --exclude event \
               --exclude cathedral \
               --exclude rock_band \
               --exclude settlement \
               --exclude non_profit \
               --exclude company \
               --exclude organization \
               --train-min 500 \
               --train-max 2500 \
               --train-incr 500 \
               --dev 250 \
               --test 250

# eof
