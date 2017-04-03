package pl.edwi.app;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.tool.Book;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("Duplicates")
public class App6a {

    public static final String GUTENBERG_DIR = "pgdvd042010\\";
    public static final String LUCENE_DIR = "lucene\\";

    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    private static final Pattern FILE_FORMAT = Pattern.compile(
            "([0-9]+)(?:[_\\-][0-9])?\\.(txt|zip)", Pattern.CASE_INSENSITIVE // 00000.txt / 00000-9.txt
    );

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Set<String> books = ConcurrentHashMap.newKeySet();
    private final AtomicInteger counter = new AtomicInteger();

    public App6a() throws IOException {
    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        try (Analyzer analyzer = new StandardAnalyzer();
             Directory index = FSDirectory.open(Paths.get(LUCENE_DIR))) {

            App6a app6a = new App6a();
            app6a.logger.info("start");

            app6a.processAll(analyzer, index);
            app6a.logger.debug("process.done");
            app6a.logger.info("process.counter: {}", app6a.counter.get());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void processAll(Analyzer analyzer, Directory index) throws IOException {
        try (IndexWriter indexWriter = new IndexWriter(index, new IndexWriterConfig(analyzer))) {

            Files.find(
                    Paths.get(GUTENBERG_DIR),
                    Integer.MAX_VALUE,
                    (path, attr) -> attr.isRegularFile()
            )
                    .parallel()
                    .filter(path -> !path.toString().contains("ETEXT"))
                    .forEach(path -> {

                        String pathLower = path.toString().toLowerCase();

                        if (pathLower.endsWith(".zip")) {
                            processZip(indexWriter, path);
                        } else if (pathLower.endsWith(".txt")) {
                            processTxt(indexWriter, path);
                        }
                    });

            indexWriter.commit();
        }
    }


    // ---------------------------------------------------------------------------------------------------------------

    private void processZip(IndexWriter indexWriter, Path path) {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }

                if (!isNewBook(new File(zipEntry.getName()).getName())) {
                    continue;
                }

                try (InputStream is = zipFile.getInputStream(zipEntry)) {
                    Book book = parseBook(path.getFileName().toString(), is);
                    Document doc = bookDocument(book);
                    indexWriter.addDocument(doc);
                    counterPlusPlus();
                }
            }

        } catch (IOException e) {
            logger.error("process.error: {} {}", path, e.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void processTxt(IndexWriter indexWriter, Path path) {
        if (!isNewBook(path.getFileName().toString())) {
            return;
        }

        try (InputStream is = Files.newInputStream(path)) {
            Book book = parseBook(path.getFileName().toString(), is);
            Document doc = bookDocument(book);
            indexWriter.addDocument(doc);
            counterPlusPlus();

        } catch (IOException e) {
            logger.error("process.error: {} {}", path, e.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void counterPlusPlus() {
        int cnt = counter.incrementAndGet();
        if (cnt % 100 == 0) {
            logger.debug("process.counter: {}", cnt);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private boolean isNewBook(String fileName) {
        Matcher matcher = FILE_FORMAT.matcher(fileName);
        return matcher.matches() && books.add(matcher.group(1));
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Document bookDocument(Book book) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", book.getId(), Field.Store.YES));
        doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
        doc.add(new TextField("content", book.getContent(), Field.Store.YES));
        return doc;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Book parseBook(String filename, InputStream inputStream) throws IOException {
        String raw = readInputStream(filename, inputStream);
        String title = "";
        String content = "";

        int titleBegin = raw.indexOf("Title:") + 6;
        if (titleBegin != -1) {
            int titleEnd = raw.indexOf("\r\n\r\n", titleBegin);
            if (titleEnd == -1) {
                titleEnd = raw.indexOf("\n\n", titleBegin);
            }
            title = raw.substring(titleBegin, titleEnd);
            title = WHITESPACES.matcher(title).replaceAll(" ").trim();


            int startBegin = raw.indexOf("***", titleEnd);
            if (startBegin != -1) {
                int startEnd = raw.indexOf("\r\n", startBegin);
                if (startEnd == -1) {
                    startEnd = raw.indexOf('\n', titleBegin);
                }

                content = raw.substring(startEnd);
                content = WHITESPACES.matcher(content).replaceAll(" ").trim();
            }
        }

        return new Book(filename, title, content);
    }

    // ---------------------------------------------------------------------------------------------------------------

    private String readInputStream(String filename, InputStream inputStream) throws IOException {
        Charset charset;
        if (filename.contains("-0") || filename.contains("_0")) {
            charset = StandardCharsets.UTF_8;
        } else if (filename.contains("-8") || filename.contains("_8")) {
            charset = StandardCharsets.ISO_8859_1;
        } else {
            charset = StandardCharsets.US_ASCII;
        }

        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024 * 8];

            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString(charset.name());
        }
    }
}
