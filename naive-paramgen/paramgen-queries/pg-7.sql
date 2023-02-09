SELECT tagName AS 'tag:STRING'
FROM tagNumMessages
ORDER BY md5(tagName || 'c')
LIMIT 400
