SELECT
    countryNumPersons.name AS 'country:STRING',
    creationDay::date + INTERVAL (-15 + salt*37 % 30) DAY AS 'startDate:DATE',
    creationDay::date + INTERVAL (-15 + salt*37 % 30 + 92 + salt*47 % 18) DAY AS 'endDate:DATE'
FROM
    countryNumPersons,
    creationDayNumMessages,
    (SELECT unnest(generate_series(1, 3)) AS salt)
ORDER BY md5(concat(creationDay, countryNumPersons.name, salt))
