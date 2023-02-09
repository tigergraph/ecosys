SELECT
    date_trunc('day', date) AS 'date:DATE',
    tagClassName AS 'tagClass:STRING'
FROM (
    SELECT
        date,
        tagClassName,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.62) WITHIN GROUP (ORDER BY frequency) FROM tagClassAndWindowNumMessages)) AS diff
    FROM tagClassAndWindowNumMessages
    ORDER BY diff, date, tagClassName
    LIMIT 400
)
ORDER BY md5(concat(tagClassName, date))
