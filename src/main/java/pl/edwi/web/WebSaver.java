package pl.edwi.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

public class WebSaver {

    private static final Pattern FILENAME_INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");

    // ---------------------------------------------------------------------------------------------------------------

    public void save(WebPage page) throws IOException {
        save(page.url(), page.rawText(), ".html");
        save(page.url(), page.cleanText(), ".txt");
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void save(String url, String page, String extension) throws IOException {
        String filename = urlToFilename(url, extension);
        Path path = Paths.get("cache/" + filename);

        Files.write(
                path,
                page.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String urlToFilename(String url, String extension) {
        return FILENAME_INVALID_CHARS.matcher(url).replaceAll("_") + extension;
    }
}
