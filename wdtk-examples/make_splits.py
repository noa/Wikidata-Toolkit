#! /usr/bin/env python3

import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--output-prefix', required=True)
parser.add_argument('--train-size', required=True, type=int)
parser.add_argument('--valid-size', required=True, type=int)
parser.add_argument('--test-size',  required=True, type=int)
parser.add_argument('--corpus', default='typo-corpus-r1.txt')
args = parser.parse_args()

n = 0
paths = [ args.output_prefix + '_train.txt',
          args.output_prefix + '_valid.txt',
          args.output_prefix + '_test.txt' ]
sizes = [ args.train_size, args.valid_size, args.test_size ]
d_idx = 0
ouf = open(paths[d_idx], "w")
i = 0
for line in open(args.corpus):
    #print('line '+str(i))
    #tokens = line.split()
    #ouf.write(" ".join(list(tokens[0])) + "\t" + " ".join(list(tokens[1])) + "\n")
    ouf.write(line)
    n += 1
    i += 1
    if n == sizes[d_idx]:
        #print('d_idx ' + str(d_idx))
        print('wrote ' + str(n) + ' lines to: ' + paths[d_idx])
        n = 0
        ouf.close()
        if d_idx < len(paths)-1:
            d_idx += 1
            ouf = open(paths[d_idx], "w")
        else:
            ouf.close()
            break
#print('wrote ' + str(n) + ' lines to: ' + paths[d_idx])

# eof
