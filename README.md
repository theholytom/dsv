# Dijkstra-Feijen-Van Gasteren Termination Detection Algorithm

This project consists of code for one node in a distributed network that 
implements the Dijkstra-Feijen-Van Gasteren termination detection algorithm. 
The nodes in the network dynamically shape a ring topology sorted based on 
the ids. The communication between the nodes is provided via a RabbitMQ cluster 
that each node is connected to. Additionally, each node can be controlled via a 
REST interface.

## Contents

- Node
  - Javalin Controller
  - Messaging Service
  - Healthcheck Service
  - Worker Service
- Topology
  - JOIN
  - LEAVE
  - KILL
  - TOPOLOGY_UPDATE
- Termination Detection Algorithm
- Setup Instructions


## Node

A node is implemented as Java application. Each node has a Javalin controller to 
process HTTP requests from user that controls the node, own messaging service that 
takes care of the communication between the nodes, own healthcheck service that 
check whether a neighbouring node is alive, own worker service that takes care of the 
assigned work from the user or other nodes.

### Javalin Controller

Here is the list of endpoints each node is exposing for user interaction.

#### GET Requests
```
address:port/                   # hello world!
address:port/join               # JOIN topology request
address:port/leave              # LEAVE topology request
address:port/kill               # KILL node request
address:port/status             # Node Status request
```

#### POST Request
```
address:port/work/{workAmount}  # WORK_ASSIGNMENT to node
```

### Messaging Service

Messaging service is created on node startup and is given the connection to RabbitMQ cluster. 
This service is responsible for sending and handling messages to and from the RabbitMQ queues 
that are used for node-to-node communication.

### Healthcheck Service

Healthcheck service is responsible for both checking if a neighbouring node is alive and also 
reporting to the other neighbouring node that this node is alive. This service is only active
when the topology consist of at least two nodes. The nodes are checking the health of each other. 
In case more than two nodes are part of the network, one neighbour of the node is being checked, 
the other one is being reported to.

The application logic is set up that one node is handling messages of type HEALTHCHECK and check 
if the sender is the correct node according to the current topology and checks if interval between 
healthcheck messages is not greater than the timeout. The interval a node is sending the messages is 
set to 3 seconds while the timeout is set to 4 seconds. The healthcheck service runs on a separate thread.

### Worker Service

Worker service is responsible for performing the computing that is being distributed across the network 
(if all conditions for work distribution are met). This project implements a simple distributed waiting 
where each node can be assigned a finite amount of work units and this amount can then be distributed.
On work assignment, node updates the amount of work that it has before and notifies the thread the worker runs on.
Worker sets node status to active when performing the work, when he is finished, sets the node status to passive 
and interrupts the thread. 

The speed at which the node is able to process one unit of work is based on a pseudo-randomized function that 
chooses a number from 1 to 20 representing the number of seconds that a node has to sleep for completing the unit. 
This is done once on a worker startup when created during node startup and simulates different processing speed of 
different machines and also enables some faster nodes to request work from its neighbours.

## Topology

When nodes are joining the network, it forms a ring topology sorted based on nodeId. The ring topology is achieved as 
a virtual/overlay topology, because nodes are not connected with each other. On node startup, the node connects to a 
RabbitMQ cluster, and it is not part of any network even though it can receive messages. The network is able to 
dynamically repair and update the topology to keep the ring topology.

### JOIN

In order for node to join the network, user has to call join endpoint - node then sends a broadcast 
join message. In case some node(s) already started forming the topology, he/they add the requesting node 
and sort the order accordingly. If no node is present in the network, node adds himself after some time (7s).

### LEAVE

In order for node to leave the network, user has to call leave endpoint - node then sends a broadcast leave message.
In case some other node(s) are part of the network, he/they remove the requesting node from the topology 
and sort it accordingly.

### KILL

In order for node simulate the node failure, user has to call kill endpoint - node then stops sending 
healthcheck messages. The node that is responsible for checking the health of this node then detects 
a failure and broadcasts a message requesting the removal of this node.

### TOPOLOGY_UPDATE

TOPOLOGY_UPDATE is a special type of message that is sent when a node is being added/removed. It is 
a broadcast message that consists of new topology object. Each node handles this message by updating the 
topology they have stored, updating the references of their neighbours and restarting the healthcheck 
service with correct addresses.

## Termination Detection Algorithm

The Dijkstra-Feijen-Van Gasteren termination detection algorithm was used in this project. 
It is implemented as part of the distributed computation algorithm. Once the worker completed 
all the assigned work, he than checks if he can pass the token, otherwise he is expecting token 
or additional work assignment.

The token handling is protected against the loss of the token in case of node failures and joins 
during the computation. No matter who has the token of who does not, on the arrival of TOPOLOGY_UPDATE 
message, the token is regenerated on a leader node (lowest id) and all other nodes should burn the 
token in case they have it. Therefore, the termination detection algorithm is restarted each time 
the topology is updated.

The risk of the token being faster than work assignment and causing the unstable behaviour of the termination 
detection algorithm is mitigated by the application logic set in a way where nodes request work only from neighbours.

## Setup Instructions

1. **RabbitMQ Cluster Setup:** First run the VMs and see what IPs are assigned to them. Then run the 
following scripts on both VMs (you will need to copy the Erlang cookie from rabbit1 to rabbit2)

    ```bash
    # On first server:
    ./rabbit1_setup.sh <rabbit1_ip> <rabbit2_ip>
    
    # On second server:
    ./rabbit2_setup.sh <rabbit1_ip> <rabbit2_ip>
    ```

2. **Configuration:** Enter IPs from rabbit VMs to ```src/main/resources/rabbitmq.json:```

    ```json
    {
      "username": "myuser",
      "password": "mypassword",
      "addresses":[
      { 
        "host": "<rabbit1_ip>", 
        "port": 5672
      },
      {
        "host": "<rabbit2_ip>",
        "port": 567
      }]
    }
    ```
   
3. **Build and run with required parameter**

    ```bash
    # Build the project
    mvn clean package

    # Run the node
    java -jar target/dsv-1.0-SNAPSHOT.jar --nodeId=nodeX # X -> 1 - 5
   ```