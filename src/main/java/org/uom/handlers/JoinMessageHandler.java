package org.uom.handlers;

import org.uom.contracts.handlers.MessageHandler;
import org.uom.domain.Node;
import org.uom.model.message.JoinMessage;
import org.uom.model.message.Message;
import org.uom.utils.Constants.CommandResponses;

public class JoinMessageHandler implements MessageHandler {
    @Override
    public void handle(Node node, Message message) {
        JoinMessage joinMessage = new JoinMessage(message);
        node.addNeighbour(joinMessage.originIp, joinMessage.originPort);

        String joinResponseMsg = String.format(CommandResponses.JOINOK, 0);
        node.sendToNeighbour(message.originIp, message.originPort, joinResponseMsg);
    }
}
