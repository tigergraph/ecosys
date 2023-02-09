-- materialize table
CREATE OR REPLACE TABLE same_university_knows AS
    SELECT p1joined.person1id AS person1Id, p1joined.person2id AS person2Id
    FROM (
      SELECT k.person1Id AS person1Id, k.person2Id AS person2Id, p1.universityId AS universityId
      FROM personKnowsPersonDays_window k
      JOIN personStudyAtUniversityDays_window p1
        ON p1.personId = k.person1Id
    ) p1joined
    JOIN personStudyAtUniversityDays_window p2
      ON p2.personId = p1joined.person2Id
     AND p2.universityId = p1joined.universityId
