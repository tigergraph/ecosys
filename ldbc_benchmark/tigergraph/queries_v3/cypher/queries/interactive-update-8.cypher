MATCH (p1:Person {id:$person1Id}), (p2:Person {id:$person2Id})
CREATE (p1)-[:KNOWS {creationDate:$creationDate}]->(p2)
