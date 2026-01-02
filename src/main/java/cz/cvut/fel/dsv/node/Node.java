package cz.cvut.fel.dsv.node;

import com.rabbitmq.client.Channel;
import cz.cvut.fel.dsv.controller.NodeController;
import cz.cvut.fel.dsv.message.Message;
import cz.cvut.fel.dsv.message.MessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
public class Node {
    private final String exchangeName;
    private final NodeDetails details;
    private final Channel channel;
    private NodeMessageService messageService;
    @Getter
    private final String nodeId;
    @Getter
    @Setter
    private Topology topology;

    public Node(NodeDetails details, Channel channel, String exchangeName) {
        this.details = details;
        this.channel = channel;
        this.exchangeName = exchangeName;
        this.topology = new Topology(new ArrayList<>(), 0);

        this.nodeId = details.getNodeId();
    }

    public void start() {

        setupQueue();
        messageService = new NodeMessageService(channel, exchangeName, this);
        new NodeController(details.getPort(), details.getHost(), messageService);

//        // TESTING
//
//        messageService.sendMessage(new Message(nodeId, "all", MessageType.HEALTHCHECK, "Node " + nodeId + "žije!!!"));
    }

    private void setupQueue() {
        try {

            channel.exchangeDeclare(exchangeName, "topic", true);


            String queueName = nodeId + "-node-queue";

            channel.queueDeclare(queueName, false, true, true, null);

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
