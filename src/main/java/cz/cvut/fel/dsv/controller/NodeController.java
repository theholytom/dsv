package cz.cvut.fel.dsv.controller;

import io.javalin.Javalin;

import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.*;

public class NodeController {

    public NodeController(int port, String ip) {
        setupServer(port, ip);
    }

    public void setupServer(int port, String ip) {
        Javalin app = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
                get("/join", ctx -> ctx.result("join"));
                get("/leave", ctx -> ctx.result("leave"));
                get("/kill", ctx -> ctx.result("kill"));
                get("/revive", ctx -> ctx.result("revive"));
                get("/status", ctx -> ctx.result("join"));
                post("/delay/{milliseconds}", this::setDelay);
                post("/start/{workAmount}", this::startWork);
            });
        }).start(ip ,port);
    }

    private void setDelay(Context ctx) {
        ctx.json("Delay set to: " + ctx.pathParam("milliseconds"));
    }

    private void startWork(Context ctx) {
        ctx.json("Starting work for amount: " + ctx.pathParam("workAmount"));
    }
}
