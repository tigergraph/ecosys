MATCH (person:Person {id: $personId})<-[member:HAS_MEMBER]-(forum:Forum {id: $forumId})
OPTIONAL MATCH (person)<-[moderator:HAS_MODERATOR]-(forum)
DELETE moderator, member
