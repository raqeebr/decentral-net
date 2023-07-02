package org.uom.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.model.message.SocketMessage;
import org.uom.utils.Constants.Commands;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

public class MessageDispatcher extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDispatcher.class);
    private final BlockingQueue<SocketMessage> dispatcherQueue;
    private final DatagramSocket socket;
    private volatile boolean isRunning = true;

    public MessageDispatcher(BlockingQueue<SocketMessage> dispatcherQueue, DatagramSocket socket) {
        this.dispatcherQueue = dispatcherQueue;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            while (this.isRunning) {
                SocketMessage message = this.dispatcherQueue.take();
                String messageToDispatch = String.format(Commands.MSG_FMT, message.body.length(), message.body);
                byte[] sendData = messageToDispatch.getBytes();
                InetAddress address = InetAddress.getByName(message.ipAddress);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, message.port);
                this.socket.send(sendPacket);
                LOGGER.info("[Message Dispatched ({})] {}", this.socket.getLocalPort(), message.body);
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error("[Dispatch Failed] {}", e.getMessage(), e);
        }
    }

    public void stopDispatching() {
        this.isRunning = false;
    }
}
