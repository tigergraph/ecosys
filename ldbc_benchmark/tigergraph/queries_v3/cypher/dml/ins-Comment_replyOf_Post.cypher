LOAD CSV FROM 'file:///inserts/dynamic/Comment_replyOf_Post/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS commentId,
  toInteger(row[2]) AS postId
MATCH (comment:Comment {id: commentId}), (post:Post {id: postId})
CREATE (comment)-[:REPLY_OF {creationDate: creationDate}]->(post)
RETURN count(*)
