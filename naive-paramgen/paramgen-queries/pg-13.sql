SELECT
    countryNumPersons.name AS 'country:STRING',
    date_trunc('day', creationDay) AS 'endDate:DATE'
FROM
    countryNumPersons,
    creationDayNumMessages
ORDER BY md5(concat(countryNumPersons.name, creationDay))
LIMIT 400
