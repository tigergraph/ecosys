LOAD CSV FROM 'file:///inserts/dynamic/Comment_hasTag_Tag/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS commentId,
  toInteger(row[2]) AS tagId
MATCH (comment:Comment {id: commentId}), (tag:Tag {id: tagId})
CREATE (comment)-[:HAS_TAG {creationDate: creationDate}]->(tag)
RETURN count(*)
