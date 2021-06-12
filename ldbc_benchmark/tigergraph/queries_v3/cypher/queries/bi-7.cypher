// Q7. Related Topics
/*
:param tag => 'Enrique_Iglesias'
*/
MATCH
  (tag:Tag {name: $tag})<-[:HAS_TAG]-(message:Message),
  (message)<-[:REPLY_OF]-(comment:Comment)-[:HAS_TAG]->(relatedTag:Tag)
WHERE NOT (comment)-[:HAS_TAG]->(tag)
RETURN
  relatedTag.name,
  count(DISTINCT comment) AS count
ORDER BY
  count DESC,
  relatedTag.name ASC
LIMIT 100
