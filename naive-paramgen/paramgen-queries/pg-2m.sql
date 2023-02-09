-- materialize table
CREATE OR REPLACE TABLE tagClassAndWindowNumMessages AS
    SELECT
        d.anchorDate + INTERVAL (-10 + s.salt) DAY AS date,
        name,
        sum(frequency) AS frequency
    FROM
        creationDayAndTagClassNumMessages,
        (SELECT percentile_disc(0.15) WITHIN GROUP (ORDER BY creationDay) AS anchorDate FROM creationDayNumMessages) d,
        (SELECT unnest(generate_series(1, 20)) AS salt) s
    WHERE creationDay BETWEEN d.anchorDate + INTERVAL (-10 + s.salt) DAY
                        AND d.anchorDate + INTERVAL (-10 + s.salt + 199) DAY -- we count the total 200 days
    GROUP BY name, d.anchorDate, salt
    ORDER BY frequency DESC
