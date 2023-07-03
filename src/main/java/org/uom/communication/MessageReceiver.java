package org.uom.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uom.model.message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

public class MessageReceiver extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageReceiver.class);
    private final BlockingQueue<Message> receiveQueue;
    private final DatagramSocket socket;
    private volatile boolean isRunning;

    public MessageReceiver(BlockingQueue<Message> receiveQueue, DatagramSocket socket) {
        this.receiveQueue = receiveQueue;
        this.socket = socket;
        this.isRunning = true;
    }

    @Override
    public void run() {
        try {
            while (this.isRunning) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                this.socket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String ipAddress = receivePacket.getAddress().getHostAddress();
                int port = receivePacket.getPort();
                Message response = new Message(ipAddress, port, message);

                this.receiveQueue.put(response);
                LOGGER.info("[Received Message ({} <-- {})]: {}", this.socket.getLocalPort(), port, message);
            }
        } catch (IOException e) {
            LOGGER.error("[Receive Failed] {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.warn("[Receive Failed] Could not add to receive queue");
        }
    }

    public void stopReceiving() {
        this.isRunning = false;
    }
}
