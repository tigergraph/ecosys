#!usr/bin/sh
# remove existing data
echo "remove folder $target"
rm -r $target
echo 'done remove'

# download data
echo "download $data($index/$nodes)"
python3 -u download_one_partition.py $data $index $nodes
echo 'done download'


# decompress data
cd $target
cur=$(pwd)
echo "deompose files in $cur"
mv inserts_split inserts 
# in CentOS, you may use /home/tigergraph/bin/parallel
find . -name *.gz  -print0 | parallel -q0 gunzip 
echo 'done decompress'