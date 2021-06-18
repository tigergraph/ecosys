LOAD CSV FROM 'file:///deletes/dynamic/Person_likes_Post/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  toInteger(row[1]) AS personId,
  toInteger(row[2]) AS postId
MATCH (:Person {id: personId})-[likes:LIKES]->(:Post {id: postId})
DELETE likes
RETURN count(*)
