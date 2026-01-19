package cz.cvut.fel.dsv.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NodeStatusResponse {
    private String nodeId;
    private NodeDetails details;
    private int work;
    private Topology topology;
    private String prevNode;
    private String nextNode;
    private boolean hasToken;
    private boolean isNodeWhite;
    private boolean isTokenWhite;
    private boolean isActive;
    private int delay;
}
