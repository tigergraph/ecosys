#!usr/bin/sh
# remove existing data
echo "remove folder $target"
rm -r $target
echo 'done remove'

# download data
echo "download ($index/$nodes)"
python3 -u download_data_gcs.py $index $nodes -d $data 

# decompress data
echo '======= start decompress ======='
cd $target
mv inserts_split inserts 
# in CentOS, you may use /home/tigergraph/bin/parallel
find . -name *.gz  -print0 | parallel -q0 gunzip 
echo '======= done decompress ======='