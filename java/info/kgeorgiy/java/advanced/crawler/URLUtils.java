package info.kgeorgiy.java.advanced.crawler;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class for <a href="http://tools.ietf.org/html/rfc3986">URL</a> manipulations.
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public class URLUtils {
    private URLUtils() {
        // Utility class
    }

    /**
     * Returns host part of the specified URL.
     *
     * @param url url to get host part for.
     *
     * @return host part of the provided URL or empty string if URL has no host part.
     *
     * @throws MalformedURLException if specified URL is invalid.
     */
    public static String getHost(final String url) throws MalformedURLException {
        return getURI(url).getHost();
    }

    /**
     * Converts string representation of the URL to {@link URI}. Converted URI always
     * have non-empty path. If original URL has empty path, the "<tt>/</tt>" path is used.
     *
     * @param url url to convert.
     *
     * @return converted URL.
     *
     * @throws MalformedURLException if specified URL is invalid.
     */
    public static URI getURI(final String url) throws MalformedURLException {
        final String fragmentless = removeFragment(url);
        try {
            final URI uri = new URL(fragmentless).toURI();
            return uri.getPath() == null || uri.getPath().isEmpty() ? new URL(fragmentless + "/").toURI() : uri;
        } catch (final URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
    }

    /**
     * Removes fragment part of the URL.
     *
     * @param url url to remove fragment from.
     *
     * @return URL without fragment part
     */
    public static String removeFragment(final String url) {
        final int index = url.indexOf('#');
        return index >= 0 ? url.substring(0, index) : url;
    }
}
