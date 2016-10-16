package info.kgeorgiy.java.advanced.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads document from the Web and stores them in storage directory.
 *
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public class CachingDownloader implements Downloader {
    private final File directory;

    /**
     * Creates a new downloader storing documents in temporary directory.
     *
     * @throws IOException if an error occurred.
     */
    public CachingDownloader() throws IOException {
        this(Files.createTempDirectory(CachingDownloader.class.getName()).toFile());
    }

    /**
     * Creates a new downloader storing documents in specified directory.
     *
     * @param directory storage directory.
     *
     * @throws IOException if an error occurred.
     */
    public CachingDownloader(final File directory) throws IOException {
        this.directory = directory;
        if (!directory.exists()) {
            Files.createDirectories(directory.toPath());
        }
        if (!directory.isDirectory()) {
            throw new IOException(directory + " is not a directory");
        }
    }

    /**
     * Downloads document and stores it to the storage directory. An error during
     * download may result in incomplete files in storage directory.
     *
     * @param url URL of the document to download.
     *
     * @return downloaded document.
     *
     * @throws IOException if an error occurred.
     */
    public Document download(final String url) throws IOException {
        System.out.println("Downloading " + url);
        final URI uri = URLUtils.getURI(url);
        final File file = new File(directory, URLEncoder.encode(uri.toString(), "UTF-8"));
        try (
                final InputStream is = new BufferedInputStream(uri.toURL().openStream());
                final OutputStream os = new BufferedOutputStream(new FileOutputStream(file))
        ) {
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) >= 0) {
                os.write(buffer, 0, read);
            }
        }
        System.out.println("Downloaded " + url);
        return () -> {
            final Elements elements = Jsoup.parse(file, null, url).select("a[href]");
            final List<String> result = new ArrayList<>();
            for (final Element element : elements) {
                try {
                    final URI href = uri.resolve(element.attr("href"));
                    if (("http".equalsIgnoreCase(href.getScheme()) || "https".equals(href.getScheme())) && href.getHost() != null) {
                        result.add(URLUtils.removeFragment(href.normalize().toString()));
                    }
                } catch (final IllegalArgumentException e) {
                    // Invalid URI, ignore
                }
            }
            System.out.println("Links for " + url + ": " + result);
            return result;
        };
    }
}
