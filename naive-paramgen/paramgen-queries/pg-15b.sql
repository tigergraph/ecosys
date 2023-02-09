SELECT
    p1.id AS 'person1Id:ID',
    p2.id AS 'person2Id:ID',
    (SELECT date_trunc('day', creationDay) FROM creationDayNumMessages ORDER BY md5(creationDay) ASC ) - INTERVAL (p1.id % 7) DAY AS 'startDate:DATE',
    (SELECT date_trunc('day', creationDay) FROM creationDayNumMessages ORDER BY md5(creationDay) DESC) + INTERVAL (p2.id % 7) DAY AS 'endDate:DATE'
FROM
    (SELECT id FROM personNumFriends ORDER BY md5(id) ASC  LIMIT 20) p1,
    (SELECT id FROM personNumFriends ORDER BY md5(id) DESC LIMIT 20) p2
ORDER BY md5(131::bigint*p1.id + 241::bigint*p2.id), md5(p1.id)
LIMIT 400
