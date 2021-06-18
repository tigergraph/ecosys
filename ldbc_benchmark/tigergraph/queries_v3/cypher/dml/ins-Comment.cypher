LOAD CSV FROM 'file:///inserts/dynamic/Comment/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS id,
  row[2] AS locationIP,
  row[3] AS browserUsed,
  row[4] AS content,
  toInteger(row[5]) AS length
CREATE (c:Comment:Message {
    creationDate: creationDate,
    id: id,
    locationIP: locationIP,
    browserUsed: browserUsed,
    content: content,
    length: length
  })
RETURN count(*)
