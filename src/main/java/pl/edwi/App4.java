package pl.edwi;

import io.mola.galimatias.GalimatiasParseException;
import io.mola.galimatias.StrictErrorHandler;
import io.mola.galimatias.URL;
import io.mola.galimatias.URLParsingSettings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.edwi.web.WebDownloader;
import pl.edwi.web.WebPage;
import pl.edwi.web.WebSaver;

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

    private static final String START_URL = "http://pduch.iis.p.lodz.pl/";
    private static final int MAX_DEPTH = 2;

    private final WebDownloader webDownloader = new WebDownloader();
    private final WebSaver webSaver = new WebSaver();

    private final InetAddress startIP = InetAddress.getByName(URL.parse(START_URL).host().toString());
    private final Set<String> urlProcessed = new LinkedHashSet<>(5000);
    private final Set<String> urlSameIP = new LinkedHashSet<>(5000);
    private final Set<String> urlDiffIP = new LinkedHashSet<>(5000);

    private final Queue<Entry> queue = new LinkedList<>();

    private final URLParsingSettings urlParsingSettings = URLParsingSettings.create()
            .withErrorHandler(StrictErrorHandler.getInstance());

    public App4() throws
            GalimatiasParseException,
            UnknownHostException {
    }

    public static void main(String[] args) throws
            GalimatiasParseException,
            UnknownHostException,
            InterruptedException {

        long startTime = System.nanoTime();

        App4 app = new App4();
        app.queue.add(new Entry(START_URL, 0));

        while (!app.queue.isEmpty()) {
            Entry entry = app.queue.remove();
            String url = entry.url;
            int depth = entry.depth;


        }


        try {
            Files.write(Paths.get("app4-wew.txt"), app.urlSameIP, StandardCharsets.UTF_8);
            Files.write(Paths.get("app4-zew.txt"), app.urlDiffIP, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.printf("FAIL F.WRITE:  %s%n", e);
        }

        double timeSec = (System.nanoTime() - startTime) / 1_000_000.0 / 1_000.0;
        System.out.printf("time = %.4f seconds%n", timeSec);
        System.out.println(app.total);
        System.out.println(app.caches);

    }

    int total;
    int caches;

    public void process(String url, final int depth) {
        System.out.println("VISITED " + depth + " " + url);
//        aaaaa.lock();
        try {
            WebPage page;
            String s = webSaver.urlToFilename(url, ".html");
            try {
                if (Files.exists(Paths.get("cache/" + s))) {
                    total++;
                    caches++;
                    page = new WebPage(url, new String(Files.readAllBytes(Paths.get("cache/" + s)), StandardCharsets.UTF_8));
                } else {
                    total++;
                    page = webDownloader.download(url);
                    webSaver.save(url, page);
                }

            } catch (IOException e) {
                System.out.printf("FAIL.DL: %s %s%n", url, e.toString());
                return;
            }

            Elements xxx = page.document().select("a[href]").clone();
            // System.out.println("> " + depth + " " + new ArrayList<>(xxx).size());

            for (Element link : xxx) {
                if (depth == 0) System.out.println("LINK " + link);
                String linkHref = link.attr("abs:href");
                linkHref = SPACE_PATTEN.matcher(linkHref).replaceAll("%20");
                if (linkHref.isEmpty()) {
                    continue;
                }

//                Lock lock = locks.get(linkHref);
//                lock.lock();
                try {
                    URL linkUrl = URL.parse(urlParsingSettings, linkHref).withFragment("");
                    if (linkUrl.host() == null) {
                        continue;
                    }

                    String linkHost = linkUrl.host().toString();
                    String linkString = END_HASH_PATTERN.matcher(linkUrl.toString()).replaceAll("");

                    if (urlProcessed.contains(linkString)) {
                        continue;
                    }

                    if (depth < MAX_DEPTH) {
//                        System.out.println("> " + depth + " " + linkString);
                    }
                    urlProcessed.add(linkString);
                    InetAddress ipAddress = InetAddress.getByName(linkHost);
                    (ipAddress.equals(startIP) ? urlSameIP : urlDiffIP).add(linkString);

                    if (linkString.contains("SMPD.html")) {
                        System.out.println("X");
                    }

                    if (depth != MAX_DEPTH) {
                        //   System.out.println(depth);
                        //   sync.countUp();
                        // process(linkString, (depth + 1));
                        process(linkString, (depth + 1));
                    }

                } catch (Exception e) {
                    System.out.printf("SKIPPED: %s %s%n", linkHref, e.toString());
                } finally {
//                    lock.unlock();
                }
            }
        } catch (Exception e) {
            System.out.println("WTFWTFTWF " + e.toString());
        } finally {
//            aaaaa.unlock();
            //  sync.countDown();
        }
    }

    private static class Entry {
        private final String url;
        private final int depth;

        public Entry(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }
}
