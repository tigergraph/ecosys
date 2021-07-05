LOAD CSV FROM 'file:///deletes/dynamic/Person/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH toInteger(row[1]) AS id
MATCH (person:Person {id: id})
// DEL 6/7: Post/Comment
OPTIONAL MATCH (person)<-[:HAS_CREATOR]-(:Message)<-[:REPLY_OF*0..]-(message1:Message)
// DEL 4: Forum
OPTIONAL MATCH (person)<-[:HAS_MODERATOR]-(forum:Forum)
WHERE forum.title STARTS WITH 'Album '
   OR forum.title STARTS WITH 'Wall '
OPTIONAL MATCH (forum)-[:CONTAINER_OF]->(:Post)<-[:REPLY_OF*0..]-(message2:Message)
DETACH DELETE person, forum, message1, message2
RETURN count(*)
