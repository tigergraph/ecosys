# original load-in-one-step.sh runs many files
# to make reading and editing easier, I put wrote them in functons
# and finally run
# stopDelete && concate && replaceHeader && import

: ${RAW_DIR:?"Environment variable RAW_DIR is unset or empty"}
: ${CSV_DIR:?"Environment variable CSV_DIR is unset or empty"}
: ${POSTFIX:?"Environment variable POSTFIX is unset or empty"}

# stop neo4j and delete the data in Neo4j
stopDelete() {
    $NEO4J_HOME/bin/neo4j stop
    #rm $NEO4J_HOME/data/databases/neo4j/*
    echo "Done stop neo4j and delete neo4j data"
}
stopDelete

# Prepare data
# concatenate files of each type into one csv file 
# and store at $CSV_DIR 
concate() {
    if [ -d $CSV_DIR ]; then
        rm -r $CSV_DIR
    fi
    mkdir -p $CSV_DIR

    for d in $(ls $RAW_DIR); do
    for f in $(ls $RAW_DIR/$d/*.csv); do
        #echo $f
        target=$CSV_DIR/$d.csv
        if [ ! -f $target ]; then
            cp $f $target
        else
            tail -n +2 $f >> $target #append the file without header to target 
        fi
    done
    done
    echo "done concatenate"
}
concate 

# Replace the header
showHeader() {
    for f in $(ls ${CSV_DIR}); do
        echo "${f::-4} : $(head -1 ${CSV_DIR}/$f)"
    done
}
replaceHeader() {
    while read line; do
        IFS=' ' read -r -a array <<< $line
        filename=${array[0]}
        header=${array[1]}
        sed -i "1s/.*/$header/" "${CSV_DIR}/${filename}${POSTFIX}"
    done < headers.txt
    echo "Done replace header"
}
replaceHeader

# import the csv files 
import() {
    $NEO4J_HOME/bin/neo4j-admin import \
    --id-type=INTEGER \
    --ignore-empty-strings=true \
    --bad-tolerance=0 \
    --nodes=Place="${CSV_DIR}/Place${POSTFIX}" \
    --nodes=Organisation="${CSV_DIR}/Organisation${POSTFIX}" \
    --nodes=TagClass="${CSV_DIR}/TagClass${POSTFIX}" \
    --nodes=Tag="${CSV_DIR}/Tag${POSTFIX}" \
    --nodes=Forum="${CSV_DIR}/Forum${POSTFIX}" \
    --nodes=Person="${CSV_DIR}/Person${POSTFIX}" \
    --nodes=Message:Comment="${CSV_DIR}/Comment${POSTFIX}" \
    --nodes=Message:Post="${CSV_DIR}/Post${POSTFIX}" \
    --relationships=IS_PART_OF="${CSV_DIR}/Place_isPartOf_Place${POSTFIX}" \
    --relationships=IS_SUBCLASS_OF="${CSV_DIR}/TagClass_isSubclassOf_TagClass${POSTFIX}" \
    --relationships=IS_LOCATED_IN="${CSV_DIR}/Organisation_isLocatedIn_Place${POSTFIX}" \
    --relationships=HAS_TYPE="${CSV_DIR}/Tag_hasType_TagClass${POSTFIX}" \
    --relationships=HAS_CREATOR="${CSV_DIR}/Comment_hasCreator_Person${POSTFIX}" \
    --relationships=IS_LOCATED_IN="${CSV_DIR}/Comment_isLocatedIn_Country${POSTFIX}" \
    --relationships=REPLY_OF="${CSV_DIR}/Comment_replyOf_Comment${POSTFIX}" \
    --relationships=REPLY_OF="${CSV_DIR}/Comment_replyOf_Post${POSTFIX}" \
    --relationships=CONTAINER_OF="${CSV_DIR}/Forum_containerOf_Post${POSTFIX}" \
    --relationships=HAS_MEMBER="${CSV_DIR}/Forum_hasMember_Person${POSTFIX}" \
    --relationships=HAS_MODERATOR="${CSV_DIR}/Forum_hasModerator_Person${POSTFIX}" \
    --relationships=HAS_TAG="${CSV_DIR}/Forum_hasTag_Tag${POSTFIX}" \
    --relationships=HAS_INTEREST="${CSV_DIR}/Person_hasInterest_Tag${POSTFIX}" \
    --relationships=IS_LOCATED_IN="${CSV_DIR}/Person_isLocatedIn_City${POSTFIX}" \
    --relationships=KNOWS="${CSV_DIR}/Person_knows_Person${POSTFIX}" \
    --relationships=LIKES="${CSV_DIR}/Person_likes_Comment${POSTFIX}" \
    --relationships=LIKES="${CSV_DIR}/Person_likes_Post${POSTFIX}" \
    --relationships=HAS_CREATOR="${CSV_DIR}/Post_hasCreator_Person${POSTFIX}" \
    --relationships=HAS_TAG="${CSV_DIR}/Comment_hasTag_Tag${POSTFIX}" \
    --relationships=HAS_TAG="${CSV_DIR}/Post_hasTag_Tag${POSTFIX}" \
    --relationships=IS_LOCATED_IN="${CSV_DIR}/Post_isLocatedIn_Country${POSTFIX}" \
    --relationships=STUDY_AT="${CSV_DIR}/Person_studyAt_University${POSTFIX}" \
    --relationships=WORK_AT="${CSV_DIR}/Person_workAt_Company${POSTFIX}" \
    --delimiter '|'
    echo "Done import"
}
import

# Data is located at the default place.
# use 'du -sh $DATA_DIR' to check the loaded data size 
export DATA_DIR=$NEO4J_HOME/data/databases

# Start the neo4j
start() {
    $NEO4J_HOME/bin/neo4j start
}
start


#create indices
createIndices() {
     cypher-shell < indices.cypher
}
#createIndices