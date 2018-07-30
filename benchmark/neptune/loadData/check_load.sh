# replace parameter for your-neptune-endpoint
# $1 is the argument of job id which returned by Neptune loader

curl -G "http://your-neptune-endpoint:8182/loader/$1"
