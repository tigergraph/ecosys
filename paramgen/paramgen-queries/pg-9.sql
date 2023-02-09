SELECT DISTINCT
      startDate AS 'startDate:DATE',
      endDate AS 'endDate:DATE'
FROM (
      SELECT
            anchorDate::date + INTERVAL (-5 + salt1*37 % 10) DAY AS startDate,
            anchorDate::date + INTERVAL (80 + salt1*31 % 20 + salt2) DAY AS endDate
      FROM (
            SELECT percentile_disc(0.89) WITHIN GROUP (ORDER BY creationDay) AS anchorDate
            FROM creationDayNumMessages
      ),
      (SELECT unnest(generate_series(1, 20)) AS salt1),
      (SELECT unnest(generate_series(0, 1)) AS salt2),
)
ORDER BY md5(concat(startDate, endDate))
