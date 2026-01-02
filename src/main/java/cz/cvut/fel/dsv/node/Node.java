package cz.cvut.fel.dsv.node;

import com.rabbitmq.client.Channel;
import cz.cvut.fel.dsv.controller.NodeController;
import cz.cvut.fel.dsv.message.Message;
import cz.cvut.fel.dsv.message.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Node {
    private final String exchangeName;
    private final NodeDetails details;
    private final Channel channel;
    private NodeMessageService messageService;
    private final String nodeId;

    public Node(NodeDetails details, Channel channel, String exchangeName) {
        this.details = details;
        this.channel = channel;
        this.exchangeName = exchangeName;

        this.nodeId = details.getNodeId();
    }

    public void start() {

        setupQueue();
        new NodeController(details.getPort(), details.getHost());
        messageService = new NodeMessageService(channel, exchangeName);

        // TESTING

        messageService.sendMessage(new Message(nodeId, "all", MessageType.HEALTHCHECK, "Node " + nodeId + "žije!!!"));
    }

    private void setupQueue() {
        try {

            channel.exchangeDeclare(exchangeName, "topic", true);


            String queueName = nodeId + "-node-queue";

            channel.queueDeclare(queueName, true, false, false, null);

            channel.queueBind(queueName, exchangeName, "*." + nodeId + ".#");
            channel.queueBind(queueName, exchangeName, "broadcast.#");

            // Nastavení consumera
            channel.basicConsume(queueName, true, (consumerTag, message) -> {
                try {
                    messageService.handleMessage(
                            message.getEnvelope().getRoutingKey(),
                            message.getBody(),
                            message.getProperties()
                    );
                } catch (Exception e) {
                    log.error("Error processing message in node {}: {}", nodeId, e.getMessage(), e);
                }
            }, consumerTag -> {
                log.warn("Consumer was cancelled for node {}: {}", nodeId, consumerTag);
            });

            log.info("Node {} initialized queue {} and consumer", nodeId, queueName);
        } catch (IOException e) {
            log.error("Failed to setup node queue for {}", nodeId, e);
            throw new RuntimeException(e);
        }
    }
}
