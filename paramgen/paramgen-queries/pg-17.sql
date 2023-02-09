SELECT
    tagName AS 'tag:STRING',
    8 + (SELECT sum(x) FROM (SELECT unicode(unnest(string_split(tagName, ''))) AS x)) % 9 AS 'delta:INT'
FROM (
    SELECT
        tagName,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.27) WITHIN GROUP (ORDER BY frequency) FROM tagNumMessages)) AS diff
    FROM tagNumMessages
    ORDER BY diff, tagName
    LIMIT 400
)
ORDER BY md5(tagName)
