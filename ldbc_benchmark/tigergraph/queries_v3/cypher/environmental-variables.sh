export POSTFIX=.csv
export NEO4J_CONTAINER_NAME="neo-snb"

# directory of raw data
export RAW_DIR=$HOME/initial_snapshot

# the folders below will be created after loading 
# Please ensure these folders do not exist
export CSV_DIR=$HOME/ldbc_data 


echo "variables loaded, direcotry to be loaded is $RAW_DIR"