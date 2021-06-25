MATCH (n:Comment)
WITH count(n) as nComment
MATCH (n:Post)
WITH nComment, count(n) as nPost
MATCH (n:Forum)
WITH nComment, nPost, count(n) as nForum
MATCH (n:Person)
WITH nComment, nPost, nForum, count(n) as nPerson
MATCH (:Message)-[e:HAS_TAG]->(:Tag)
WITH nComment, nPost, nForum, nPerson, count(e) as message_has_tag
MATCH (:Forum)-[e:HAS_TAG]->(:Tag)
WITH nComment, nPost, nForum, nPerson, message_has_tag, count(e) as forum_has_tag
WITH nComment, nPost, nForum, nPerson, message_has_tag + forum_has_tag as has_tag
MATCH (:Person)-[e:LIKES]->(:Message)
WITH nComment, nPost, nForum, nPerson, has_tag, count(e) as likes
MATCH (:Person)-[e:KNOWS]->(:Person)
WITH nComment, nPost, nForum, nPerson, has_tag, likes, count(e) as knows
MATCH (:Comment)-[e:REPLY_OF]->(:Message)
RETURN nComment, nPost, nForum, nPerson, has_tag, likes, knows, count(e) as reply_of