SELECT tagName AS 'tag:STRING'
FROM (
    SELECT
        tagName,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.98) WITHIN GROUP (ORDER BY frequency) FROM tagNumMessages)) AS diff
    FROM tagNumMessages
    ORDER BY diff, tagName
    LIMIT 200
)
ORDER BY md5(tagName)
