LOAD CSV FROM 'file:///inserts/dynamic/Person_likes_Post/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS personId,
  toInteger(row[2]) AS postId
MATCH (person:Person {id: personId}), (post:Post {id: postId})
CREATE (person)-[:LIKES {creationDate: creationDate}]->(post)
RETURN count(*)
