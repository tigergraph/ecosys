MATCH (person:Person {id:$personId}),(post:Post {id:$postId})
CREATE (person)-[:LIKES {creationDate:$creationDate}]->(post)
