package org.uom.handlers;

import org.uom.contracts.handlers.MessageHandler;
import org.uom.domain.Node;
import org.uom.model.message.LeaveMessage;
import org.uom.model.message.Message;

public class LeaveMessageHandler implements MessageHandler {
    @Override
    public void handle(Node node, Message message) {
        LeaveMessage leaveMessage = new LeaveMessage(message);
        node.removeNeighbour(leaveMessage.ipAddress, leaveMessage.port);
    }
}
