package info.kgeorgiy.java.advanced.hello;

import org.junit.Assert;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public final class Util {
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private Util() {
    }

    public static String request(final String msg, final DatagramSocket socket, final SocketAddress address) throws IOException {
        send(socket, msg, address);
        return receive(socket);
    }

    public static void send(final DatagramSocket socket, final String msg, final SocketAddress address) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[0], 0);
        setString(packet, msg);
        packet.setSocketAddress(address);
        synchronized (socket) {
            socket.send(packet);
        }
    }

    public static String receive(final DatagramSocket socket) throws IOException {
        DatagramPacket packet = createPacket(socket);
        socket.receive(packet);
        return getString(packet);
    }

    public static void setString(final DatagramPacket packet, final String msg) {
        packet.setData(msg.getBytes(CHARSET));
        packet.setLength(packet.getData().length);
    }

    public static DatagramPacket createPacket(final DatagramSocket socket) throws SocketException {
        return new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
    }

    public static String getString(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), CHARSET);
    }

    public static AtomicInteger[] server(final String prefix, final int threads, final double p, final DatagramSocket socket) {
        final AtomicInteger[] data = Stream.generate(AtomicInteger::new).limit(threads).toArray(AtomicInteger[]::new);
        new Thread(() -> {
            try {
                Random random = new Random(4357204587045842850L);
                while (true) {
                    DatagramPacket packet = createPacket(socket);
                    socket.receive(packet);
                    String request = getString(packet);
                    String errorMsg = "Invalid request " + request;
                    Assert.assertTrue(errorMsg, request.startsWith(prefix));
                    String[] requestParts = request.substring(prefix.length()).split("_");
                    Assert.assertEquals(errorMsg, 2, requestParts.length);

                    try {
                        int partOne = Integer.parseInt(requestParts[0]);
                        int partTwo = Integer.parseInt(requestParts[1]);
                        Assert.assertEquals(errorMsg, partTwo, data[partOne].get());
                        if (p >= random.nextDouble()) {
                            data[partOne].incrementAndGet();
                            setString(packet, response(request));
                            socket.send(packet);
                        } else if (random.nextBoolean()) {
                            setString(packet, corrupt(response(request), random));
                            socket.send(packet);
                        }
                    } catch (NumberFormatException ignored) {
                        throw new AssertionError(errorMsg);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }).start();
        return data;
    }

    public static String response(final String msg) {
        return "Hello, " + msg;
    }

    private static String corrupt(final String var0, final Random random) {
        switch (random.nextInt(3)) {
            case 0:
                return var0 + '0';
            case 1:
                return var0 + 'Q';
            case 2:
                return "";
            default:
                throw new AssertionError("Impossible");
        }
    }
}

