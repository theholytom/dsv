package cz.cvut.fel.dsv;

import cz.cvut.fel.dsv.config.ConfigLoader;
import cz.cvut.fel.dsv.controller.NodeController;
import cz.cvut.fel.dsv.node.Node;
import cz.cvut.fel.dsv.node.NodeDetails;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("App started");

        String nodeId = parseNodeId(args);

        System.out.println("Starting node " + nodeId);

        try {
            NodeDetails config = ConfigLoader.load(nodeId);
            Node node = new Node(config);

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
}