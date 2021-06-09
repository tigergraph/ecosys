// Q15. Weighted interaction paths
/*
:param [{ person1Id, person2Id, startDate, endDate }] => {
  RETURN
    2 AS person1Id,
    4 AS person2Id,
    datetime('2011-06-01') AS startDate,
    datetime('2012-05-31') AS endDate
}
*/
MATCH
  path=allShortestPaths((p1:Person {id: $person1Id})-[:KNOWS*]-(p2:Person {id: $person2Id}))
UNWIND relationships(path) AS k
WITH
  path,
  startNode(k) AS pA,
  endNode(k) AS pB,
  0 AS relationshipWeights

// case 1, A to B
// every reply (by one of the Persons) to a Post (by the other Person): 1.0
OPTIONAL MATCH
  (pA)<-[:HAS_CREATOR]-(c:Comment)-[:REPLY_OF]->(post:Post)-[:HAS_CREATOR]->(pB),
  (post)<-[:CONTAINER_OF]-(forum:Forum)
WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
WITH path, pA, pB, relationshipWeights + count(c)*1.0 AS relationshipWeights

// case 2, A to B
// every reply (by ones of the Persons) to a Comment (by the other Person): 0.5
OPTIONAL MATCH
  (pA)<-[:HAS_CREATOR]-(c1:Comment)-[:REPLY_OF]->(c2:Comment)-[:HAS_CREATOR]->(pB),
  (c2)-[:REPLY_OF*]->(:Post)<-[:CONTAINER_OF]-(forum:Forum)
WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
WITH path, pA, pB, relationshipWeights + count(c1)*0.5 AS relationshipWeights

// case 1, B to A
// every reply (by one of the Persons) to a Post (by the other Person): 1.0
OPTIONAL MATCH
  (pB)<-[:HAS_CREATOR]-(c:Comment)-[:REPLY_OF]->(post:Post)-[:HAS_CREATOR]->(pA),
  (post)<-[:CONTAINER_OF]-(forum:Forum)
WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
WITH path, pA, pB, relationshipWeights + count(c)*1.0 AS relationshipWeights

// case 2, B to A
// every reply (by ones of the Persons) to a Comment (by the other Person): 0.5
OPTIONAL MATCH
  (pB)<-[:HAS_CREATOR]-(c1:Comment)-[:REPLY_OF]->(c2:Comment)-[:HAS_CREATOR]->(pA),
  (c2)-[:REPLY_OF*]->(:Post)<-[:CONTAINER_OF]-(forum:Forum)
WHERE forum.creationDate >= $startDate AND forum.creationDate <= $endDate
WITH path, pA, pB, relationshipWeights + count(c1)*0.5 AS relationshipWeights

WITH
  [person IN nodes(path) | person.id] AS personIds,
  sum(relationshipWeights) AS weight

RETURN
  personIds,
  weight
ORDER BY
  weight DESC,
  personIds ASC
