SELECT date_trunc('day', creationDay) AS 'date:DATE'
FROM creationDayNumMessages
ORDER BY md5(creationDay)
