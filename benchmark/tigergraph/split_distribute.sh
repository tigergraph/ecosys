#!/bin/bash

usage(){
  echo "Usage:"
  echo "./`basename $0` (src_dir) [tgt_dir] (file_extension:csv)"
  echo "e.g.:  ./`basename $0` ~/raw_data ~/raw_data csv"
  echo "The script will split each raw files into n parts, n is the"
  echo "number of cluster nodes, and distribute one file part to"
  echo "each cluster nodes"
  exit 0
}

# path to raw data
src_dir=$1

if [ ! -d "$src_dir" ]; then
  echo "Cannot find source data dir: $src_dir"
  usage
  exit 1
fi
tgt_dir=$2
tgt_dir=${tgt_dir:-$src_dir}
if [ -z "$tgt_dir" ]; then
  echo "Must specify target dir: $src_dir"
  usage
  exit 1
fi

cluster_str=$(grep cluster.nodes ~/.gsql/gsql.cfg | cut -d' ' -f2)
cluster_list=(${cluster_str//,/ })
# split to n partitions
partition_num=${#cluster_list[@]}
if [ -z "$partition_num" ]; then
  echo "Partition number must be specified"
  exit 1
elif ! [[ "$partition_num" =~ ^[1-9][0-9]*$ ]]; then
  echo "Partition number must be positive integer"
  exit 1
fi
echo "partition_num: $partition_num"
file_extension=$3
# default extension: .csv
file_extension=${file_extension:-csv}

cd $src_dir

# step 1, split data
echo "=========== step one: split data ==========="
for file in $(find ./ -type f -name "*.$file_extension" -printf "%f\n"); do
  echo "Spliting file: $file"
  split -n l/$partition_num -d $file $file || exit 1
 # rm -f $file
done
cd -

o1="UserKnownHostsFile=/dev/null"
o2="StrictHostKeyChecking=no"
echo "=========== step two: distribute data ==========="
grun all "mkdir -p $src_dir" || exit 1
n=0
for node in "${cluster_list[@]}"; do
  node_ip=$(echo $node | cut -d':' -f2)
  if /sbin/ip addr | grep inet | grep $node_ip 1>/dev/null && [ "$src_dir" = "$tgt_dir" ]; then
    echo "don't remove files on local machine, $node_ip"
  else
    echo "Scp to node $node_ip ..."
    if [ "$n" -gt 9 ]; then
      scp -i ~/.ssh/tigergraph_rsa -o $o1 -o $o2 $src_dir/*\.${file_extension}$n $node_ip:$tgt_dir
      rm -rf $src_dir/*\.${file_extension}$n
    else
      scp -i ~/.ssh/tigergraph_rsa -o $o1 -o $o2 $src_dir/*\.${file_extension}0$n $node_ip:$tgt_dir
      rm -rf $src_dir/*\.${file_extension}0$n
    fi
  fi
  n=$((n+1))
done
#grun all "cd $src_dir; find . -name \"*.csv*\" -exec rename 's/csv.*$/csv/' {} \;"
#grun all "cd $tgt_dir; find . -name \"*.${file_extension}*\" -exec rename 's/${file_extension}.*$/${file_extension}/' {} \;"
echo "DONE"
