MATCH (person:Person {id: $personId})
DETACH DELETE person
