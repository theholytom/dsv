package cz.cvut.fel.dsv.node;

import com.rabbitmq.client.Channel;
import cz.cvut.fel.dsv.controller.NodeController;
import cz.cvut.fel.dsv.healthcheck.HealthChecker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

@Slf4j
public class Node {
    private final String exchangeName;
    private final NodeDetails details;
    private final Channel channel;
    private NodeMessageService messageService;
    private HealthChecker healthChecker;
    private Thread healthCheckerThread;
    @Getter
    private Worker worker;
    private Thread workerThread;
    @Getter
    private volatile int work = 0;
    @Getter
    private final String nodeId;
    @Getter
    private Topology topology;
    @Getter
    private String prevNode;
    @Getter
    private String nextNode;
    private final static int MIN_WORK_AMOUNT = 6;
    private final static int MAX_WORK_TO_ASSIGN = 3;

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
        new NodeController(details.getPort(), details.getHost(), messageService, this);
        setupWorker();
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

    private void setupWorker() {
        this.worker = new Worker(this, messageService);
        workerThread = new Thread(worker);
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("Worker for node {} started", nodeId);
    }

    public void setTopology(Topology newTop) {
        this.topology = newTop;

        int size = topology.getOrder().size();

        if (size == 1) {
            // node is alone in the network
            // cancel node checker and return
            stopHealthChecker();
            return;
        }

        int index = this.topology.getOrder().indexOf(nodeId);
        if (index == -1) { // nodeId not present in order list
            nextNode = "";
            prevNode = "";
            return;
        }

        int nextIndex = (index + 1) % size;
        int prevIndex = (index - 1 + size) % size;

        String newNextNode = this.topology.getOrder().get(nextIndex);
        String newPrevNode = this.topology.getOrder().get(prevIndex);

        if (!newNextNode.equals(nextNode)) {
            nextNode = newNextNode;
        }

        if (!newPrevNode.equals(prevNode)) {
            prevNode = newPrevNode;
        }

        startHealthChecker();
    }

    private void startHealthChecker() {
        stopHealthChecker();
        messageService.resetLastHealthcheck();

        healthChecker = new HealthChecker(
                prevNode,
                nextNode,
                messageService
        );

        healthCheckerThread = new Thread(healthChecker);
        healthCheckerThread.setDaemon(true);
        healthCheckerThread.start();

        log.info("Health checker started for node: {}", nodeId);
    }

    public void stopHealthChecker() {
        if (healthChecker != null) {
            log.info("Stopping health checker");
            healthChecker.stop();
            healthChecker = null;
        }
        if (healthCheckerThread != null) {
            healthCheckerThread.interrupt();
            healthCheckerThread = null;
        }
    }

    public synchronized void distributeWork(int workAmount) {
        // vypočítat kolik práce rozdělit a odeslat
        int workForEachNode = calculateWork(workAmount);
        if (workForEachNode != workAmount) {
            messageService.broadcastWorkAssignment(workForEachNode);
            updateWork(x -> x + (workAmount % topology.getOrder().size()));
            this.notify();
            return;
        }
        updateWork(x -> x + workAmount);
        this.notify();
    }

    private int calculateWork(int workAmount) {
        int topologySize = topology.getOrder().size();

        if (topologySize <= 1 || !topology.getOrder().contains(nodeId)) {
            log.info("Node is alone -> The work was not divided!!!");
            return workAmount;
        }

        int remainder = workAmount % topologySize;
        return (workAmount - remainder) / topologySize;
    }

    public synchronized void updateWork(Function<Integer, Integer> operation ) {
        int old = work;
        work = operation.apply(old);
        log.info("Work updated from {} to {}", old, work);
    }

    public int assignWork() {
        if (work > MIN_WORK_AMOUNT) {
            updateWork(x -> x - MAX_WORK_TO_ASSIGN);
            return MAX_WORK_TO_ASSIGN;
        }
        return 0;
    }
}
