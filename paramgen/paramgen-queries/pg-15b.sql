SELECT DISTINCT
    people2Hops.person1Id AS 'person1Id:ID',
    people2Hops.person2Id AS 'person2Id:ID',
    (SELECT date_trunc('day', percentile_disc(0.10) WITHIN GROUP (ORDER BY creationDay)) AS startAnchorDate FROM creationDayNumMessages) + INTERVAL (-11 + people2Hops.person1Id % 9) DAY AS 'startDate:DATE',
    (SELECT date_trunc('day', percentile_disc(0.10) WITHIN GROUP (ORDER BY creationDay)) AS endAnchorDate   FROM creationDayNumMessages) + INTERVAL ( 11 + people2Hops.person1Id % 9) DAY AS 'endDate:DATE'
FROM people2Hops
-- only person pairs which are reachable through 2 hops in the benchmark's time window
JOIN personKnowsPersonDays_window knows1
  ON knows1.person1Id = people2Hops.person1Id
JOIN personKnowsPersonDays_window knows2
  ON knows2.person1Id = knows1.person2Id
 AND knows2.person2Id = people2Hops.person2Id
ORDER BY md5(131*people2Hops.person1Id + 241*people2Hops.person2Id), md5(people2Hops.person1Id)
LIMIT 400
