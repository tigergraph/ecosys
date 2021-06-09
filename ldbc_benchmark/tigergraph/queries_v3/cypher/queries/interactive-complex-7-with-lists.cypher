MATCH (person:Person {id:$personId})<-[:HAS_CREATOR]-(message)<-[like:LIKES]-(liker:Person)
WITH liker, message, like.creationDate AS likeTime, person
ORDER BY likeTime DESC, toInteger(message.id) ASC
WITH
  liker,
  collect([message, likeTime]) AS latestLikes,
  person
WITH
  liker,
  head(latestLikes) AS latestLike,
  person
RETURN
  liker.id AS personId,
  liker.firstName AS personFirstName,
  liker.lastName AS personLastName,
  latestLike[1] AS likeCreationDate,
  latestLike[0].id AS messageId,
  CASE exists(latestLike[0].content)
    WHEN true THEN latestLike[0].content
    ELSE latestLike[0].imageFile
  END AS messageContent,
  latestLike[0].creationDate AS messageCreationDate,
  not((liker)-[:KNOWS]-(person)) AS isNew
ORDER BY likeCreationDate DESC, toInteger(personId) ASC
LIMIT 20
