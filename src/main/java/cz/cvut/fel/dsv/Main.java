package cz.cvut.fel.dsv;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import cz.cvut.fel.dsv.config.ConfigLoader;
import cz.cvut.fel.dsv.node.Node;
import cz.cvut.fel.dsv.node.NodeDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    private static final String NODE_EXCHANGE = "nodes.topic";

    public static void main(String[] args) {

        String nodeId = parseNodeId(args);

        log.info("Starting node {}", nodeId);

        try {
            NodeDetails config = ConfigLoader.load(nodeId);
            Channel channel = createRabbitMQConnection();


            Node node = new Node(config, channel, NODE_EXCHANGE);
            node.start();

            Thread.sleep(2000);

            keepAlive();

        } catch (Exception e) {
            log.error("Node startup failed", e);
            System.exit(1);
        }
    }

    /**
     * Parses --nodeId=<id> from command-line arguments.
     */
    private static String parseNodeId(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--nodeId=")) {
                return arg.substring("--nodeId=".length());
            }
        }

        log.error("Not able to parse argument");
        System.exit(1);
        return null; // unreachable
    }

    /**
     * Prevents JVM from exiting.
     */
    private static void keepAlive() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {
        }
    }

    private static Channel createRabbitMQConnection() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            Connection connection = factory.newConnection();
            return connection.createChannel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}