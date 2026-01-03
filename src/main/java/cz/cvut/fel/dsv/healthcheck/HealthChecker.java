package cz.cvut.fel.dsv.healthcheck;


import cz.cvut.fel.dsv.node.NodeMessageService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HealthChecker implements Runnable {

    private final String nodeToCheck;
    private final String nodeToReportTo;
    private final NodeMessageService messageService;
    private static final int INTERVAL = 3000;
    private static final int TIMEOUT = 4000;
    private boolean running = false;

    public HealthChecker(String nodeToReportTo, String nodeToCheck, NodeMessageService messageService) {
        this.nodeToReportTo = nodeToReportTo;
        this.nodeToCheck = nodeToCheck;
        this.messageService = messageService;
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                messageService.sendHealthcheck(nodeToReportTo);
                verifyNodeHealth();
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {
                break;
            }

        }
    }

    private void verifyNodeHealth() {
        long lastHealthcheck = messageService.getLastHealthcheck();

        if (lastHealthcheck == 0) return; // first check, healthcheck has not yet arrived -> ignore

        if (System.currentTimeMillis() - lastHealthcheck > TIMEOUT) {
            // node neaktivní -> send topology update message s novou topologií bez nodeToCheck
            log.error("HEALTH REPORT TIMEOUT");
            messageService.removeNodeFromTopology(nodeToCheck);
            stop();
        }
    }

    public void stop() {
        running = false;
    }
}
