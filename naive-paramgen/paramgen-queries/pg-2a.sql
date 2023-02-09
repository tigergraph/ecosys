SELECT
    date_trunc('day', date) AS 'date:DATE',
    name AS 'tagClass:STRING'
FROM tagClassAndWindowNumMessages
ORDER BY md5(concat(date, name))
LIMIT 400
