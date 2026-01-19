#!/bin/bash

source bash_variables.sh

# kill node3
curl http://${NODE_IP[2]}:${NODE_PORT[2]}/kill