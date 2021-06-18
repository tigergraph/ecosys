LOAD CSV FROM 'file:///deletes/dynamic/Comment/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH toInteger(row[1]) AS id
MATCH (:Comment {id: id})<-[:REPLY_OF*0..]-(comment:Comment)
DETACH DELETE comment
RETURN count(*)
