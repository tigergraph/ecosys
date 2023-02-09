-- materialize table
CREATE OR REPLACE TABLE tagAndWindowNumMessages AS
    SELECT
        d.anchorDate + INTERVAL (-10 + s.salt) DAY AS startDate,
        d.anchorDate + INTERVAL (-10 + s.salt + 10 + s.salt * 37 % 5) DAY AS endDate,
        name AS tagName,
        sum(frequency) AS frequency
    FROM
        creationDayAndTagNumMessages,
        (SELECT percentile_disc(0.15) WITHIN GROUP (ORDER BY creationDay) AS anchorDate FROM creationDayNumMessages) d,
        (SELECT unnest(generate_series(1, 20)) AS salt) s
    WHERE creationDay BETWEEN d.anchorDate + INTERVAL (-10 + s.salt) DAY
                          AND d.anchorDate + INTERVAL (-10 + s.salt + 10 + s.salt * 37 % 5) DAY -- 10..15 day intervals
    GROUP BY tagName, d.anchorDate, salt
    ORDER BY frequency DESC
