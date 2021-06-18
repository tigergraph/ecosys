LOAD CSV FROM 'file:///deletes/dynamic/Post/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH toInteger(row[1]) AS id
MATCH (:Post {id: id})<-[:REPLY_OF*0..]-(message:Message) // DEL 6/7
DETACH DELETE message
RETURN count(*)
