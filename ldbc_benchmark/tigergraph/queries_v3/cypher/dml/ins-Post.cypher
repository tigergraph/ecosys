LOAD CSV FROM 'file:///inserts/dynamic/Post/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS id,
  row[2] AS imageFile,
  row[3] AS locationIP,
  row[4] AS browserUsed,
  row[5] AS language,
  row[6] AS content,
  toInteger(row[7]) AS length
CREATE (post:Post:Message {
    creationDate: creationDate,
    id: id,
    imageFile: imageFile,
    locationIP: locationIP,
    browserUsed: browserUsed,
    language: language,
    content: content
  })
RETURN count(*)
