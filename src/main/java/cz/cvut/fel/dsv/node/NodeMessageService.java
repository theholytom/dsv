package cz.cvut.fel.dsv.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import cz.cvut.fel.dsv.message.Message;
import cz.cvut.fel.dsv.message.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NodeMessageService {

    private final Channel channel;
    private final String exchangeName;
    private final ObjectMapper objectMapper;
    private final Node node;
    private final String nodeId;

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
            System.out.println(" [x] Sent '" + message + "'");
        } catch (Exception e) {
            log.error("Error sending message of type: {}, to: {}. Error message: {}", message.getType(), targetId, e.getMessage(), e);
        }
    }

    public void handleMessage(String routingKey, byte[] body, AMQP.BasicProperties properties) {
        try {
            Message message = objectMapper.readValue(new String(body), Message.class);

            log.info("Node received message: type={}, from={}",
                    message.getType(), message.getSenderId());

            switch (message.getType()) {
                case HEALTHCHECK:
                    log.info("Processing HEALTHCHECK from node {}", message.getSenderId());
                    System.out.println("Content: " + message.getContent());
                    break;
                case JOIN:
                    log.info("Processing JOIN from node {}", message.getSenderId());
                    handleJoinMessage(message.getSenderId());
                    break;
                case TOPOLOGY_UPDATE:
                    log.info("Processing TOPOLOGY_UPDATE from node {}", message.getSenderId());
                    handleTopologyUpdate(message);
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

    private void sendTopologyUpdateMessage(Topology newTop) {
        sendMessage(new Message(nodeId, "all", MessageType.TOPOLOGY_UPDATE, newTop));
    }

    // ----------------------- Metody pro handling konkrétních zpráv -----------------------

    private void handleJoinMessage(String senderId) {

        ArrayList<String> presentOrder = node.getTopology().getOrder();

        // node cannot process own join request
        if (senderId.equals(nodeId)) {
            log.info("Node {} cannot process own join request", senderId);
            return;
        }

        // if join request sender is already a part of the network -> ignore
        if (presentOrder.contains(senderId)) {
            log.info("Node {} already present in topology", senderId);
            return;
        }

        // fill own id on first join request
        if (presentOrder.isEmpty()) {
            presentOrder.add(nodeId);
        }

        // if node is leader -> include requester in the network and broadcast topology update
        if  (presentOrder.get(0).equals(nodeId)) {

            presentOrder.add(senderId);
            presentOrder.sort(String.CASE_INSENSITIVE_ORDER);
            int version = node.getTopology().getVersion();
            version++;

            Topology newTop = new Topology(new ArrayList<>(presentOrder), version);

            log.info("NODE {}: Node {} was added to topology, sending new topology: {}", nodeId, senderId, newTop);
            sendTopologyUpdateMessage(newTop);

        }
    }

    private void handleTopologyUpdate(Message message) {
        log.info("New topology update to be set: {}", message.getTopology());
        node.setTopology(message.getTopology());
    }
}
