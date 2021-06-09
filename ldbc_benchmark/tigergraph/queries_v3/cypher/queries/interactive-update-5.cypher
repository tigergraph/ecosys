MATCH (f:Forum {id:$forumId}), (p:Person {id:$personId})
CREATE (f)-[:HAS_MEMBER {joinDate:$joinDate}]->(p)
