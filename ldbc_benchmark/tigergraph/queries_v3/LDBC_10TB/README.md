# Prepare LDBC SNB 10 TB data

## pre-requisite
we need to install gzip and gnu parrel to uncompress the data
```sh
sudo yum install -y  python3-pip perl bzip2 gzip wget lynx
(wget -O - pi.dk/3 || lynx -source pi.dk/3 || curl pi.dk/3/ || \
   fetch -o - http://pi.dk/3 ) > install.sh
sh install.sh
# install python3 google-cloud-storage package
pip3 install google-cloud-storage
```

## download data
Use the `download_data_gcs.py` to download the certain partition of the data. The usage of the script is `download_data.py [node index] [number of nodes]`. For example, for a cluster of 4 nodes
```sh
# on node m1
download_data.py 0 4
# on node m2
download_data.py 1 4
# on node m3
download_data.py 1 4
# on node m4
download_data.py 1 4
```
The GCS bucket address is hard coded in the code. The data is downloaded to `./sf10000/`. You can write your own script to process the data, use `grun all "[command]" ` to run the script across the all the nodes. 

The python script in the next step requires a GCP service key in json. The data is public and open to all users, so it is no matter what the public key is. The tutorial for setting up the service key can be found on [GCP docs](https://cloud.google.com/docs/authentication/getting-started).

## Other ways of downloading
You can access single file by using 
```sh
wget https://storage.googleapis.com/ldbc_snb_10k/v1/results/sf10000-compressed/runs/20210713_203448/social_network/csv/bi/composite-projected-fk/deletes/dynamic/Comment/batch_id%3D2012-11-29/part-00000-e89742bf-096f-44c5-88e5-aa3822fbff75.c000.csv.gz
```
or download the whole data using (Google Cloud SDK)[https://cloud.google.com/sdk/docs/install]
```sh
gsutil -m cp -r  gs://ldbc_snb_10k/v1/results/sf10000-compressed/runs/20210713_203448/social_network/csv/bi/composite-projected-fk/ .  
```

## uncompress the data
Uncompress the data on each node
```sh
cd sf10000
find . -name *.gz  -print0 | parallel -q0 gunzip 
```


# Prepare LDBC SNB 10 TB data
The dataset does not have header. So do not add the option `--header`. To load the data
```sh
./driver.py load all ~/sf10000 
```
Run query withou bi 17 and 19
```sh
./driver.py run -q not:17,19
```
Perform the batch update, begin date is `2012-11-29`, end date is `2012-12-31`. We perform bi reading queries every 7 days, we also add sleep factor 1.
```sh
./driver.py refresh ~/sf10000/ -b 2012-11-29 -e 2012-12-31 -q not:17,19 -r 7 -s 1
```

The combine command in background is
```sh
nohup python3 -u ./driver.py all ~/sf10000/ -b 2012-11-29 -e 2012-12-31 -q not:17,19 -r 7 -s 1  > foo.out 2>&1 < /dev/null & 
```


# About running queries
The BI queries on 10TB data is typically ~60 s and greatly depends on the parameters. We chose some parameters that are easy for use, for example if we filter the comments after an input date, we will chose a later time for the Social network graph.

BI20 have several version. The query is fast for some parameters (in this case, the non-distributed one `bi20-1.gsql` takes less time). But the query is super slow for other parameters (>200s, in this case, the distributed one `bi20-2.gsql` is better). We have not validated `bi20-3` yet.

BI17 and 19 are too hard for 10TB data and they ran out of memory.