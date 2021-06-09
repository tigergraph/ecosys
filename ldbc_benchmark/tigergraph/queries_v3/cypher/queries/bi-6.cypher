// Q6. Most authoritative users on a given topic
/*
:param tag => 'Pyrenees'
*/
MATCH (tag:Tag {name: $tag})<-[:HAS_TAG]-(message2:Message)-[:HAS_CREATOR]->(person1)
OPTIONAL MATCH (message2)<-[:LIKES]-(person2:Person)
OPTIONAL MATCH (person2)<-[:HAS_CREATOR]-(message3:Message)<-[like:LIKES]-(person3:Person)
RETURN
  person1.id,
  // Using 'DISTINCT like' here ensures that each person2's popularity score is only added once for each person1
  count(DISTINCT like) AS authorityScore
ORDER BY
  authorityScore DESC,
  person1.id ASC
LIMIT 100

// We need to use a redundant computation due to the lack of composable graph queries in the currently supported Cypher version.
// This might change in the future with new Cypher versions and GQL.
