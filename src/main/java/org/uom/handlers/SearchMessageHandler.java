package org.uom.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.contracts.handlers.MessageHandler;
import org.uom.domain.File;
import org.uom.domain.Node;
import org.uom.model.message.Message;
import org.uom.model.message.SearchMessage;
import org.uom.utils.Constants.CommandResponses;

import java.util.List;

public class SearchMessageHandler implements MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchMessageHandler.class);

    @Override
    public void handle(Node node, Message message) {
        SearchMessage searchMessage = new SearchMessage(message);
        List<File> matchingFiles;

        if (searchMessage.initiatorIp.equals(node.getIpAddress()) && searchMessage.initiatorPort == node.getPort()) {
            LOGGER.info("[Search Loopback ({})] Search loopback for '{}'", node.getPort(), searchMessage.query);
        } else if (searchMessage.hops <= 0) {
            LOGGER.info("[Max Hop ({})] Reached max hop count for '{}'", node.getPort(), searchMessage.query);
            this.sendFileNotFoundMessage(node, searchMessage);
        } else if ((matchingFiles = node.getMatchingFiles(searchMessage.query)).size() > 0) {
            LOGGER.info("[Search File Found ({})] File found for query '{}'", node.getPort(), searchMessage.query);
            node.sendFileFound(searchMessage, matchingFiles);
        } else {
            node.forwardSearch(
                searchMessage.query,
                searchMessage.initiatorIp,
                searchMessage.initiatorPort,
                searchMessage.hops);
        }
    }

    private void sendFileNotFoundMessage(Node node, SearchMessage message) {
        String searchFailMsg = String.format(CommandResponses.SEROK, 0, node.getIpAddress(), node.getPort(), 0, message.query);
        node.sendToNeighbour(message.initiatorIp, message.initiatorPort, searchFailMsg);
    }
}
