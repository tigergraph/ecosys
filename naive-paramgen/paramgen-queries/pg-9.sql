SELECT
    creationDay::date + INTERVAL (-5 + salt*37 % 10) DAY AS 'startDate:DATE',
    creationDay::date + INTERVAL (80 + salt*31 % 20) DAY AS 'endDate:DATE'
FROM
    creationDayNumMessages,
    (SELECT unnest(generate_series(1, 20)) AS salt)
