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

    public void save(String url, WebPage page) throws IOException {
        save(url, page.raw(), ".html");
        save(url, page.clean(), ".txt");
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void save(String url, String page, String extension) throws IOException {
        String filename = urlToFilename(url, extension);
        Path path = Paths.get(filename);

        Files.write(
                path,
                page.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ---------------------------------------------------------------------------------------------------------------

    private String urlToFilename(String url, String extension) {
        return FILENAME_INVALID_CHARS.matcher(url).replaceAll("_") + extension;
    }
}
