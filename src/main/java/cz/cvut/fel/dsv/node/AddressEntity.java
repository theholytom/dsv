package cz.cvut.fel.dsv.node;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AddressEntity {

    private String host;
    private int port;
}
