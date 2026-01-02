package cz.cvut.fel.dsv.message;

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
}
