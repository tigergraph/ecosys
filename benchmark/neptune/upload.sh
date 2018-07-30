# this script use to upload data from client EC2 to S3
# $1 is the argument of path/to/local/file
# $2 is the argument of entire path of a file, including the file name
# "bucket-name" (need to modify): is your bucket-name in S3

aws s3 cp $1 s3://bucket-name/$2
