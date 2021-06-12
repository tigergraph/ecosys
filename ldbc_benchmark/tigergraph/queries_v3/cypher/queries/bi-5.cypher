// Q5. Most active Posters of a given Topic
/*
:param tag => 'Abbas_I_of_Persia'
*/
MATCH (tag:Tag {name: $tag})<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(person:Person)
OPTIONAL MATCH (message)<-[likes:LIKES]-(:Person)
WITH person, message, count(likes) AS likeCount
OPTIONAL MATCH (message)<-[:REPLY_OF]-(reply:Comment)
WITH person, message, likeCount, count(reply) AS replyCount
WITH person, count(message) AS messageCount, sum(likeCount) AS likeCount, sum(replyCount) AS replyCount
RETURN
  person.id,
  replyCount,
  likeCount,
  messageCount,
  1*messageCount + 2*replyCount + 10*likeCount AS score
ORDER BY
  score DESC,
  person.id ASC
LIMIT 100
