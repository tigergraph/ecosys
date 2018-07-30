# cancel current loading job
# replace para for your-neptune-endpoint
# $1 id the argument for loadId

curl -X DELETE "http://your-neptune-endpoint:8182/loader/$1"
