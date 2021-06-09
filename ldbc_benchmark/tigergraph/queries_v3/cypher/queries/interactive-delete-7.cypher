MATCH (comment:Comment {id: $commentId})<-[:REPLY_OF*]-(replies:Comment)
DETACH DELETE comment, replies
