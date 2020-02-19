MATCH (person:Person {id:30786325583618})-[:KNOWS*1..2]-(friend:Person)
WHERE not(person=friend)
WITH DISTINCT friend
MATCH (friend)-[workAt:WORK_AT]->(company:Organisation)-[:IS_LOCATED_IN]->(:Place {name:"Laos"})
WHERE workAt.workFrom < 2010
RETURN
  friend.id AS personId,
  friend.firstName AS personFirstName,
  friend.lastName AS personLastName,
  company.name AS organizationName,
  workAt.workFrom AS organizationWorkFromYear
ORDER BY organizationWorkFromYear ASC, toInteger(personId) ASC, organizationName DESC
LIMIT 10;
