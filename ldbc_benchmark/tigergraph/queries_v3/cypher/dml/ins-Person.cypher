LOAD CSV FROM 'file:///inserts/dynamic/Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS id,
  row[2] AS firstName,
  row[3] AS lastName,
  row[4] AS gender,
  date(row[5]) AS birthday,
  row[6] AS locationIP,
  row[7] AS browserUsed,
  split(row[8], ';') AS speaks,
  split(row[9], ';') AS email
CREATE (p:Person {
    creationDate: creationDate,
    id: id,
    firstName: firstName,
    lastName: lastName,
    gender: gender,
    birthday: birthday,
    locationIP: locationIP,
    browserUsed: browserUsed,
    speaks: speaks,
    email: email
  })
RETURN count(*)
