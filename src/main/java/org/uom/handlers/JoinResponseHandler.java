package org.uom.handlers;

import org.uom.contracts.handlers.MessageHandler;
import org.uom.domain.Node;
import org.uom.model.message.JoinResponse;
import org.uom.model.message.Message;

public class JoinResponseHandler implements MessageHandler {
    @Override
    public void handle(Node node, Message message) {
        JoinResponse joinResponse = new JoinResponse(message);
        node.addNeighbour(joinResponse.originIp, joinResponse.originPort);
    }
}
