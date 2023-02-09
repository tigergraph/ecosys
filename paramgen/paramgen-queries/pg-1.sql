SELECT
    strftime(
        creationDay::timestamp + INTERVAL ( epoch(creationDay) / 37 % 24) HOUR
            + INTERVAL ( epoch(creationDay) / 37 % 60) MINUTE
            + INTERVAL ( epoch(creationDay) / 31 % 60) SECOND,
        '%Y-%m-%dT%H:%M:%S.%g+00:00'
        ) AS 'datetime:DATETIME'
FROM (
    SELECT
        creationDay,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.15) WITHIN GROUP (ORDER BY frequency) FROM creationDayNumMessages)) AS diff
    FROM creationDayNumMessages
    ORDER BY diff, creationDay
    LIMIT 80
)
ORDER BY md5(creationDay)
