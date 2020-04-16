- We have generated scale-factor 1 data set (approximate 1GB). You can download it from https://s3-us-west-1.amazonaws.com/tigergraph-benchmark-dataset/LDBC/SF-1/ldbc_snb_data-sf1.tar.gz 
- After downloading the raw file, you can run tar command below to decompress the downloaded file. 
tar -xzf  ldbc_snb_data-sf1.tar.gz

- After decompressing the file, you will see a folder named "ldbc_snb_data". Enter it, you will see two subfolders  
social_network 
substitution_parameters

The raw data is under the social_network folder. 

- load data  under bash shell

 Setup the environment variable LDBC_SNB_DATA_DIR  pointing to your raw file folder un-tarred in the previous section. In the example below, the raw data is in /home/tigergraph/ldbc_snb_data/social_network. Note, the folder should have the name social_network. 

#change the directory to your raw file directory
export LDBC_SNB_DATA_DIR=/home/tigergraph/ldbc_snb_data/social_network/

#start all TigerGraph services
gadmin start

#setup schema and loading job
gsql setup_schema.gsql

./load_data.sh

gadmin status graph -v
