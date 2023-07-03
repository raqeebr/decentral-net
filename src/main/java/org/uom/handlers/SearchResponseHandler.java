package org.uom.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.contracts.handlers.MessageHandler;
import org.uom.domain.Node;
import org.uom.model.message.Message;
import org.uom.model.message.SearchResponse;

public class SearchResponseHandler implements MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchResponseHandler.class);

    @Override
    public void handle(Node node, Message message) {
        SearchResponse response = new SearchResponse(message);

        boolean isNeighbour = node.getNeighbours()
            .stream()
            .anyMatch(n -> n.ipAddress().equals(response.originIp) && n.port() == response.originPort);

        if (!isNeighbour) {
            LOGGER.info("[Search Response ({})] Sending Join Request to ({})", node.getPort(), response.originPort);
            node.join(response.originIp, response.originPort);
        }

        if (response.fileCount > 0) {
            for (String fileName : response.fileNames) {
                node.cacheFileNameForNode(fileName, response.originIp, response.originPort);
            }
        } else {
            node.addVisitedNode(response.fileNames[0], response.originIp, response.originPort);
        }
    }
}
