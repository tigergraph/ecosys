MATCH (forum:Forum {id: $forumId})-[:CONTAINER_OF]->(posts:Post)<-[:REPLY_OF*]-(comments:Comment)
DETACH DELETE forum, posts, comments
