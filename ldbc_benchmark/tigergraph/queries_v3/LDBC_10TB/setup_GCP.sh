# !/usr/bin/sh
# run `gcloud init` to log in before using this script
export sys=ubuntu
export nvm=40
gcloud compute config-ssh
for i in $(seq 1 $nvm)
do
  gcloud beta compute scp setup_${sys}.sh m${i}:~ 
  gcloud beta compute ssh m${i} --command="nohup sh setup_${sys}.sh > foo.out 2>&1 < /dev/null & "
done




