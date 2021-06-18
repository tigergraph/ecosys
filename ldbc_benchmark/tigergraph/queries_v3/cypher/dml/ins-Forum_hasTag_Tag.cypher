LOAD CSV FROM 'file:///inserts/dynamic/Forum_hasTag_Tag/' + $batch + '/' + $csv_file AS row FIELDTERMINATOR '|'
WITH
  datetime(row[0]) AS creationDate,
  toInteger(row[1]) AS forumId,
  toInteger(row[2]) AS tagId
MATCH (forum:Forum {id: forumId}), (tag:Tag {id: tagId})
CREATE (forum)-[:HAS_TAG {creationDate: creationDate}]->(tag)
RETURN count(*)
