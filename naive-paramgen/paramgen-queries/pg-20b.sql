SELECT
    companyNumEmployees.name AS 'company:STRING',
    personNumFriends_sample.id AS 'person2Id:ID'
FROM
    companyNumEmployees,
    (SELECT id FROM personNumFriends ORDER BY md5(id) LIMIT 100) personNumFriends_sample
ORDER BY md5(3532569367::bigint*companyNumEmployees.id + 211::bigint*personNumFriends_sample.id)
LIMIT 400
