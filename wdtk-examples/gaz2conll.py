#! /usr/bin/env python

import codecs
import argparse
import os
import sys
import collections
import random
import copy

NO_TYPE_LIMIT = float('inf')

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--gaz', required=True, type=str)
    parser.add_argument('--counts', required=True, type=str)
    parser.add_argument('--output-prefix', required=True, type=str)
    parser.add_argument('--aliases', action='store_true')
    parser.add_argument('--shuffle', action='store_true')
    parser.add_argument('--min-count', required=True, type=int)
    parser.add_argument('--exclude', action='append', type=str, default=[])
    parser.add_argument('--train-min', type=int)
    parser.add_argument('--train-max', type=int)
    parser.add_argument('--train-incr', type=int)
    parser.add_argument('--dev', type=int)
    parser.add_argument('--test', type=int)
    args = parser.parse_args()
    return args

def write_name(out, name, name_type):
    tokens = name.split()
    out_name_type = "B-" + name_type
    for token in tokens:
        out.write(token + " " + out_name_type + "\n")
        out_name_type = "I-" + name_type
    out.write("\n")

def write_names(outpath, name_dict, max_per_type = NO_TYPE_LIMIT):
    out = codecs.open(outpath, 'w', 'utf-8')
    c = collections.defaultdict(int)
    for t in name_dict:
        while len(name_dict[t]) > 0:
            name = name_dict[t].pop()
            write_name(out, name, t)
            c[t] += 1
            if c[t] >= max_per_type:
                break
    total = 0
    for t in c:
        total += c[t]
    print('wrote ' + str(total) + ' names to ' + outpath)
    for t in c:
        print(str(c[t]) + ' ' + t)
    out.close()

def process_gaz(type_set, args):
    names = collections.defaultdict(set)

    # Read the gazetteer
    for line in codecs.open(args.gaz, 'r', 'utf-8'):
        tokens = line.rstrip().split('\t')
        t = tokens[2]
        if t in type_set:
            if args.aliases:
                for name in tokens[3:]:
                    names[t].add(name)

            else:
                name = tokens[3]
                names[t].add(name)

    # Optionally: shuffle names
    #if args.shuffle:
    #    random.seed(42)
    #    random.shuffle(names)

    # Write dev and test
    if args.dev:
        write_names(args.output_prefix+"_dev.txt", names, args.dev)
    if args.test:
        write_names(args.output_prefix+"_test.txt", names, args.test)

    curr_size = args.train_min
    while curr_size <= args.train_max:
        names_copy = copy.deepcopy(names)
        write_names(args.output_prefix+"_train_" + str(curr_size) + ".txt", names_copy, curr_size)
        curr_size += args.train_incr

def read_counts(inpath):
    ret = dict()
    for line in open(inpath, 'r'):
        tokens = line.rstrip().split()
        assert len(tokens) == 2, line
        t = tokens[0]
        c = int(tokens[1])
        ret[t] = c
    return ret

def get_counts(inpath, include_aliases):
    names = collections.defaultdict(set)
    for line in open(inpath, 'r'):
        tokens = line.rstrip().split('\t')
        tag = tokens[2]
        if include_aliases:
            for token in tokens[3:]:
                names[tag].add(token)
        else:
            names[tag].add(tokens[3])
    ret = collections.defaultdict(int)
    for t in names:
        ret[t] = len(names[t])
    return ret

def get_type_set(counts, min_c, exclude_set):
    ret = set()
    for k, v in counts.items():
        if v >= min_c and not k in exclude_set:
            ret.add(k)
    return ret

def print_types(type_set, counts):
    print("Keeping the following types:")
    for t in type_set:
        print(t + " : " + str(counts[t]))

def main():
    args = get_args()
    #counts = read_counts(args.counts)
    counts = get_counts(args.gaz, args.aliases)
    type_set = get_type_set(counts, args.min_count, set(args.exclude))
    print_types(type_set, counts)
    process_gaz(type_set, args)
    return True

if __name__ == "__main__":
    sys.exit(main())

# eof
