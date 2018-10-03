#!/usr/bin/env bash
# Pass csv file as parameter to script

newfile=`echo $1 | cut -d '.' -f 1`_header.csv
echo "Writing to $newfile"
echo "doc_id,text" > $newfile
count=0
while read line
do
    echo "$count,$line" >> $newfile
    count=$(($count+1))
done <$1

