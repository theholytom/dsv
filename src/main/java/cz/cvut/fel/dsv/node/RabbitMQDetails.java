package cz.cvut.fel.dsv.node;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@NoArgsConstructor
@Data
public class RabbitMQDetails {

    private String username;
    private String password;
    private ArrayList<AddressEntity> addresses;
}
