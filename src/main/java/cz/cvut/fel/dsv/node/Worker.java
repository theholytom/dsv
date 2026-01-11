package cz.cvut.fel.dsv.node;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class Worker implements Runnable {

    private final Node node;
    private final NodeMessageService messageService;
    private final static int INT_BOUND = 20;
    private final Random random = new Random();

    public Worker(Node node, NodeMessageService messageService) {
        this.node = node;
        this.messageService = messageService;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            synchronized (node) {
                while (node.getWork() <= 0) {
                    try {
                        log.info("Thread {} waiting", Thread.currentThread());
                        node.wait(); // safe: holding node's monitor
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            work();
        }
    }

    private void work() {
        node.updateWork(work -> work - 1);
        int workTime = random.nextInt(INT_BOUND) * 1000;
        try {
            Thread.sleep(workTime);
        } catch (InterruptedException e) {
            log.error("Error in work method inside worker");
            throw new RuntimeException(e);
        }
    }

    private void completed() {

    }
}
