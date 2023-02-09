SELECT
    tagName AS 'tag:STRING',
    date_trunc('day', startDate) AS 'startDate:DATE',
    date_trunc('day', endDate) AS 'endDate:DATE'
FROM (
    SELECT
        tagName,
        startDate,
        endDate,
        abs(frequency - (SELECT percentile_disc(0.31) WITHIN GROUP (ORDER BY frequency) FROM tagAndWindowNumMessages)) diff
    FROM tagAndWindowNumMessages
    ORDER BY diff, tagName, startDate
    LIMIT 100
)
ORDER BY md5(concat(tagName, startDate))
