package cz.cvut.fel.dsv.node;

import cz.cvut.fel.dsv.controller.NodeController;

public class Node {
    private final NodeDetails details;

    public Node(NodeDetails details) {
        this.details = details;

        new NodeController(details.getPort(), details.getHost());
    }
}
