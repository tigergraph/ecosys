MATCH (person:Person {id: $personId})-[likes:LIKES]->(comment:Comment {id: $commentId})
DELETE likes
