LOAD CSV FROM 'file:///deletes/dynamic/Person_knows_Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  toInteger(row[1]) AS person1Id,
  toInteger(row[2]) AS person2Id
MATCH (:Person {id: person1Id})-[knows:KNOWS]-(:Person {id: person2Id})
DELETE knows
RETURN count(*)
