SELECT
    CASE salt2 % 2 == 0 WHEN true THEN 'India' ELSE 'China' END AS 'country:STRING',
    endDate AS 'endDate:DATE'
FROM (
    SELECT DISTINCT
        anchorDate::date + INTERVAL (-5 + salt1*47 % 20) DAY AS endDate
    FROM (
        SELECT percentile_disc(0.94) WITHIN GROUP (ORDER BY creationDay) AS anchorDate
        FROM creationDayNumMessages
        ),
        (SELECT unnest(generate_series(456789, 456789+20)) AS salt1)
    ),
    (SELECT unnest(generate_series(0, 1)) AS salt2)
ORDER BY md5(concat(endDate, salt2))
