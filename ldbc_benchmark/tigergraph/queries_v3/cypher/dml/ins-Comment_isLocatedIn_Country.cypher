LOAD CSV FROM 'file:///inserts/dynamic/Comment_isLocatedIn_Country/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS commentId,
  toInteger(row[2]) AS countryId
MATCH (comment:Comment {id: commentId}), (country:Country {id: countryId})
CREATE (comment)-[:IS_LOCATED_IN {creationDate: creationDate}]->(country)
RETURN count(*)
