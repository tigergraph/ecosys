MATCH (n:Comment)
RETURN count(n) as nComment;

MATCH (n:Post)
RETURN count(n) as nPost;

MATCH (n:Person)
RETURN count(n) as nPerson;

MATCH (n:Forum)
RETURN count(n) as nForum;

MATCH (n:Tag)
RETURN count(n) as nTag;

MATCH (f:Forum)-[e:HAS_MEMBER]->(t:Person)
RETURN count(e) as nHasMember;

MATCH (s:Person)-[e:KNOWS]->(t:Person)
RETURN count(e) as nKnows;

MATCH (s:Person)-[e:LIKES]->(t:Post)
RETURN count(e) as nlikePost;

MATCH (s:Person)-[e:LIKES]->(t:Comment)
RETURN count(e) as nLikeComment;


MATCH (:Person)-[e:IS_LOCATED_IN]->(:City)
RETURN count(e) as nLoc;