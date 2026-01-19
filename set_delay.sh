#!/bin/bash

source bash_variables.sh

# set delay of 1000 ms on node1
curl -X POST http://${NODE_IP[0]}:${NODE_PORT[0]}/delay/1000