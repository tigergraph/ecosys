# $1 is the argument of local data-file-name
# $2 is the argument of file name in S3
# replace your S3 path in entire-path-in-S3
aws s3 cp $1 s3://entire-path-in-S3/$2
