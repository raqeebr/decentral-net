package org.uom.contracts.handlers;

import org.uom.domain.Node;
import org.uom.model.message.Message;

public interface MessageHandler {
    void handle(Node node, Message message);
}
