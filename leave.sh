#!/bin/bash

source bash_variables.sh

# make node2 leave the topology
curl http://${NODE_IP[1]}:${NODE_PORT[1]}/leave