// Q4. Top message creators in a country
/*
:param date => datetime('2012-09-01') AS date
*/
MATCH (country:Country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(person:Person)<-[:HAS_MEMBER]-(forum:Forum)
WHERE forum.creationDate > $date
WITH country, forum, count(person) AS numberOfMembers
ORDER BY numberOfMembers DESC, forum.id ASC, country.id
WITH DISTINCT forum, max(numberOfMembers) AS maxNumberOfMembers
LIMIT 100
WITH collect(forum) AS popularForums
UNWIND popularForums AS forum
MATCH
  (forum)-[:HAS_MEMBER]->(person:Person)
OPTIONAL MATCH
  (person)<-[:HAS_CREATOR]-(message:Message)-[:REPLY_OF*0..]->(post:Post)<-[:CONTAINER_OF]-(popularForum:Forum)
WHERE popularForum IN popularForums
RETURN
  person.id,
  person.firstName,
  person.lastName,
  person.creationDate,
  count(DISTINCT message) AS messageCount
ORDER BY
  messageCount DESC,
  person.id ASC
LIMIT 100
