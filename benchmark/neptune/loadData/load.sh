# load data from S3 to Neptune, data files have been stored in s3 already
# replace parameter for "your-neptune-endpoint","source", "iamRoleArn"

curl -X POST -H 'Content-Type: application/json' http://your-neptune-endpoint:8182/loader -d ' {
"source" : "s3://bucket-name/object-key-name",
"format" : "csv",
"iamRoleArn" : "arn:aws:iam::account-id:role/role-name", "region" : "us-east-1",
      "failOnError" : "FALSE"
    }'
