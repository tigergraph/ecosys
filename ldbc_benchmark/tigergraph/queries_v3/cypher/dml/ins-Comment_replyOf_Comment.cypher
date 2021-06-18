LOAD CSV FROM 'file:///inserts/dynamic/Comment_replyOf_Comment/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS commentId,
  toInteger(row[2]) AS parentCommentId
MATCH (comment:Comment {id: commentId}), (parentComment:Comment {id: parentCommentId})
CREATE (comment)-[:REPLY_OF {creationDate: creationDate}]->(parentComment)
RETURN count(*)
