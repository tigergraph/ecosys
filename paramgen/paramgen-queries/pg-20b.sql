-- variant (b): guaranteed that a path exists
SELECT DISTINCT
    comp.companyName AS 'company:STRING',
    k2.person2Id AS 'person2Id:ID'
FROM
    (SELECT
        person1Id,
        companyId,
        companyName,
      FROM (
          SELECT
              personWorkAtCompanyDays_window.personId AS person1Id,
              personWorkAtCompanyDays_window.companyId AS companyId,
              name AS companyName,
              frequency AS freq,
              abs(frequency - (SELECT percentile_disc(0.47) WITHIN GROUP (ORDER BY frequency) FROM companyNumEmployees)) AS diff,
              row_number() OVER (PARTITION BY personWorkAtCompanyDays_window.companyId ORDER BY md5(personWorkAtCompanyDays_window.personId)) AS rnum
          FROM companyNumEmployees
          JOIN personWorkAtCompanyDays_window
          ON personWorkAtCompanyDays_window.companyId = companyNumEmployees.id
          ORDER BY diff, md5(personWorkAtCompanyDays_window.personId), md5(personWorkAtCompanyDays_window.companyId)
      )
      WHERE rnum < 5
      LIMIT 200
    ) comp
-- ensure that there is a two-hop path
-- hop 1
JOIN same_university_knows k1
  ON k1.person1Id = comp.person1Id
-- hop 2
JOIN same_university_knows k2
  ON k2.person1Id = k1.person2Id
 AND k2.person2Id != k1.person1Id
-- 'person2' does not work at the company
WHERE NOT EXISTS (SELECT 1
        FROM personWorkAtCompanyDays_window work
        WHERE work.companyId = comp.companyId
          AND work.personId = k2.person2Id
        )
ORDER BY md5(k2.person2Id), md5(comp.companyId)
LIMIT 400
