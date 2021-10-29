#!/bin/bash
data=$1
i=$2
nodes=$3
thread=${4:-10} # default 10 threads

if [[ $data == "10t" ]]; then
	target=sf10k
elif [[ $data == "30t" ]]; then
	target=sf30k
fi

sudo apt-get update 
sudo apt install -y  python3-pip  parallel gzip wget git
pip3 install google-cloud-storage
cd tigergraph/data
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
echo "download $data($index/$nodes) using $thread threads"
python3 -u ecosys/ldbc_benchmark/tigergraph/queries_v3/LDBC_10TB/download_one_partition.py $data $i $nodes -t $thread  && \
echo 'done download' && \
echo "deompose files in $target" && \
mv $target/inserts_split $target/inserts && \
find $target -name *.csv.gz  -print0 | parallel -q0 gunzip && \
echo 'done decompress'