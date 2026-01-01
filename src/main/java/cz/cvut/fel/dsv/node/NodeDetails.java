package cz.cvut.fel.dsv.node;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NodeDetails {

    private String nodeId;
    private String host;
    private int port;
}
