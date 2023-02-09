SELECT
    id AS 'personId:ID',
    countryName AS 'country:STRING',
    tagClassName AS 'tagClass:STRING',
    3 AS 'minPathDistance:INT',
    4 AS 'maxPathDistance:INT'
FROM
    (SELECT
        name AS countryName,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.55) WITHIN GROUP (ORDER BY frequency) FROM countryNumPersons)) AS diff
    FROM countryNumPersons
    ORDER BY diff, countryName
    LIMIT 20),
    (SELECT
        personNumFriends.id,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.55) WITHIN GROUP (ORDER BY frequency) FROM personNumFriends)) AS diff
    FROM personNumFriends
    -- only keep persons which exist in the benchmark's time window
    JOIN personDays_window
      ON personDays_window.id = personNumFriends.id
    ORDER BY diff, personNumFriends.id
    LIMIT 50),
    (SELECT
        name AS tagClassName,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.55) WITHIN GROUP (ORDER BY frequency) FROM tagClassNumTags)) AS diff
    FROM tagClassNumTags
    ORDER BY diff, tagClassName
    LIMIT 15)
ORDER BY md5(concat(id, countryName, tagClassName))
LIMIT 400
