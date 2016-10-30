package ru.ifmo.ctddev.trofiv.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import net.java.quickcheck.collection.Triple;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicateStringLiteralInspection")
public class HelloUDPClient implements HelloClient {
    public static void main(final String[] args) {
        try {
            final Triple<Integer, Integer, Integer> arguments = parseArgument(args);
            new HelloUDPClient().start(args[0], arguments.getFirst(), args[2], arguments.getSecond(), arguments.getThird());
        } catch (Exception e) {
            printHelp(e.getMessage());
        }
    }

    private static Triple<Integer, Integer, Integer> parseArgument(final String[] args) {
        final int port, requests, threads;
        if (args.length == 5) {
            port = Integer.parseInt(args[1]);
            requests = Integer.parseInt(args[3]);
            threads = Integer.parseInt(args[4]);
        } else {
            throw new IllegalArgumentException("Invalid arguments number!");
        }
        return new Triple<>(port, requests, threads);
    }

    @Override
    public void start(final String host, final int port, final String prefix, final int requests, final int threads) {
        final ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        try {
            final InetAddress address;
            try {
                address = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Unknown hostname", e);
            }

            for (int i = 0; i < threads; i++) {
                threadPool.execute(new ClientRunnable(i, requests, prefix, port, address));
            }

            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
        } finally {
            threadPool.shutdownNow();
        }
    }

    private static void printHelp(final String msg) {
        System.err.println(msg);
        System.err.println("Usage: java HelloUDPClient <hostname> <port> <prefix> <requests> <threads>");
    }
}
