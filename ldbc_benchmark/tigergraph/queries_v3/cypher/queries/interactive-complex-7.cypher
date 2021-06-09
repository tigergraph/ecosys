MATCH (person:Person {id:$personId})<-[:HAS_CREATOR]-(message:Message)<-[like:LIKES]-(liker:Person)
WITH liker, message, like.creationDate AS likeTime, person
ORDER BY likeTime DESC, toInteger(message.id) ASC
WITH
  liker,
  head(collect({msg: message, likeTime: likeTime})) AS latestLike,
  person
RETURN
  liker.id AS personId,
  liker.firstName AS personFirstName,
  liker.lastName AS personLastName,
  latestLike.likeTime AS likeCreationDate,
  latestLike.msg.id AS messageId,
  CASE exists(latestLike.msg.content)
    WHEN true THEN latestLike.msg.content
    ELSE latestLike.msg.imageFile
  END AS messageContent,
  latestLike.likeTime.minutes - latestLike.msg.creationDate.minutes AS minutesLatency,
  not((liker)-[:KNOWS]-(person)) AS isNew
ORDER BY likeCreationDate DESC, toInteger(personId) ASC
LIMIT 20
