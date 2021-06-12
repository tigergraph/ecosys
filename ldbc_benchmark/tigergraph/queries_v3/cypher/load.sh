# modified from https://github.com/ldbc/ldbc_snb_bi/blob/main/cypher/scripts/load-in-one-step.sh
# stopDelete && import && start
: ${CSV_DIR:?"Environment variable CSV_DIR is unset or empty"}
stopDelete() {
    $NEO4J_HOME/bin/neo4j stop
    #rm -rf $NEO4J_HOME/data/databases/neo4j
    echo "Done stop neo4j and delete neo4j data"
}

removeFirstLine(){
    for t in static dynamic ; do
        for d in $(ls $CSV_DIR/initial_snapshot/$t); do
            for f in $(ls $CSV_DIR/initial_snapshot/$t/$d/*.csv); do
                tail -n +2 $f > $f.tmp && mv $f.tmp $f 
                #echo $f
            done
        done
    done 
    echo "Done remove the header of csv files"
}

import() {
    $NEO4J_HOME/bin/neo4j-admin import \
    --id-type=INTEGER \
    --ignore-empty-strings=true \
    --nodes=Place="./headers/static/Place.csv$(find ${CSV_DIR}/initial_snapshot/static/Place -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=Organisation="./headers/static/Organisation.csv$(find ${CSV_DIR}/initial_snapshot/static/Organisation -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=TagClass="./headers/static/TagClass.csv$(find ${CSV_DIR}/initial_snapshot/static/TagClass -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=Tag="./headers/static/Tag.csv$(find ${CSV_DIR}/initial_snapshot/static/Tag -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=Forum="./headers/dynamic/Forum.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Forum -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=Person="./headers/dynamic/Person.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=Message:Comment="./headers/dynamic/Comment.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Comment -type f -name 'part-*.csv' -printf ',%p')" \
    --nodes=Message:Post="./headers/dynamic/Post.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Post -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=IS_PART_OF="./headers/static/Place_isPartOf_Place.csv$(find ${CSV_DIR}/initial_snapshot/static/Place_isPartOf_Place -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=IS_SUBCLASS_OF="./headers/static/TagClass_isSubclassOf_TagClass.csv$(find ${CSV_DIR}/initial_snapshot/static/TagClass_isSubclassOf_TagClass -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=IS_LOCATED_IN="./headers/static/Organisation_isLocatedIn_Place.csv$(find ${CSV_DIR}/initial_snapshot/static/Organisation_isLocatedIn_Place -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_TYPE="./headers/static/Tag_hasType_TagClass.csv$(find ${CSV_DIR}/initial_snapshot/static/Tag_hasType_TagClass -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_CREATOR="./headers/dynamic/Comment_hasCreator_Person.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Comment_hasCreator_Person -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=IS_LOCATED_IN="./headers/dynamic/Comment_isLocatedIn_Country.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Comment_isLocatedIn_Country -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=REPLY_OF="./headers/dynamic/Comment_replyOf_Comment.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Comment_replyOf_Comment -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=REPLY_OF="./headers/dynamic/Comment_replyOf_Post.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Comment_replyOf_Post -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=CONTAINER_OF="./headers/dynamic/Forum_containerOf_Post.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Forum_containerOf_Post -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_MEMBER="./headers/dynamic/Forum_hasMember_Person.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Forum_hasMember_Person -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_MODERATOR="./headers/dynamic/Forum_hasModerator_Person.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Forum_hasModerator_Person -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_TAG="./headers/dynamic/Forum_hasTag_Tag.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Forum_hasTag_Tag -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_INTEREST="./headers/dynamic/Person_hasInterest_Tag.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_hasInterest_Tag -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=IS_LOCATED_IN="./headers/dynamic/Person_isLocatedIn_City.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_isLocatedIn_City -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=KNOWS="./headers/dynamic/Person_knows_Person.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_knows_Person -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=LIKES="./headers/dynamic/Person_likes_Comment.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_likes_Comment -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=LIKES="./headers/dynamic/Person_likes_Post.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_likes_Post -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_CREATOR="./headers/dynamic/Post_hasCreator_Person.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Post_hasCreator_Person -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_TAG="./headers/dynamic/Comment_hasTag_Tag.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Comment_hasTag_Tag -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=HAS_TAG="./headers/dynamic/Post_hasTag_Tag.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Post_hasTag_Tag -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=IS_LOCATED_IN="./headers/dynamic/Post_isLocatedIn_Country.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Post_isLocatedIn_Country -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=STUDY_AT="./headers/dynamic/Person_studyAt_University.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_studyAt_University -type f -name 'part-*.csv' -printf ',%p')" \
    --relationships=WORK_AT="./headers/dynamic/Person_workAt_Company.csv$(find ${CSV_DIR}/initial_snapshot/dynamic/Person_workAt_Company -type f -name 'part-*.csv' -printf ',%p')" \
    --delimiter '|'
}

# Start the neo4j
start() {
    $NEO4J_HOME/bin/neo4j start
}

stopDelete && import && start