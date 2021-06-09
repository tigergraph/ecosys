MATCH (person1:Person {id: $person1Id})-[knows:KNOWS]->(person2:Person {id: $person2Id})
DELETE knows
