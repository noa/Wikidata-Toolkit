#! /usr/bin/env bash

# Shuffle lines
shuf results/wikidatawiki-20150720/en_person_entries.txt \
     > shuffled_per_entries.txt

# Make splits
./make_splits.py --corpus shuffled_per_entries.txt  \
                 --output-prefix per_attrs --train-size 500000 \
                 --valid-size 25000 --test-size 25000

# eof
