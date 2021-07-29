#!usr/bin/sh
# remove existing data
echo '======= start remove ======='
rm -r $target
echo '======= done remove ======='

# download data
echo '======= start download ======='
python3 -u download_data_gcs.py $index $nodes -d $data 
echo '======= done download ======='

# decompress data
echo '======= start decompress ======='
cd $target
mv inserts_split inserts 
# in CentOS, you may use /home/tigergraph/bin/parallel
find . -name *.gz  -print0 | parallel -q0 gunzip 
echo '======= done decompress ======='