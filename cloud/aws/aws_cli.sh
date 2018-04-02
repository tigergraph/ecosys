#!/bin/bash
######################################
## Helps to get ec2 info using awscli
##
## Requirement: aws command installed
##     ubuntu: sudo apt-get install awscli
##     mac:    sudo easy_install pip; pip3 install awscli --upgrade --user; sudo ln -s /Users/user/Library/Python/2.7/bin/aws /usr/local/bin/
##
## Step:
##     Fill the aws access key and secret in aws_config file
##     Modify the FILTERs and region as needed
##     Enjoy life
##
## Reference: 
##     http://blog.xi-group.com/2015/01/small-tip-how-to-use-aws-cli-filter-parameter/
##     https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-instances.html
##
######################################


## get aws access key and secret
source aws_config

## the filters, MODIFY
## most common filter is tag, but also can use others
FILTER1="Name=tag-key,Values=the_tag_key"
FILTER2="Name=tag-value,Values=the_tag_value"

## the region, modify if not virginia
## US East (N. Virginia)   -- us-east-1
## US East (Ohio)          -- us-east-2
## US West (N. California) -- us-west-1
## details here: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.RegionsAndAvailabilityZones.html
REGION="us-east-1"

## the configuration
OUTPUT="json"
CONFIG="--region $REGION --output $OUTPUT --filter $FILTER1 $FILTER2"
PREFIX=".Reservations | .[0] | .Instances"

function get_all_private_ip () {
  aws ec2 describe-instances $CONFIG | jq -r "$PREFIX | .[] | .PrivateIpAddress"
}

function get_all_public_dns () {
  aws ec2 describe-instances $CONFIG | jq -r "$PREFIX | .[] | .PublicDnsName"
}

function get_all_public_ip () {
  aws ec2 describe-instances $CONFIG | jq -r "$PREFIX | .[] | .PublicDnsName" | sed 's/ec2-\(.*\)-\(.*\)-\(.*\)-\(.*\).compute-1.amazonaws.com/\1.\2.\3.\4/'
}

