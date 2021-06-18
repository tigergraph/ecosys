LOAD CSV FROM 'file:///inserts/dynamic/Post_isLocatedIn_Country/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS postId,
  toInteger(row[2]) AS countryId
MATCH (post:Post {id: postId}), (country:Country {id: countryId})
CREATE (post)-[:IS_LOCATED_IN {creationDate: creationDate}]->(country)
RETURN count(*)
