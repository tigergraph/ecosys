// Q20. Recruitment
// Requires the Neo4j Graph Data Science library
/*
:param [{ company, person2Id }] => {
  RETURN
    'Pamir_Airways' AS company,
    15393162792760 AS person2Id
  }
*/
MATCH
  (company:Company {name: $company})<-[:WORK_AT]-(person1:Person),
  (person2:Person {id: $person2Id})
CALL gds.shortestPath.dijkstra.stream({
  nodeQuery: 'MATCH (p:Person) RETURN id(p) AS id',
  relationshipQuery:
    'MATCH
       (personA:Person)-[:KNOWS]-(personB:Person),
       (personA)-[saA:STUDY_AT]->(u:University)<-[saB:STUDY_AT]-(personB)
     RETURN
        id(personA) AS source,
        id(personB) AS target,
        abs(saA.classYear - saB.classYear) + 1 AS weight',
  sourceNode: person1,
  targetNode: person2,
  relationshipWeightProperty: 'weight'
})
YIELD totalCost
WHERE person1.id <> $person2Id
RETURN person1.id, totalCost AS totalWeight
ORDER BY totalWeight ASC, person1.id ASC
LIMIT 20
