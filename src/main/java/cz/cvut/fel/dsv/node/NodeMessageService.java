package cz.cvut.fel.dsv.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import cz.cvut.fel.dsv.message.Message;
import cz.cvut.fel.dsv.message.MessageType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class NodeMessageService {

    private final Channel channel;
    private final String exchangeName;
    private final ObjectMapper objectMapper;
    private final Node node;
    private final String nodeId;
    @Getter
    private volatile long lastHealthcheck = 0;
    private boolean topUpdateReceived;
    private final int JOIN_RESPONSE_TIMEOUT = 7000;

    public NodeMessageService(Channel channel, String exchangeName, Node node) {
        this.channel = channel;
        this.exchangeName = exchangeName;
        this.objectMapper = new ObjectMapper();
        this.node = node;

        this.nodeId = node.getNodeId();
    }

    // ----------------------- Obecné metody pro sending a handling zpráv -----------------------

    private void sendMessage(Message message) {
        String targetId = message.getTargetId();
        try {
            String routingKey = "";
            if (targetId.equals("all")) {
                routingKey = "broadcast." + targetId;
            } else if (targetId.contains("node")) {
                routingKey = "private." + targetId;
            } else {
                log.error("Invalid routing key part ({}) when sending message from node {}", targetId, message.getSenderId());
            }

            channel.basicPublish(exchangeName, routingKey, null, objectMapper.writeValueAsBytes(message));
        } catch (Exception e) {
            log.error("Error sending message of type: {}, to: {}. Error message: {}", message.getType(), targetId, e.getMessage(), e);
        }
    }

    public void handleMessage(String routingKey, byte[] body, AMQP.BasicProperties properties) {
        try {
            Message message = objectMapper.readValue(new String(body), Message.class);

            log.info("Node {} received message: type={}, from={}", nodeId,
                    message.getType(), message.getSenderId());

            switch (message.getType()) {
                case HEALTHCHECK:
//                    log.info("Processing HEALTHCHECK from node {}", message.getSenderId());
                    handleHealthcheckMessage(message.getSenderId());
                    break;
                case JOIN:
                    log.info("Processing JOIN request from node {}", message.getSenderId());
                    handleJoinMessage(message.getSenderId());
                    break;
                case LEAVE:
                    log.info("Processing LEAVE request from node {}", message.getSenderId());
                    handleLeaveMessage(message.getSenderId());
                    break;
                case TOPOLOGY_UPDATE:
                    log.info("Processing TOPOLOGY_UPDATE from node {}", message.getSenderId());
                    handleTopologyUpdate(message);
                    break;
                case WORK_ASSIGNMENT:
                    log.info("Processing WORK_ASSIGNMENT from node {}. Work amount: {}", message.getSenderId(), message.getContent());
                    break;
                default:
                    log.warn("Node received unhandled message type: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Error processing message in node: {}", e.getMessage(), e);
        }
    }

    // ----------------------- Metody pro sending konkrétních zpráv -----------------------

    public void sendJoinMessage() {
        sendMessage(new Message(nodeId, "all", MessageType.JOIN, ""));
    }

    public void sendLeaveMessage() {
        sendMessage(new Message(nodeId, "all", MessageType.LEAVE, ""));
    }

    private void sendTopologyUpdateMessage(Topology newTop, String content) {
        sendMessage(new Message(nodeId, "all", MessageType.TOPOLOGY_UPDATE, content, newTop));
    }

    public void sendHealthcheck(String targetId) {
        sendMessage(new Message(nodeId, targetId, MessageType.HEALTHCHECK, ""));
    }

    public void broadcastWorkAssignment(int workForEachNode) {
        sendMessage(new Message(nodeId, "all", MessageType.WORK_ASSIGNMENT, String.valueOf(workForEachNode)));
    }

    public void sendWorkAssignment(String targetId, int work) {
        sendMessage(new Message(nodeId, targetId, MessageType.WORK_ASSIGNMENT, String.valueOf(work)));
    }

    // ----------------------- Metody pro handling konkrétních zpráv -----------------------

    private void handleJoinMessage(String senderId) {

        ArrayList<String> presentOrder = node.getTopology().getOrder();
        topUpdateReceived = false;

        long time = System.currentTimeMillis();

        // if join request sender is already a part of the network -> ignore
        if (presentOrder.contains(senderId)) {
            log.info("Node {} already present in topology", senderId);
            return;
        }

        // if node is leader -> include requester in the network and broadcast topology update
        if  (!presentOrder.isEmpty() && presentOrder.get(0).equals(nodeId)) {
            addNodeToTopology(senderId);
            return;
        }

        if (senderId.equals(nodeId) && presentOrder.isEmpty()) {
            while (true) {
                if (System.currentTimeMillis() - time > JOIN_RESPONSE_TIMEOUT && !topUpdateReceived) {
                    // there is no node in the topology and no other node have added him -> node can add himself
                    addNodeToTopology(senderId);
                    return;
                }
            }
        }
        log.info("Node {} could not add node {} to the network", nodeId, senderId);
    }

    private void handleLeaveMessage(String senderId) {

        ArrayList<String> presentOrder = node.getTopology().getOrder();

        // if leave request sender is not a part of the network -> ignore
        if (!presentOrder.contains(senderId)) {
            log.info("Node {} is not part of topology", senderId);
            return;
        }

        // node is last in the topology -> node can remove himself
        if (senderId.equals(nodeId) && presentOrder.size() == 1) {
            removeNodeFromTopology(senderId);
            return;
        }

        // if the leader is leaving, the topology update must be done by new leader - second-lowest id
        int index = 0;
        if (senderId.equals(presentOrder.get(0))) index = 1;

        // if node is leader -> exclude requester from the network and broadcast topology update
        if  (presentOrder.get(index).equals(nodeId)) {

            removeNodeFromTopology(senderId);
        }
    }

    private void handleTopologyUpdate(Message message) {

        log.info("New topology update to be set: {}", message.getTopology());
        node.setTopology(message.getTopology());
        topUpdateReceived = true;
    }

    private void handleHealthcheckMessage(String senderId) {

        // healthcheck from incorrect node
        if (!senderId.equals(node.getNextNode())) {
            log.error("Healthcheck message received on node {}, from incorrect node ({})", nodeId, senderId);
        }

        lastHealthcheck = System.currentTimeMillis();
//        log.info("Healthcheck received on node {}, from node ({})", nodeId, senderId);
    }

    // ----------------------- helper metody -----------------------

    private Topology sortOrderAndIncrementVersion(ArrayList<String> list) {
        list.sort(String.CASE_INSENSITIVE_ORDER);
        int version = node.getTopology().getVersion();
        version++;

        return new Topology(new ArrayList<>(list), version);
    }

    public void removeNodeFromTopology(String toRemoveId) {

        ArrayList<String> tmp = node.getTopology().getOrder();

        tmp.remove(toRemoveId);

        Topology newTop = sortOrderAndIncrementVersion(tmp);

        log.info("NODE {}: Node {} was removed from topology, sending new topology: {}", nodeId, toRemoveId, newTop);
        sendTopologyUpdateMessage(newTop, "remove");
    }

    public void addNodeToTopology(String toAddId) {

        ArrayList<String> tmp = node.getTopology().getOrder();

        tmp.add(toAddId);

        Topology newTop = sortOrderAndIncrementVersion(tmp);

        log.info("NODE {}: Node {} was added to topology, sending new topology: {}", nodeId, toAddId, newTop);
        sendTopologyUpdateMessage(newTop, "add");
    }

    public void resetLastHealthcheck() {
        lastHealthcheck = 0;
    }
}
