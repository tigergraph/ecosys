LOAD CSV FROM 'file:///deletes/dynamic/Person_likes_Comment/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  toInteger(row[1]) AS personId,
  toInteger(row[2]) AS commentId
MATCH (:Person {id: personId})-[likes:LIKES]->(:Comment {id: commentId})
DELETE likes
RETURN count(*)
