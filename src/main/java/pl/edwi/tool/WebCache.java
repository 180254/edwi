package pl.edwi.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.regex.Pattern;

public class WebCache {

    public static final Pattern FILENAME_INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");
    public static final int MAX_URL_FILENAME_LEN = 150;

    public WebCache() {
        try {
            Files.createDirectories(Paths.get("cache/page"));
            Files.createDirectories(Paths.get("cache/ip"));
        } catch (IOException e) {
            System.out.printf("WEB CACHE DIR FAIL %s%n", e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    public Optional<WebPage> getPage(String url) {
        return read(url, "cache/page/", ".html")
                .map((content) -> new WebPage(url, content));
    }

    public void savePage(WebPage page) {
        save(page.url(), "cache/page/", ".html", page.rawText());
        // save(page.url(), "cache/page/", ".txt", page.cleanText());
    }

    // ---------------------------------------------------------------------------------------------------------------

    public Optional<String> getIp(String host) {
        return read(host, "cache/ip/", ".txt");
    }

    public void saveIp(String host, String ip) {
        save(host, "cache/ip/", ".txt", ip);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getRobots(String url, WebDownloader wd) {
        int slash = url.indexOf('/', 8);
        String robotsUrl = (slash == -1 ? url : url.substring(0, slash)) + "/robots.txt";

        Optional<WebPage> cachedPage = getPage(robotsUrl);
        if (cachedPage.isPresent()) {
            return cachedPage.get().rawText();

        } else {
            WebPage robotsPage;
            try {
                robotsPage = wd.downloadPage(robotsUrl);
            } catch (IOException e) {
                robotsPage = new WebPage(robotsUrl, "");
            }
            savePage(robotsPage);
            return robotsPage.rawText();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Optional<String> read(String url, String prefix, String suffix) {
        String filename = prefix + urlToFilename(url) + suffix;
        Path path = Paths.get(filename);

        try {
            byte[] bytes = Files.readAllBytes(path);
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void save(String url, String prefix, String suffix, String content) {
        String filename = prefix + urlToFilename(url) + suffix;
        Path path = Paths.get(filename);

        try {
            Files.write(
                    path,
                    content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            System.out.printf("WEB CACHE SAVE FAIL %s %s %s%n", url, prefix, suffix);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private String urlToFilename(String url) {
        String s = FILENAME_INVALID_CHARS.matcher(url).replaceAll("_");
        return s.substring(0, Math.min(s.length(), MAX_URL_FILENAME_LEN));
    }
}
