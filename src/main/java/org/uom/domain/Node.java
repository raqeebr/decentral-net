package org.uom.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.communication.MessageDispatcher;
import org.uom.communication.MessageReceiver;
import org.uom.contracts.handlers.MessageHandler;
import org.uom.handlers.JoinMessageHandler;
import org.uom.handlers.JoinResponseHandler;
import org.uom.handlers.LeaveMessageHandler;
import org.uom.handlers.RegisterResponseHandler;
import org.uom.model.message.Message;
import org.uom.model.message.SocketMessage;
import org.uom.utils.Constants.Commands;
import org.uom.utils.Constants.MessageTypes;
import org.uom.utils.Constants.Metadata;
import org.uom.utils.UsernameGenerator;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
    private static final Logger LOGGER = LoggerFactory.getLogger(Node.class);
    private static final int FILE_COUNT = 3;
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
        while (!this.sendQueue.isEmpty()) {
            try {
                synchronized (this) {
                    this.wait(1000);
                }
            } catch (InterruptedException e) {
                LOGGER.error("[Wait for Dispatch Failed ({})] {}", this.port, e.getMessage(), e);
            }
        }
        this.sender.stopDispatching();
        this.receiver.stopReceiving();
        if (this.socket.isClosed()) {
            this.socket.close();
        }
        LOGGER.info("[Node Disconnect] Stopped node ({}) {}", this.port, this.username);
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public int getPort() {
        return this.port;
    }

    public void addNeighbour(String ipAddress, int port) {
        this.neighbours.add(new Neighbour(ipAddress, port));
        LOGGER.info("[Added Neighbour ({})] {}:{}", this.port, ipAddress, port);
    }

    public void removeNeighbour(String ipAddress, int port) {
        Neighbour neighbour = this.neighbours.stream()
                .filter(n -> n.ipAddress.equals(ipAddress) && n.port == port)
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
            this.sendToNeighbour(neighbour.ipAddress, neighbour.port, leaveMessage);
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
        for (int i = 1; i <= FILE_COUNT; i++) {
            this.files.add(new File(fileNames.get(i)));
        }
    }
}
