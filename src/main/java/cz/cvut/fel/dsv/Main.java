package cz.cvut.fel.dsv;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import cz.cvut.fel.dsv.config.ConfigLoader;
import cz.cvut.fel.dsv.node.Node;
import cz.cvut.fel.dsv.node.NodeDetails;
import cz.cvut.fel.dsv.node.RabbitMQDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Main {

    private static final String NODE_EXCHANGE = "nodes.topic";

    public static void main(String[] args) {

        String nodeId = parseNodeId(args);

        log.info("Starting node {}", nodeId);

        try {
            NodeDetails config = ConfigLoader.loadNodeDetails(nodeId);
            Channel channel = createRabbitMQConnection(ConfigLoader.loadRabbitDetails());


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

    private static Channel createRabbitMQConnection(RabbitMQDetails details) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(details.getUsername());
            factory.setPassword(details.getPassword());
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(10000);

            factory.setConnectionTimeout(5000);
            factory.setHandshakeTimeout(10000);

            List<Address> addresses = List.of(new Address(details.getAddresses().get(0).getHost(), details.getAddresses().get(0).getPort()), new Address(details.getAddresses().get(1).getHost(), details.getAddresses().get(1).getPort()));

            Connection connection = factory.newConnection(addresses);
            return connection.createChannel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}