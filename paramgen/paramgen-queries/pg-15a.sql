SELECT DISTINCT
    p1Id AS 'person1Id:ID',
    p2Id AS 'person2Id:ID',
    (SELECT date_trunc('day', percentile_disc(0.95) WITHIN GROUP (ORDER BY creationDay)) AS startAnchorDate FROM creationDayNumMessages) - INTERVAL (p1Id % 7) DAY AS 'startDate:DATE',
    (SELECT date_trunc('day', percentile_disc(0.95) WITHIN GROUP (ORDER BY creationDay)) AS endAnchorDate   FROM creationDayNumMessages) + INTERVAL (p2Id % 7) DAY AS 'endDate:DATE'

FROM
  (
    SELECT DISTINCT
      people4Hops_sample.person1Id AS p1Id,
      people4Hops_sample.person2Id AS p2Id,
      knows2.person2Id AS middleCandidate
    FROM (
      SELECT *
      FROM people4Hops
      LIMIT 80
    ) people4Hops_sample
    -- two hops from person1Id
    JOIN personKnowsPersonDays_window knows1
      ON knows1.person1Id = people4Hops_sample.person1Id
    JOIN personKnowsPersonDays_window knows2
      ON knows2.person1Id = knows1.person2Id
  ) sub

-- two hops from person2Id
JOIN personKnowsPersonDays_window knows4
  ON knows4.person1Id = p2Id
JOIN personKnowsPersonDays_window knows3
  ON knows3.person1Id = knows4.person2Id

-- meet in the middle
WHERE middleCandidate = knows3.person2Id

ORDER BY md5(131*p1Id + 241*p2Id), md5(p1Id)
