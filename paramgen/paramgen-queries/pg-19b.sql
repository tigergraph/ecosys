SELECT
    city1Id AS 'city1Id:ID',
    city2Id AS 'city2Id:ID'
FROM
    (SELECT
        city1Id,
        city2Id,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.25) WITHIN GROUP (ORDER BY frequency) FROM cityPairsNumFriends)) AS diff
    FROM cityPairsNumFriends
    WHERE country1Id = country2Id
    ORDER BY diff, city1Id, city2Id)
ORDER BY md5(3532569367::bigint*city1Id + 342663089::bigint*city2Id)
LIMIT 400
