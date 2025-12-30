package cz.cvut.fel.dsv;

import cz.cvut.fel.dsv.controller.NodeController;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        new NodeController(9000);
    }
}