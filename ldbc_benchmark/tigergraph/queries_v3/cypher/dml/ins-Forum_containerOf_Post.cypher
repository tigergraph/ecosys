LOAD CSV FROM 'file:///inserts/dynamic/Forum_containerOf_Post/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS forumId,
  toInteger(row[2]) AS postId
MATCH (forum:Forum {id: forumId}), (post:Post {id: postId})
CREATE (forum)-[:CONTAINER_OF {creationDate: creationDate}]->(post)
RETURN count(*)
