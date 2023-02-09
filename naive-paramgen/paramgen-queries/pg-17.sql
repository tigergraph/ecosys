SELECT
    tagName AS 'tag:STRING',
    8 + (SELECT sum(x) FROM (SELECT unicode(unnest(string_split(tagName, ''))) AS x)) % 9 AS 'delta:INT'
FROM tagNumMessages
ORDER BY md5(tagName || 'e')
LIMIT 400
