LOAD CSV FROM 'file:///inserts/dynamic/Person_studyAt_University/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS personId,
  toInteger(row[2]) AS universityId,
  toInteger(row[3]) AS classYear
MATCH (person:Person {id: personId}), (university:University {id: universityId})
CREATE (person)-[:STUDY_AT {creationDate: creationDate, classYear: classYear}]->(university)
RETURN count(*)
