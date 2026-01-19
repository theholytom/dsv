#!/bin/bash

source bash_variables.sh

SLEEP_TIME=2

curl -X POST http://${NODE_IP[0]}:${NODE_PORT[0]}/start/10
sleep ${SLEEP_TIME}
curl -X POST http://${NODE_IP[1]}:${NODE_PORT[1]}/start/10
sleep ${SLEEP_TIME}
curl -X POST http://${NODE_IP[2]}:${NODE_PORT[2]}/start/10
