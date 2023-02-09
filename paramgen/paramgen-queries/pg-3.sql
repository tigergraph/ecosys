SELECT
    tagClassName AS 'tagClass:STRING',
    country AS 'country:STRING'
FROM (
    SELECT
        name AS tagClassName,
        abs(frequency - (SELECT percentile_disc(0.82) WITHIN GROUP (ORDER BY frequency) FROM tagClassNumMessages)) AS diff,
        CASE id % 2 == 0 WHEN true THEN 'China' ELSE 'India' END AS country
    FROM tagClassNumMessages
    ORDER BY diff, tagClassName
    LIMIT 80
)
