package cz.cvut.fel.dsv.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Topology {
    private ArrayList<String> order;
    private int version;
}
