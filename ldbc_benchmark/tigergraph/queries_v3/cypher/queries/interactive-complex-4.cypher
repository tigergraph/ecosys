MATCH (person:Person {id:$personId})-[:KNOWS]-(:Person)<-[:HAS_CREATOR]-(post:Post)-[:HAS_TAG]->(tag:Tag)
WHERE post.creationDate >= $startDate
   AND post.creationDate < $endDate
WITH person, count(post) AS postsOnTag, tag
OPTIONAL MATCH (person)-[:KNOWS]-()<-[:HAS_CREATOR]-(oldPost:Post)-[:HAS_TAG]->(tag)
WHERE oldPost.creationDate < $startDate
WITH person, postsOnTag, tag, count(oldPost) AS cp
WHERE cp = 0
RETURN
  tag.name AS tagName,
  sum(postsOnTag) AS postCount
ORDER BY postCount DESC, tagName ASC
LIMIT 10
