// Q19. Interaction path between cities
// Requires the Neo4j Graph Data Science library
/*
:param [{ city1Id, city2Id }] => {RETURN 5 AS city1Id, 6 AS city2Id}
*/
MATCH
  (person1:Person)-[:IS_LOCATED_IN]->(city1:City {id: $city1Id}),
  (person2:Person)-[:IS_LOCATED_IN]->(city2:City {id: $city2Id})
CALL gds.shortestPath.dijkstra.stream({
  nodeQuery: 'MATCH (p:Person) RETURN id(p) AS id',
  relationshipQuery:
    'MATCH
       (personA:Person)-[:KNOWS]-(personB:Person),
       (personA)<-[:HAS_CREATOR]-(:Message)-[replyOf:REPLY_OF]-(:Message)-[:HAS_CREATOR]->(personB)
     RETURN
        id(personA) AS source,
        id(personB) AS target,
        1.0/count(replyOf) AS weight',
  sourceNode: person1,
  targetNode: person2,
  relationshipWeightProperty: 'weight'
})
YIELD totalCost
RETURN person1.id, person2.id, totalCost AS totalWeight
ORDER BY totalWeight DESC, person1.id ASC, person2.id ASC
LIMIT 20
