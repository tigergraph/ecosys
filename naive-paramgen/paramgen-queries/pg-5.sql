SELECT tagName AS 'tag:STRING'
FROM tagNumMessages
ORDER BY md5(tagName || 'a')
LIMIT 400
