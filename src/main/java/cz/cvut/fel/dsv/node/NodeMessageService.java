package cz.cvut.fel.dsv.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import cz.cvut.fel.dsv.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeMessageService {

    private final Channel channel;
    private final String exchangeName;
    private final ObjectMapper objectMapper;

    public NodeMessageService(Channel channel, String exchangeName) {
        this.channel = channel;
        this.exchangeName = exchangeName;
        this.objectMapper = new ObjectMapper();
    }

    public void sendMessage(Message message) {
        try {
            String routingKey = "";
            if (message.getTargetId().equals("all")) {
                routingKey = "broadcast." + message.getTargetId();
            } else {
                log.error("Invalid routing key part ({}) when sending message from node {}", message.getTargetId(), message.getSenderId());
            }

            channel.basicPublish(exchangeName, routingKey, null, objectMapper.writeValueAsBytes(message));
            System.out.println(" [x] Sent '" + message + "'");
        } catch (Exception e) {
            throw new RuntimeException(e);
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
//                case CONNECTION_TEST:
//                    log.info("Processing CONNECTION_TEST from node {}", message.getSenderId());
//                    break;
//                case QUEUE_UPDATE:
//                    handleQueueUpdate(message);
//                    break;
                default:
                    log.warn("Node received unhandled message type: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Error processing message in node: {}", e.getMessage(), e);
        }
    }
}
