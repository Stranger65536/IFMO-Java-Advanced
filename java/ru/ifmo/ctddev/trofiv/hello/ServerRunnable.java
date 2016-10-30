package ru.ifmo.ctddev.trofiv.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"DuplicateStringLiteralInspection", "BooleanVariableAlwaysNegated"})
class ServerRunnable implements Runnable {
    private final boolean closed;
    private final int threads;
    private final DatagramSocket socket;
    private final DatagramPacket request;
    private final ExecutorService threadPool;

    public ServerRunnable(
            final boolean closed,
            final int threads,
            final DatagramSocket socket,
            final DatagramPacket request,
            final ExecutorService threadPool) {
        this.closed = closed;
        this.threads = threads;
        this.socket = socket;
        this.request = request;
        this.threadPool = threadPool;
    }

    @Override
    @SuppressWarnings({"WhileLoopSpinsOnField", "LoopConditionNotUpdatedInsideLoop"})
    public void run() {
        while (!closed) {
            try {
                socket.receive(request);
            } catch (IOException ignored) {
                continue;
            }

            final InetAddress clientAddress = request.getAddress();
            final int clientPort = request.getPort();
            final byte[] respBuf = getResponse(request);
            final DatagramPacket response = new DatagramPacket(respBuf, respBuf.length, clientAddress, clientPort);

            if (threads == 1) {
                executeImmediately(socket, response);
            } else {
                threadPool.execute(() -> executeImmediately(socket, response));
            }
        }
    }

    private static byte[] getResponse(final DatagramPacket request) {
        return ("Hello, " + new String(request.getData(), 0, request.getLength(), StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void executeImmediately(final DatagramSocket socket, final DatagramPacket response) {
        try {
            socket.send(response);
        } catch (IOException ignored) {
            System.out.println("Answer send failed!");
        }
    }
}
