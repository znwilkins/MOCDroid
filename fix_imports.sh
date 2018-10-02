#!/usr/bin/env bash

newfile=`echo $1 | cut -d '.' -f 1`_header.csv
echo "Writing to $newfile"
echo -e "doc_id,text" > $newfile
count=0
while read p
do
    echo "$count,$p" >> $newfile
    count=$(($count+1))
done <$1

