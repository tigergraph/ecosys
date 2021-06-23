LOAD CSV FROM 'file:///inserts/dynamic/Person_isLocatedIn_City/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS personId,
  toInteger(row[2]) AS cityId
MATCH (person:Person {id: personId}), (city:City {id: cityId})
CREATE (person)-[:IS_LOCATED_IN {creationDate: creationDate}]->(city)
RETURN count(*)
