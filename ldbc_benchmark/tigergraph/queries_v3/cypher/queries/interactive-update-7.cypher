MATCH
  (author:Person {id: $authorPersonId}),
  (country:Country {id: $countryId}),
  (message:Message {id: $replyToPostId + $replyToCommentId + 1}) // $replyToCommentId is -1 if the message is a reply to a post and vica versa (see spec)
CREATE (author)<-[:HAS_CREATOR]-(c:Comment:Message {id: $commentId, creationDate: $creationDate, locationIP: $locationIP, browserUsed: $browserUsed, content: $content, length: $length})-[:REPLY_OF]->(message), (c)-[:IS_LOCATED_IN]->(country)
WITH c
UNWIND $tagIds AS tagId
    MATCH (t:Tag {id: tagId})
    CREATE (c)-[:HAS_TAG]->(t)
