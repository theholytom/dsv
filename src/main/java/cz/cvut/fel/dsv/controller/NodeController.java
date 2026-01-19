package cz.cvut.fel.dsv.controller;

import cz.cvut.fel.dsv.node.Node;
import cz.cvut.fel.dsv.node.NodeMessageService;
import io.javalin.Javalin;

import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import static io.javalin.apibuilder.ApiBuilder.*;

@Slf4j
public class NodeController {

    private final NodeMessageService messageService;
    private final Node node;

    public NodeController(int port, String ip, NodeMessageService service, Node node) {
        setupServer(port, ip);
        messageService = service;
        this.node = node;
    }

    public void setupServer(int port, String ip) {
        Javalin app = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
                get("/join", this::join);
                get("/leave", this::leave);
                get("/kill", this::kill);
                get("/status", this::getStatus);
                post("/delay/{milliseconds}", this::setDelay);
                post("/start/{workAmount}", this::startWork);
            });
        }).start(ip ,port);
    }

    private void setDelay(Context ctx) {
        log.info("Set delay request received with value {}", ctx.pathParam("milliseconds"));
        int delay = 0;
        try {
            delay = Integer.parseInt(ctx.pathParam("milliseconds"));
        } catch (Exception e) {
            log.error("Incorrect parameter type: {} is not int", ctx.pathParam("milliseconds"));
            return;
        }
        messageService.setDelay(delay);
        ctx.json("Delay set to: " + ctx.pathParam("milliseconds"));
    }

    private void startWork(Context ctx) {
        log.info("Start work request received with work amount of {} units", ctx.pathParam("workAmount"));
        int workAmount = 0;
        try {
            workAmount = Integer.parseInt(ctx.pathParam("workAmount"));
        } catch (Exception e) {
            log.error("Incorrect parameter type: {} is not int", ctx.pathParam("workAmount"));
            return;
        }
        node.distributeWork(workAmount);

        ctx.json("Starting work for amount: " + ctx.pathParam("workAmount"));
    }

    private void join(Context ctx) {
        log.info("Join request received");
        try {
            messageService.sendJoinMessage();
            ctx.result("processing JOIN request");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void leave(Context ctx) {
        log.info("Leave request received");
        try {
            messageService.sendLeaveMessage();
            node.stopHealthChecker();
            ctx.result("processing LEAVE request");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void kill(Context ctx) {
        log.info("Kill request received");
        node.stopHealthChecker();
        ctx.result("processing KILL request");
    }

    private void getStatus(Context ctx) {
        log.info("Get status request received");
        try {
            ctx.json(node.reportStatus());
        } catch (Exception e) {
            log.error("Exception caught in getStatus!", e);
        }

    }
}
