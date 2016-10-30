package info.kgeorgiy.java.advanced.hello;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface HelloClient {
    void start(String host, int port, String prefix, int requests, int threads);
}
