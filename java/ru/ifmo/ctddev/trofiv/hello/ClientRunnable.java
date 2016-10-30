package ru.ifmo.ctddev.trofiv.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("DuplicateStringLiteralInspection")
class ClientRunnable implements Runnable {
    private static final int TIMEOUT = 200;

    private final int threadId;
    private final int requests;
    private final String prefix;
    private final int port;
    private final InetAddress address;

    public ClientRunnable(
            final int threadId,
            final int requests,
            final String prefix,
            final int port,
            final InetAddress address) {
        this.threadId = threadId;
        this.requests = requests;
        this.prefix = prefix;
        this.port = port;
        this.address = address;
    }

    @Override
    @SuppressWarnings("AssignmentToForLoopParameter")
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            final byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
            for (int i = 0; i < requests; i++) {
                final String request = prefix + threadId + '_' + i;
                final byte[] sendBuffer = request.getBytes(StandardCharsets.UTF_8);

                send(port, address, socket, sendBuffer);

                try {
                    final DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(packet);
                    final String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (response.equals("Hello, " + request)) {
                        System.out.println(response);
                    } else {
                        i--;
                    }
                } catch (IOException ignored) {
                    i--;
                }
            }
        } catch (SocketException e) {
            throw new IllegalStateException("Socket can't be created", e);
        }
    }

    private static void send(final int port, final InetAddress address, final DatagramSocket socket, final byte[] sendBuffer) {
        while (true) {
            try {
                socket.send(new DatagramPacket(sendBuffer, sendBuffer.length, address, port));
                break;
            } catch (IOException ignored) {
                continue;
            }
        }
    }
}
