package org.uom.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.communication.MessageDispatcher;
import org.uom.communication.MessageReceiver;
import org.uom.contracts.handlers.MessageHandler;
import org.uom.handlers.*;
import org.uom.model.message.Message;
import org.uom.model.message.SearchMessage;
import org.uom.model.message.SocketMessage;
import org.uom.utils.Constants;
import org.uom.utils.Constants.Commands;
import org.uom.utils.Constants.MessageTypes;
import org.uom.utils.Constants.Metadata;
import org.uom.utils.FileUtils;
import org.uom.utils.UsernameGenerator;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class Node {
    private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);
    private final String ipAddress;
    private final int port;
    private final String username;
    private final DatagramSocket socket;
    private final String bSrvIpAddress;
    private final int bSrvPort;
    private final BlockingQueue<SocketMessage> sendQueue;
    private final BlockingQueue<Message> receiveQueue;
    private final MessageDispatcher sender;
    private final MessageReceiver receiver;
    private final Map<String, MessageHandler> responseHandlerMap;
    private final List<Neighbour> neighbours;
    private final List<File> files;
    private final Map<String, List<Neighbour>> visitedNodes;
    private final Map<String, List<String>> queryHitNodesMap;

    public Node(String bSrvIpAddress, int bSrvPort) throws SocketException, UnknownHostException {
        try {
            this.ipAddress = InetAddress.getLocalHost().getHostAddress();
            this.socket = new DatagramSocket();
        } catch (SocketException | UnknownHostException e) {
            LOGGER.error("[Node initialize] Could not initialize Node");
            throw e;
        }

        this.port = this.socket.getLocalPort();
        this.username = this.generateUsername();

        this.bSrvIpAddress = bSrvIpAddress;
        this.bSrvPort = bSrvPort;

        this.sendQueue = new LinkedBlockingQueue<>();
        this.receiveQueue = new LinkedBlockingQueue<>();

        this.sender = new MessageDispatcher(this.sendQueue, this.socket);
        this.receiver = new MessageReceiver(this.receiveQueue, this.socket);

        this.responseHandlerMap = new HashMap<>();
        this.neighbours = new ArrayList<>();
        this.files = new ArrayList<>();
        this.visitedNodes = new HashMap<>();
        this.queryHitNodesMap = new HashMap<>();

        this.initializeResponseHandlers();
        this.initializeFiles();
        this.start();
    }

    public void start() {
        this.sender.start();
        this.receiver.start();

        this.register();
        this.processReceivedMessages();
    }

    public void join(String neighbourIpAddress, int neighbourPort) {
        String joinMessage = String.format(Commands.JOIN, this.ipAddress, this.port);
        this.sendToNeighbour(neighbourIpAddress, neighbourPort, joinMessage);
    }

    public void stop() {
        this.unregister();
        this.leave();

        this.sender.stopDispatching();
        this.receiver.stopReceiving();

        LOGGER.info("[Node Disconnect] Stopped node ({}) {}", this.port, this.username);
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    public List<Neighbour> getNeighbours() {
        return this.neighbours;
    }

    public void addNeighbour(String ipAddress, int port) {
        this.neighbours.add(new Neighbour(ipAddress, port));
        LOGGER.info("[Added Neighbour ({})] {}:{} Total: {}", this.port, ipAddress, port, this.neighbours.size());
    }

    public void removeNeighbour(String ipAddress, int port) {
        Neighbour neighbour = this.neighbours.stream()
            .filter(n -> n.ipAddress().equals(ipAddress) && n.port() == port)
            .findFirst()
            .orElse(null);

        if (neighbour == null) {
            LOGGER.warn("[Neighbour not found ({})] {}:{}", this.port, ipAddress, port);
            return;
        }

        this.neighbours.remove(neighbour);
        LOGGER.info("[Removed Neighbour ({})] {}:{}", this.port, ipAddress, port);
    }

    public void sendToNeighbour(String neighbourIp, int neighbourPort, String msg) {
        SocketMessage message = new SocketMessage(neighbourIp, neighbourPort, msg);
        try {
            this.sendQueue.put(message);
        } catch (InterruptedException e) {
            LOGGER.warn("[Enqueue failed ({})] To Neighbour({}:{}) server: {}", this.port, neighbourIp, neighbourPort, msg);
        }
    }

    public List<File> getMatchingFiles(String query) {
        return this.files.stream()
            .filter(f -> FileUtils.hasMatchingFileName(f.fileName(), query))
            .collect(Collectors.toList());
    }

    public void searchFile(String query) {
        List<File> files = this.getMatchingFiles(query);
        if (!files.isEmpty()) {
            LOGGER.info("[File Exists ({})] Query: '{}'", this.port, query);
            return;
        }

        this.forwardSearch(query, this.ipAddress, this.port, Metadata.MAX_HOPS + 1);
    }

    public void forwardSearch(String query, String initiatorIp, int initiatorPort, int currentHops) {
        List<Neighbour> nodesToForward = this.getNodesWithFile(query)
            .stream()
            .toList();

        if (!nodesToForward.isEmpty()) {
            LOGGER.info("[Search Forward ({})] File found from prev search for '{}'", this.port, query);
            Neighbour neighbour = nodesToForward.get(0);
            this.sendSearchReqToNeighbour(query, initiatorIp, initiatorPort, --currentHops, neighbour);
            return;
        }

        LOGGER.info("[Search Forward ({})] No previous query hits for '{}'", this.port, query);

        if (initiatorPort == this.port && this.visitedNodes.containsKey(query)) {
            LOGGER.info("[Search Forward ({})] Searching from last visited node for '{}'", this.port, query);
            List<Neighbour> prevVisitedNodes = this.visitedNodes.get(query);
            Neighbour neighbour = prevVisitedNodes.get(prevVisitedNodes.size() - 1);
            this.sendSearchReqToNeighbour(query, initiatorIp, initiatorPort, --currentHops, neighbour);
            return;
        }

        nodesToForward = this.getNeighbours()
            .stream()
            .filter(not(n -> n.ipAddress().equals(initiatorIp) && n.port() == initiatorPort))
            .filter(n -> this.isNotVisitedNode(query, n))
            .toList();

        if (nodesToForward.isEmpty()) {
            LOGGER.info("[Search Forward ({})] No neighbours left to forward for '{}'", this.port, query);
            return;
        }

        for (Neighbour neighbour : nodesToForward) {
            if (currentHops == 0) {
                break;
            }

            this.sendSearchReqToNeighbour(query, initiatorIp, initiatorPort, --currentHops, neighbour);
        }
    }

    public void sendFileFound(SearchMessage message, List<File> matchingFiles) {
        String fileNames = matchingFiles.stream()
            .map(File::fileName)
            .collect(Collectors.joining(", "));

        String searchResponse = String.format(
            Constants.CommandResponses.SEROK,
            matchingFiles.size(),
            this.ipAddress,
            this.port,
            message.hops,
            fileNames);

        this.sendToNeighbour(message.initiatorIp, message.initiatorPort, searchResponse);
        if (!message.originIp.equals(message.initiatorIp) && message.originPort != message.initiatorPort) {
            this.sendToNeighbour(message.originIp, message.originPort, searchResponse);
        }
    }

    public boolean isNotVisitedNode(String query, Neighbour neighbour) {
        return !this.visitedNodes.containsKey(query) ||
            this.visitedNodes.get(query)
                .stream()
                .noneMatch(n -> n == neighbour);
    }

    public void addVisitedNode(String query, Neighbour neighbour) {
        this.visitedNodes.putIfAbsent(query, new ArrayList<>());
        if (this.isNotVisitedNode(query, neighbour)) {
            this.visitedNodes.get(query).add(neighbour);
        }
    }

    public void addVisitedNode(String query, String ipAddress, int port) {
        Neighbour neighbour = new Neighbour(ipAddress, port);
        this.addVisitedNode(query, neighbour);
    }

    public List<Neighbour> getNodesWithFile(String query) {
        String fileName = this.queryHitNodesMap.keySet()
            .stream()
            .filter(f -> FileUtils.hasMatchingFileName(f, query))
            .findFirst()
            .orElse(null);

        if (fileName != null) {
            return this.queryHitNodesMap.get(fileName)
                .stream()
                .map(addr -> {
                    String[] address = addr.split(":");
                    return new Neighbour(address[0], Integer.parseInt(address[1]));
                }).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public void cacheFileNameForNode(String fileName, String hostIp, int hostPort) {
        this.queryHitNodesMap.putIfAbsent(fileName, new ArrayList<>());
        String nodeAddress = "%s:%d".formatted(hostIp, hostPort);
        if (!this.queryHitNodesMap.get(fileName).contains(nodeAddress)) {
            this.queryHitNodesMap.get(fileName).add(nodeAddress);
        }
    }

    private void register() {
        String registerMessage = String.format(Commands.REG, this.ipAddress, this.port, this.username);
        this.sendToBootstrapSrv(registerMessage);
    }

    private void unregister() {
        String unregisterMessage = String.format(Commands.UNREG, this.ipAddress, this.port, this.username);
        this.sendToBootstrapSrv(unregisterMessage);
    }

    private void leave() {
        for (Neighbour neighbour : this.neighbours) {
            String leaveMessage = String.format(Commands.LEAVE, this.ipAddress, this.port);
            this.sendToNeighbour(neighbour.ipAddress(), neighbour.port(), leaveMessage);
        }
    }

    private void processReceivedMessages() {
        new Thread(() -> {
            while (true) {
                Message message = null;
                try {
                    message = this.receiveQueue.take();
                    if (Metadata.IGNORED_RESPONSES.contains(message.resType)) {
                        continue;
                    }
                    this.responseHandlerMap.get(message.resType).handle(this, message);
                } catch (InterruptedException e) {
                    LOGGER.error("[Dispatch Failed ({})] {}", this.port, e.getMessage(), e);
                } catch (NullPointerException e) {
                    if (message != null) {
                        LOGGER.error("[Response Handler not found ({})] {}", this.port, message.resType);
                    }
                }
            }
        }).start();
    }

    private String generateUsername() {
        String username = UsernameGenerator.Generate();
        return String.format("%s-%s", Metadata.NODE_PREFIX, username);
    }

    private void initializeResponseHandlers() {
        this.responseHandlerMap.put(MessageTypes.REGOK, new RegisterResponseHandler());
        this.responseHandlerMap.put(MessageTypes.JOIN, new JoinMessageHandler());
        this.responseHandlerMap.put(MessageTypes.JOINOK, new JoinResponseHandler());
        this.responseHandlerMap.put(MessageTypes.LEAVE, new LeaveMessageHandler());
        this.responseHandlerMap.put(MessageTypes.SEARCH, new SearchMessageHandler());
        this.responseHandlerMap.put(MessageTypes.SEROK, new SearchResponseHandler());
    }

    private void sendToBootstrapSrv(String msg) {
        SocketMessage message = new SocketMessage(this.bSrvIpAddress, this.bSrvPort, msg);
        try {
            this.sendQueue.put(message);
        } catch (InterruptedException e) {
            LOGGER.warn("[Enqueue failed ({})] To bootstrap server: {}", this.port, msg);
        }
    }

    private void initializeFiles() {
        List<String> fileNames = new ArrayList<>(Metadata.FILE_NAMES);
        Collections.shuffle(fileNames);
        for (int i = 1; i <= Metadata.FILES_COUNT; i++) {
            this.files.add(new File(fileNames.get(i)));
        }

        String filesStr = this.files.stream()
            .map(File::fileName)
            .collect(Collectors.joining(", "));
        LOGGER.info("[Initialized Files ({})] {}", this.port, filesStr);
    }

    private void sendSearchReqToNeighbour(
        String query,
        String initiatorIp,
        int initiatorPort,
        int hops,
        Neighbour neighbour) {

        String searchMsg = String.format(
            Commands.SEARCH,
            initiatorIp,
            initiatorPort,
            query,
            hops);

        this.sendToNeighbour(neighbour.ipAddress(), neighbour.port(), searchMsg);
        this.addVisitedNode(query, neighbour);
    }
}
