MATCH (person:Person {id:8796093030404})-[:KNOWS*1..3]-(friend:Person)<-[membership:HAS_MEMBER]-(forum:Forum)
WHERE membership.joinDate> datetime("2012-09-08T00:00:00z")
    AND not(person=friend)
WITH DISTINCT friend, forum
OPTIONAL MATCH (friend)<-[:HAS_CREATOR]-(post:Post)<-[:CONTAINER_OF]-(forum)
WITH forum, count(post) AS postCount
RETURN
  forum.title AS forumTitle,
  postCount
ORDER BY postCount DESC, toInteger(forum.id) ASC
LIMIT 20;
