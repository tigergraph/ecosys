LOAD CSV FROM 'file:///inserts/dynamic/Forum_hasModerator_Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS forumId,
  toInteger(row[2]) AS personId
MATCH (forum:Forum {id: forumId}), (person:Person {id: personId})
CREATE (forum)-[:HAS_MODERATOR {creationDate: creationDate}]->(person)
RETURN count(*)
