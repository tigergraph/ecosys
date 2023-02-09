SELECT
    city1Id AS 'city1Id:ID',
    city2Id AS 'city2Id:ID'
FROM cityPairsNumFriends
ORDER BY md5(3532569367::bigint*city1Id + 342663089::bigint*city2Id)
LIMIT 400
