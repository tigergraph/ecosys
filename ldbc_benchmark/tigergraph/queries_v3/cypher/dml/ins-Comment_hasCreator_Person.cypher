LOAD CSV FROM 'file:///inserts/dynamic/Comment_hasCreator_Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS commentId,
  toInteger(row[2]) AS personId
MATCH (comment:Comment {id: commentId}), (person:Person {id: personId})
CREATE (comment)-[:HAS_CREATOR {creationDate: creationDate}]->(person)
RETURN count(*)
