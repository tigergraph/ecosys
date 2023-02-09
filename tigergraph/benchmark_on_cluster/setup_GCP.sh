# !/usr/bin/sh
# run `gcloud init` to log in before using this script
export nvm=40 # number of machines
gcloud compute config-ssh
  
for i in $(seq 1 $nvm)
do
  echo "setup m${i}"
  gcloud beta compute scp setup.sh m${i}:~ 
  gcloud beta compute ssh m${i} --command="nohup sh setup.sh > foo.out 2>&1 < /dev/null & "
done