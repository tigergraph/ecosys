#!usr/bin/sh
# remove existing data
echo "remove folder $target"
rm -r $target
echo 'done remove'

# download data
echo "download $data($index/$nodes) using $thread threads"
python3 -u download_one_partition.py $data $index $nodes -t $thread $key && \
echo 'done download' && \
echo "deompose files in $target" && \
mv $target/inserts_split $target/inserts && \
find $target -name *.csv.gz  -print0 | parallel -q0 gunzip && \ 
echo 'done decompress'