MATCH (post:Post {id: $postId})<-[:REPLY_OF*]-(replies:Comment)
DETACH DELETE post, replies
