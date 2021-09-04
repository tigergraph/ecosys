#!usr/bin/sh
export target=sf30k
chmod 400 ~/.aws/credentials 
# remove existing data
echo "remove folder $target"
rm -r $target
echo 'done remove'

# download data
echo "download date ($index/$nodes)"
python3 -u download_one_partition.py $index $nodes && \
echo 'done download' && \
echo "deompose files in $target" && \
find $target -name *.csv.gz  -print0 | parallel -q0 gunzip && \
echo 'done decompress'