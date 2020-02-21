MATCH (person:Person {id:17592186055119})-[:KNOWS*1..2]-(friend:Person)<-[:HAS_CREATOR]-(messageX:Message), 
      (messageX)-[:IS_LOCATED_IN]->(countryX:Place) 
WHERE not(person=friend) AND not((friend)-[:IS_LOCATED_IN]->()-[:IS_PART_OF]->(countryX)) AND countryX.name= 'Laos' AND messageX.creationDate>= datetime('2011-05-31T17:00:00z') AND messageX.creationDate< datetime('2011-07-12T17:00:00z') 
WITH friend, count(DISTINCT messageX) AS xCount 
MATCH (friend)<-[:HAS_CREATOR]-(messageY:Message)-[:IS_LOCATED_IN]->(countryY:Place) 
WHERE countryY.name='Scotland' AND not((friend)-[:IS_LOCATED_IN]->()-[:IS_PART_OF]->(countryY)) AND messageY.creationDate>=datetime('2011-05-31T17:00:00z') AND 
messageY.creationDate< datetime('2011-07-12T17:00:00z') 
WITH friend.id AS personId, friend.firstName AS personFirstName, friend.lastName AS personLastName, xCount, count(DISTINCT messageY) AS yCount 
RETURN personId, personFirstName, personLastName, xCount, yCount, xCount + yCount AS count 
ORDER BY count DESC, toInteger(personId) ASC LIMIT 20;
