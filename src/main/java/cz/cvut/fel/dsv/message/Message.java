package cz.cvut.fel.dsv.message;

import cz.cvut.fel.dsv.node.Topology;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String senderId;
    private String targetId;
    private MessageType type;
    private String content;
    private Topology topology;

    public Message(String senderId, String targetId, MessageType type, String content) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.type = type;
        this.content = content;
        topology = null;
    }

    public Message(String senderId, String targetId, MessageType type, Topology topology) {
        this.senderId = senderId;
        this.targetId = targetId;
        this.type = type;
        this.content = "";
        this.topology = topology;
    }
}


