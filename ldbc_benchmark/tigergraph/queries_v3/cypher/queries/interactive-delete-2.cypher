MATCH (person:Person {id: $personId})-[likes:LIKES]->(post:Post {id: $postId})
DELETE likes
