SELECT
    tagA AS 'tagA:STRING',
    date_trunc('day', dateA) AS 'dateA:DATE',
    tagB AS 'tagB:STRING',
    date_trunc('day', dateB) AS 'dateB:DATE',
    3 + (extract('dayofyear' FROM dateA)+extract('dayofyear' FROM dateB)) % 4 AS 'maxKnowsLimit:INT'
FROM (
    SELECT
        tagDatesA.name AS tagA,
        tagDatesA.creationDay AS dateA,
        tagDatesB.name AS tagB,
        tagDatesB.creationDay AS dateB
    FROM
        (SELECT creationDay, name
         FROM creationDayAndTagNumMessages
         ORDER BY md5(concat(creationDay, name))
         LIMIT 100
        ) tagDatesA,
        (SELECT creationDay, name
         FROM creationDayAndTagNumMessages
         ORDER BY md5(concat(creationDay, name)) DESC
         LIMIT 100
        ) tagDatesB
)
WHERE tagA <> tagB
ORDER BY md5(concat(tagA, tagB)), dateA, dateB
LIMIT 400
