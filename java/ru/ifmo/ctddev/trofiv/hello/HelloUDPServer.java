package ru.ifmo.ctddev.trofiv.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import net.java.quickcheck.collection.Pair;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("DuplicateStringLiteralInspection")
public class HelloUDPServer implements HelloServer {
    private static final String SERVER_IS_CLOSED = "Cannot start, because server is closed";
    private final Collection<DatagramSocket> sockets = new ArrayList<>();
    private final Collection<ExecutorService> threadPools = new ArrayList<>();
    private boolean closed;

    @SuppressWarnings({"WaitOrAwaitWithoutTimeout", "WaitNotInLoop", "SynchronizeOnThis"})
    public static void main(final String[] args) {
        try (HelloServer server = new HelloUDPServer()) {
            final Pair<Integer, Integer> arguments = parseArgument(args);
            server.start(arguments.getFirst(), arguments.getSecond());
            synchronized (HelloUDPServer.class) {
                try {
                    HelloUDPServer.class.wait();
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception e) {
            printHelp(e.getMessage());
        }
    }

    private static Pair<Integer, Integer> parseArgument(final String[] args) {
        final int port, threads;
        if (args.length == 2) {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } else {
            throw new IllegalArgumentException("Invalid arguments number!");
        }
        return new Pair<>(port, threads);
    }

    private static void printHelp(final String msg) {
        System.err.println(msg);
        System.err.println("Usage: java HelloUDPServer <port> <threads>");
    }

    @Override
    @SuppressWarnings("SynchronizeOnThis")
    public void start(final int port, final int threads) {
        synchronized (this) {
            checkClosed();
        }

        final ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        try {
            final DatagramSocket socket = new DatagramSocket(port);
            final byte[] reqBuf = new byte[socket.getReceiveBufferSize()];

            synchronized (this) {
                checkClosed();
                sockets.add(socket);
                threadPools.add(threadPool);
                final DatagramPacket request = new DatagramPacket(reqBuf, reqBuf.length);
                threadPool.execute(new ServerRunnable(closed, threads, socket, request, threadPool));
            }
        } catch (SocketException e) {
            throw new IllegalStateException("Can't create socket", e);
        }
    }

    @Override
    @SuppressWarnings({"MethodMayBeSynchronized", "SynchronizeOnThis"})
    public void close() {
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("Server has been already closed!");
            }
            closed = true;
            sockets.forEach(DatagramSocket::close);
            threadPools.forEach(ExecutorService::shutdownNow);
            sockets.clear();
            threadPools.clear();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException(SERVER_IS_CLOSED);
        }
    }
}
