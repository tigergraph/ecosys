# this sript used to get the status of the load with the loadId from previous step
# "your-neptune-endpoint" (need to modify): obtain from Neptune instance page and it is in Details section, marked as "Endpoint"
# $1 is the argument for loadId, which is returned by Neptune loader

curl -G "http:/your-neptune-endpoint:8182/loader/$1"
