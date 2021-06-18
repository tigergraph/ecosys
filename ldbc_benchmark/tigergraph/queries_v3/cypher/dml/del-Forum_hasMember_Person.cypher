LOAD CSV FROM 'file:///deletes/dynamic/Forum_hasMember_Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  toInteger(row[1]) AS forumId,
  toInteger(row[2]) AS personId
MATCH (:Forum {id: forumId})-[hasMember:HAS_MEMBER]->(:Person {id: personId})
DELETE hasMember
RETURN count(*)
