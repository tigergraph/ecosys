Contents
=============
This GSQL 102 tutorial contains 

- setup.gsql: scripts to create the schema and loading job 
- load_data.sh: the bash script to run the loading job. Please read the comments therein.
- queries folder: contains the tutorial queries. 


How To Run
===============

We have generated scale-factor 1 data set (approximate 1GB). You can download it via:
wget --no-check-certificate https://s3-us-west-1.amazonaws.com/tigergraph-benchmark-dataset/LDBC/SF-1/ldbc_snb_data-sf1.tar.gz 

- After downloading the raw file, you can run tar command below to decompress the downloaded file. 
tar -xzf  ldbc_snb_data-sf1.tar.gz

- After decompressing the file, you will see a folder named "ldbc_snb_data". Enter it, you will see two subfolders  
social_network 
substitution_parameters

The raw data is under the social_network folder. 

- load data  under bash shell

Setup the environment variable LDBC_SNB_DATA_DIR  pointing to your raw file folder un-tarred in the previous section. In the example below, the raw data is in /home/tigergraph/ldbc_snb_data/social_network. Note, the folder should have the name social_network. 

#change the load_data.sh, make LDBC_SNB_DATA_DIR points to the social_network directory
#e.g. export LDBC_SNB_DATA_DIR=/home/tigergraph/ldbc_snb_data/social_network/

#start tigergraph services
gadmin start

#setup schema and loading job
gsql  setup.gsql

#run the loading job
load_data.sh 

#run the tutorial queries
cd queries
#gsql  queryName.gsql
#e.g. 
gsql one-hop1.gsql

Documents
==============
https://docs.tigergraph.com/intro/gsql-102

Support
===============
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://groups.google.com/a/opengsql.org/forum/?hl=en#!forum/gsql-users



