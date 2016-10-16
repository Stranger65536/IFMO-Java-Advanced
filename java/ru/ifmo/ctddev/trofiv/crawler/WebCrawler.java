package ru.ifmo.ctddev.trofiv.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import net.java.quickcheck.collection.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

import static info.kgeorgiy.java.advanced.crawler.URLUtils.getHost;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService extractThreadPool;
    private final ExecutorService downloadThreadPool;
    private final Collection<String> downloaded = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Integer> perHostDownloadsCount = new ConcurrentHashMap<>(1024, 0.75f);
    private final Map<String, BlockingQueue<DownloadCallable>> downloadLeft = new ConcurrentHashMap<>(1024, 0.75f);
    private final BlockingQueue<Pair<Future<String>, String>> processingQueue = new LinkedBlockingQueue<>();
    private final int perHost;

    public WebCrawler(
            final Downloader downloader,
            final int downloaders,
            final int extractors,
            final int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadThreadPool = Executors.newFixedThreadPool(downloaders);
        extractThreadPool = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(final String url, final int depth) {
        try {
            return process(url, depth);
        } catch (InterruptedException ignored) {
            return new Result(Collections.emptyList(), Collections.emptyMap());
        }
    }

    @Override
    public void close() {
        downloadThreadPool.shutdown();
        extractThreadPool.shutdown();
        try {
            downloadThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            extractThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(final String[] args) {
        if (args.length < 1 || args.length > 4) {
            printHelp();
        } else {
            processCommand(args);
        }
    }

    private static void printHelp() {
        System.out.println("Usage: WebCrawler downloadableUrl [downloaders [extractors [perHost]]]");
    }

    private static void processCommand(final String[] args) {
        final int downloaders = args.length > 1
                ? Integer.parseInt(args[1])
                : 1;
        final int extractors = args.length > 2
                ? Integer.parseInt(args[2])
                : 1;
        final int perHost = args.length > 3
                ? Integer.parseInt(args[3])
                : 1;

        try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            System.out.println(crawler.download(args[0], 3));
        } catch (IOException e) {
            System.err.println("Can't download page: " + e.getMessage());
        }
    }

    private Result process(final String url, final int depth) throws InterruptedException {
        try {
            perHostDownloadsCount.put(getHost(url), 1);
            processingQueue.add(new Pair<>(downloadThreadPool.submit(new DownloadCallable(url, 1, depth)), url));

            final List<String> result = new ArrayList<>(1024);
            final Map<String, IOException> errors = new HashMap<>(64, 1.0f);

            while (!processingQueue.isEmpty()) {
                final Pair<Future<String>, String> pair = processingQueue.take();
                final Future<String> future = pair.getFirst();
                try {
                    final String res = future.get();

                    if (res != null && !errors.containsKey(res)) {
                        result.add(res);
                    }
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();

                    if (cause instanceof IOException) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        errors.put(pair.getSecond(), (IOException) cause);
                        continue;
                    }

                    throw new IllegalStateException(e);
                }
            }
            return new Result(result, errors);
        } catch (MalformedURLException e) {
            return new Result(Collections.emptyList(), Collections.singletonMap(url, e));
        }
    }

    private class ExtractionCallable implements Callable<String> {
        private final int depth;
        private final int maxDepth;
        private final Document document;

        ExtractionCallable(
                final Document document,
                final int depth,
                final int maxDepth) {
            this.depth = depth;
            this.maxDepth = maxDepth;
            this.document = document;
        }

        @Override
        public String call() throws IOException, InterruptedException {
            final List<String> links = document.extractLinks();
            for (String link : links) {
                synchronized (perHostDownloadsCount) {
                    perHostDownloadsCount.putIfAbsent(getHost(link), 0);
                    final DownloadCallable downloadTask = new DownloadCallable(link, depth + 1, maxDepth);
                    if (perHostDownloadsCount.get(getHost(link)) < perHost) {
                        perHostDownloadsCount.compute(getHost(link), (s, i) -> i + 1);
                        final Future<String> downloadFuture = downloadThreadPool.submit(downloadTask);
                        processingQueue.add(new Pair<>(downloadFuture, link));
                    } else {
                        downloadLeft.putIfAbsent(getHost(link), new LinkedBlockingQueue<>());
                        downloadLeft.get(getHost(link)).put(downloadTask);
                    }
                }
            }
            //noinspection ReturnOfNull
            return null;
        }
    }

    private class DownloadCallable implements Callable<String> {
        private final String url;
        private final int depth;
        private final int maxDepth;

        DownloadCallable(
                final String url,
                final int depth,
                final int maxDepth) {
            this.url = url;
            this.depth = depth;
            this.maxDepth = maxDepth;
        }

        @Override
        public String call() throws IOException, InterruptedException {
            if (downloaded.add(url)) {
                final Document document = downloader.download(url);
                if (depth < maxDepth) {
                    final Callable<String> extractionTask = new ExtractionCallable(document, depth, maxDepth);
                    final Future<String> extractionFuture = extractThreadPool.submit(extractionTask);
                    processingQueue.put(new Pair<>(extractionFuture, url));
                }
            }

            synchronized (perHostDownloadsCount) {
                final BlockingQueue<DownloadCallable> perHostDownloadTasks = downloadLeft.get(getHost(url));
                if (perHostDownloadTasks != null && !perHostDownloadTasks.isEmpty()) {
                    final DownloadCallable downloadTask = perHostDownloadTasks.take();
                    final Future<String> downloadFuture = downloadThreadPool.submit(downloadTask);
                    processingQueue.put(new Pair<>(downloadFuture, url));
                } else {
                    perHostDownloadsCount.compute(getHost(url), (h, c) -> c - 1);
                }
            }

            return url;
        }
    }
}