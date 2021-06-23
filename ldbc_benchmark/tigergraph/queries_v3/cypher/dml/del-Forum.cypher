LOAD CSV FROM 'file:///deletes/dynamic/Forum/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH toInteger(row[1]) AS id
MATCH (forum:Forum {id: id})
OPTIONAL MATCH (forum)-[:CONTAINER_OF]->(:Post)<-[:REPLY_OF*0..]-(message:Message)
DETACH DELETE forum, message
RETURN count(*)
