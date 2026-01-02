package cz.cvut.fel.dsv.controller;

import cz.cvut.fel.dsv.node.NodeMessageService;
import io.javalin.Javalin;

import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import static io.javalin.apibuilder.ApiBuilder.*;

@Slf4j
public class NodeController {

    private NodeMessageService messageService;

    public NodeController(int port, String ip, NodeMessageService service) {
        setupServer(port, ip);
        messageService = service;
    }

    public void setupServer(int port, String ip) {
        Javalin app = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
                get("/join", this::join);
                get("/leave", this::leave);
                get("/kill", this::kill);
                get("/revive", this::revive);
                get("/status", this::getStatus);
                post("/delay/{milliseconds}", this::setDelay);
                post("/start/{workAmount}", this::startWork);
            });
        }).start(ip ,port);
    }

    private void setDelay(Context ctx) {
        log.info("Set delay request received with value {}", ctx.pathParam("milliseconds"));
        ctx.json("Delay set to: " + ctx.pathParam("milliseconds"));
    }

    private void startWork(Context ctx) {
        ctx.json("Starting work for amount: " + ctx.pathParam("workAmount"));
    }

    private void join(Context ctx) {
        log.info("Join request received");
        try {
            messageService.sendJoinMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void leave(Context ctx) {
        log.info("Leave request received");
        try {
            messageService.sendLeaveMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void kill(Context ctx) {
        log.info("Kill request received");
    }

    private void revive(Context ctx) {
        log.info("Revive request received");
    }

    private void getStatus(Context ctx) {
        log.info("Get status request received");
    }
}
