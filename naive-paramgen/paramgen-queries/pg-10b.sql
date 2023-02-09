SELECT
    personNumFriends_sample.id AS 'personId:ID',
    countryNumPersons.name AS 'country:STRING',
    tagClassNumTags.name AS 'tagClass:STRING',
    3 AS 'minPathDistance:INT',
    4 AS 'maxPathDistance:INT'
FROM
    countryNumPersons,
    (SELECT * FROM personNumFriends ORDER BY md5(id) LIMIT 100) personNumFriends_sample,
    tagClassNumTags
ORDER BY md5(concat(personNumFriends_sample.id, countryNumPersons.name, tagClassNumTags.name))
LIMIT 400
