SELECT
    CASE salt2 % 2 == 0 WHEN true THEN 'China' ELSE 'India' END AS 'country:STRING',
    startDate AS 'startDate:DATE',
    endDate AS 'endDate:DATE'
FROM (
    SELECT
        anchorDate::date + INTERVAL (-15 + salt1*37 % 30) DAY AS startDate,
        anchorDate::date + INTERVAL (-15 + salt1*37 % 30  +  92 + salt2*47 % 18 + salt2) DAY AS endDate,
        salt2
    FROM (
            SELECT percentile_disc(0.92) WITHIN GROUP (ORDER BY creationDay) AS anchorDate
            FROM creationDayNumMessages
        ),
        (SELECT unnest(generate_series(1, 20)) AS salt1),
        (SELECT unnest(generate_series(0, 1)) AS salt2)
)
ORDER BY md5(concat(startDate, endDate))
