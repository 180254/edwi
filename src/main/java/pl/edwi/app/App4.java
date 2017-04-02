package pl.edwi.app;

import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.StrictErrorHandler;
import io.mola.galimatias.URL;
import io.mola.galimatias.URLParsingSettings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.edwi.tool.WebCache;
import pl.edwi.tool.WebDownloader;
import pl.edwi.tool.WebPage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

public class App4 {

    private static final Pattern SPACE_PATTEN = Pattern.compile(" ");
    private static final Pattern END_HASH_PATTERN = Pattern.compile("#?$");

    private static final String TASK_START_URL = "http://pduch.iis.p.lodz.pl/";
    private static final int TASK_MAX_DEPTH = 2;

    public static void main(String[] args) {

        final WebDownloader webDownloader = new WebDownloader();
        final WebCache webCache = new WebCache();

        final String startIP;
        try {
            String startHost = URL.parse(TASK_START_URL).host().toString();
            startIP = InetAddress.getByName(startHost).getHostAddress();
        } catch (UnknownHostException | GalimatiasParseException e) {
            throw new RuntimeException(e);
        }

        final Set<String> urlProcessed = new LinkedHashSet<>(5000);
        final Set<String> urlSameIP = new LinkedHashSet<>(5000);
        final Set<String> urlDiffIP = new LinkedHashSet<>(5000);
        final Queue<Task> queue = new LinkedList<>();

        final URLParsingSettings urlParsingSettings = URLParsingSettings.create()
                .withErrorHandler(StrictErrorHandler.getInstance());

        final long startTime = System.nanoTime();

        queue.add(new Task(TASK_START_URL, 0));

        while (!queue.isEmpty()) {
            Task task = queue.remove();
            String url = task.url;
            int depth = task.depth;

            WebPage page = webCache.getPage(url).orElseGet(() -> {
                WebPage pageInternal;

                try {
                    pageInternal = webDownloader.downloadPage(url);
                } catch (IOException e) {
                    pageInternal = new WebPage(url, "");
                    System.out.printf("FAIL.DL: %s %s%n", url, e.toString());
                }

                webCache.savePage(pageInternal);
                return pageInternal;
            });

            Elements links = page.document().select("a[href]");
            for (Element link : links) {

                String linkHref = link.attr("abs:href");
                linkHref = SPACE_PATTEN.matcher(linkHref).replaceAll("%20");
                if (linkHref.isEmpty()) {
                    continue;
                }

                try {
                    URL linkUrl = URL.parse(urlParsingSettings, linkHref).withFragment("");
                    if (linkUrl.host() == null) {
                        continue;
                    }

                    String linkHost = linkUrl.host().toString();
                    String linkString = END_HASH_PATTERN.matcher(linkUrl.toString()).replaceAll("");

                    if (!urlProcessed.add(linkString)) {
                        continue;
                    }

                    String ipAddress = webCache.getIp(linkHost).orElseGet(() -> {
                        String ipInternal;

                        try {
                            ipInternal = InetAddress.getByName(linkHost).getHostAddress();
                        } catch (UnknownHostException e) {
                            ipInternal = "";
                            System.out.printf("SKIPPED: %s %s%n", linkHost, e.toString());
                        }

                        webCache.saveIp(linkHost, ipInternal);
                        return ipInternal;
                    });

                    if (ipAddress.isEmpty()) {
                        continue;
                    }

                    (ipAddress.equals(startIP) ? urlSameIP : urlDiffIP).add(linkString);

                    if (depth != TASK_MAX_DEPTH && ipAddress.equals(startIP)) {
                        if (linkUrl.scheme().equals("http") || linkUrl.scheme().equals("https")) {
                            queue.add(new Task(linkString, (depth + 1)));
                        }
                    }

                } catch (GalimatiasParseException e) {
                    System.out.printf("SKIPPED: %s %s%n", linkHref, e.toString());
                }
            }
        }

        try {
            Files.write(Paths.get("app4-wew.txt"), urlSameIP, StandardCharsets.UTF_8);
            Files.write(Paths.get("app4-zew.txt"), urlDiffIP, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.printf("FAIL F.WRITE:  %s%n", e);
        }

        double timeSec = (System.nanoTime() - startTime) / 1_000_000.0 / 1_000.0;
        System.out.printf("time = %.4f seconds%n", timeSec);
    }

    private static class Task {
        private final String url;
        private final int depth;

        public Task(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }
}
