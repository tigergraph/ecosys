SELECT
    tagA AS 'tagA:STRING',
    date_trunc('day', dateA) AS 'dateA:DATE',
    tagB AS 'tagB:STRING',
    date_trunc('day', dateB) AS 'dateB:DATE',
    3 + (extract('dayofyear' FROM dateA) + extract('dayofyear' FROM dateB)) % 4 AS 'maxKnowsLimit:INT'
FROM (
    SELECT
        tagDatesA.tagName AS tagA,
        tagDatesA.creationDay AS dateA,
        tagDatesB.tagName AS tagB,
        tagDatesB.creationDay AS dateB
    FROM
        (SELECT
            max(creationDay) AS creationDay,
            name AS tagName,
            frequency AS freq,
            abs(frequency - (SELECT percentile_disc(1.00) WITHIN GROUP (ORDER BY frequency) FROM creationDayAndTagNumMessages)) diff
         FROM creationDayAndTagNumMessages
         GROUP BY tagName, freq, diff
         ORDER BY diff, md5(tagName)
         LIMIT 100
        ) tagDatesA,
        (SELECT
            min(creationDay) AS creationDay,
            name AS tagName,
            frequency AS freq,
            abs(frequency - (SELECT percentile_disc(0.995) WITHIN GROUP (ORDER BY frequency) FROM creationDayAndTagNumMessages)) diff
         FROM creationDayAndTagNumMessages
         GROUP BY tagName, freq, diff
         ORDER BY diff, md5(tagName)
         LIMIT 100
        ) tagDatesB
)
WHERE tagA <> tagB
ORDER BY md5(concat(tagA, tagB)), dateA, dateB
LIMIT 400
