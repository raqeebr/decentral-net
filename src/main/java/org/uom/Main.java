package org.uom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.domain.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        List<Node> nodes = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[Shutdown] Shutting down nodes...");
            nodes.forEach(Node::stop);
        }));

        try {
            String bSrvIpAddress = InetAddress.getLocalHost().getHostAddress();
            for (int i = 1; i <= 3; i++) {
                nodes.add(new Node(bSrvIpAddress, 55555));
            }
        } catch (IOException e) {
            LOGGER.error("[Node Failed] {}", e.getMessage(), e);
        }
    }
}