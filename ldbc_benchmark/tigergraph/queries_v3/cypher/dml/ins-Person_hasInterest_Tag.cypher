LOAD CSV FROM 'file:///inserts/dynamic/Person_hasInterest_Tag/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS personId,
  toInteger(row[2]) AS tagId
MATCH (person:Person {id: personId}), (tag:Tag {id: tagId})
CREATE (person)-[:HAS_INTEREST {creationDate: creationDate}]->(tag)
RETURN count(*)
