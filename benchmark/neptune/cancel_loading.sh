# this script used to delete the loader job with the job id
# "your-neptune-endpoint" (need to modify): obtain from Neptune instance page and it is in Details section, marked as "Endpoint"
# $1 is the argument of job id which returned by Neptune loader

curl -X DELETE "http:/your-neptune-endpoint:8182/loader/$1"
