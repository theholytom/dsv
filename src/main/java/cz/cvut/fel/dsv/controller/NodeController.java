package cz.cvut.fel.dsv.controller;

import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.get;

public class NodeController {

    public NodeController() {
        Javalin app = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
            });
        }).start("::",9000);
    }
}
