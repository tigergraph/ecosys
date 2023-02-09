SELECT tagName AS 'tag:STRING'
FROM (
    SELECT
        tagName,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.88) WITHIN GROUP (ORDER BY frequency) FROM tagNumMessages)) AS diff
    FROM tagNumMessages
    ORDER BY diff, tagName
    LIMIT 400
)
ORDER BY md5(tagName)
