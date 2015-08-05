#! /usr/bin/env bash

# First dataset (500 to 2500 train, 250 dev and test)
if [ ! -d "datasets/min3000" ]; then
    mkdir datasets/min3000
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
fi

# Second dataset (1000 to 4000 train, 500 dev and test)
if [ ! -d "datasets/min5000" ]; then
    mkdir datasets/min5000
    ./gaz2conll.py --gaz results/wikidatawiki-20150720/en_gazetteer.txt \
                   --counts results/wikidatawiki-20150720/en_gazetteer_counts.txt \
                   --output-prefix datasets/min5000/wikidata \
                   --min-count 5000 \
                   --exclude event \
                   --exclude cathedral \
                   --exclude rock_band \
                   --exclude settlement \
                   --exclude non_profit \
                   --exclude company \
                   --train-min 1000 \
                   --train-max 4000 \
                   --train-incr 1000 \
                   --dev 500 \
                   --test 500
fi

# Third dataset (1000 to 4000 train, 500 dev and test)
if [ ! -d "datasets/min10000" ]; then
    mkdir datasets/min10000
    ./gaz2conll.py --gaz results/wikidatawiki-20150720/en_gazetteer.txt \
                   --counts results/wikidatawiki-20150720/en_gazetteer_counts.txt \
                   --output-prefix datasets/sets/min10000/wikidata \
                   --min-count 10000 \
                   --exclude event \
                   --exclude cathedral \
                   --exclude rock_band \
                   --exclude settlement \
                   --exclude non_profit \
                   --exclude company \
                   --exclude organization \
                   --train-min 1000 \
                   --train-max 9000 \
                   --train-incr 1000 \
                   --dev 500 \
                   --test 500
fi

# Fourth dataset 10000 to 22500 train, 1000 dev and test
if [ ! -d "datasets/min15000" ]; then
    mkdir datasets/min15000
    ./gaz2conll.py --gaz results/wikidatawiki-20150720/en_gazetteer.txt \
                   --counts results/wikidatawiki-20150720/en_gazetteer_counts.txt \
                   --output-prefix datasets/min15000/wikidata \
                   --min-count 15000 \
                   --exclude event \
                   --exclude cathedral \
                   --exclude rock_band \
                   --exclude settlement \
                   --exclude non_profit \
                   --train-min 8000 \
                   --train-max 14000 \
                   --train-incr 1000 \
                   --dev 500 \
                   --test 500
fi

# eof
