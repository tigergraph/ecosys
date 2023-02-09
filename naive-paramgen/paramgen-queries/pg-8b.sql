SELECT
    name AS 'tag:STRING', 
    date_trunc('day', startDate) AS 'startDate:DATE',
    date_trunc('day', endDate) AS 'endDate:DATE'
FROM tagAndWindowNumMessages
ORDER BY md5(concat(name, startDate))
LIMIT 400
