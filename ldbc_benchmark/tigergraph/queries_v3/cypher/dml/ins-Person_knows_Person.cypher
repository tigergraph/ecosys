LOAD CSV FROM 'file:///inserts/dynamic/Person_knows_Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS person1Id,
  toInteger(row[2]) AS person2Id
MATCH (person1:Person {id: person1Id}), (person2:Person {id: person2Id})
CREATE (person1)-[:KNOWS {creationDate: creationDate}]->(person2)
RETURN count(*)
