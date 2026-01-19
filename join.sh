#!/bin/bash

source bash_variables.sh

SLEEP_TIME=2
FIRST_JOIN_SLEEP_TIME=10

curl http://${NODE_IP[0]}:${NODE_PORT[0]}/join
sleep ${FIRST_JOIN_SLEEP_TIME}
curl http://${NODE_IP[1]}:${NODE_PORT[1]}/join
sleep ${SLEEP_TIME}
curl http://${NODE_IP[2]}:${NODE_PORT[2]}/join
sleep ${SLEEP_TIME}
curl http://${NODE_IP[3]}:${NODE_PORT[3]}/join
sleep ${SLEEP_TIME}
curl http://${NODE_IP[4]}:${NODE_PORT[4]}/join
sleep ${SLEEP_TIME}
