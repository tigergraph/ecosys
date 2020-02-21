MATCH (:Person {id:13194139542834})-[:KNOWS*1..2]-(friend:Person)<-[:HAS_CREATOR]-(message:Message)
WHERE message.creationDate < datetime("2011-12-07T00:00:00z")
RETURN DISTINCT
  friend.id AS personId,
  friend.firstName AS personFirstName,
  friend.lastName AS personLastName,
  message.id AS messageId,
  CASE exists(message.content)
    WHEN true THEN message.content
    ELSE message.imageFile
  END AS messageContent,
  message.creationDate AS messageCreationDate
ORDER BY message.creationDate DESC, toInteger(message.id) ASC
LIMIT 20;
