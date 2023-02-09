SELECT
    tagClassNumMessages.name AS 'tagClass:STRING',
    countryNumPersons.name AS 'country:STRING'
FROM
    tagClassNumMessages,
    countryNumPersons
ORDER BY md5(concat(tagClassNumMessages.name, countryNumPersons.name))
