#!/usr/bin/env bash

# Requires bash v4+ for range
for cluster in {20..180..20}
do
    for sparse in 0.95 0.96 0.97 0.98 0.99
    do
        Rscript clustering.R $1 $cluster $sparse
    done
done

