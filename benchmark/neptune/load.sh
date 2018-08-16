# this script used to load data from S3 to Neptune
# "your-neptune-endpoint" (need to modify): check from Neptune instance page and it is in Details section, marked as "Endpoint"
# "bucket-name" (need to modify): is your bucket-name in S3
# "object-key-name" (need to modify): is the entire path of a file in S3, including the file name.
# "account-id" and "role-name" (need to modify): in aws console, check "My Security Credentials" from drop bar, check "Roles"
#     from left side, then check correct role name. "iamRoleArn" list in Summary page 

curl -X POST -H 'Content-Type: application/json' http://your-neptune-endpoint:8182/loader -d ' {
"source" : "s3://bucket-name/object-key-name",
"format" : "csv",
"iamRoleArn" : "arn:aws:iam::account-id:role/role-name", "region" : "us-east-1",
      "failOnError" : "FALSE"
    }'
