// Q17. Information propagation analysis
/*
:param [{ tag, delta }] => {RETURN 'Snowboard' AS tag, 10 AS delta}
*/
MATCH
  (tag:Tag {name: $tag}),
  (person1:Person)<-[:HAS_CREATOR]-(message1:Message)-[:REPLY_OF*0..]->(post1:Post)<-[:CONTAINER_OF]-(forum1:Forum),
  (message1)-[:HAS_TAG]->(tag),
  (forum1)<-[:HAS_MEMBER]->(person2:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:HAS_TAG]->(tag),
  (forum1)<-[:HAS_MEMBER]->(person3:Person)<-[:HAS_CREATOR]-(message2:Message)-[:HAS_TAG]->(tag),
  (comment)-[:REPLY_OF]->(message2)-[:REPLY_OF*0..]->(post2:Post)<-[:CONTAINER_OF]-(forum2:Forum)
WHERE forum1 <> forum2
  AND message2.creationDate > message1.creationDate + duration({hours: $delta})
  AND NOT (forum2)-[:HAS_MEMBER]->(person1)
RETURN person1.id, count(message2) AS messageCount
ORDER BY person1.id ASC
LIMIT 10
