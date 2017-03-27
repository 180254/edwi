package pl.edwi;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edwi.gut.Book;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@SuppressWarnings("Duplicates")
public class App6 {

    private static final String GUTENBERG_DVD = "pgdvd042010/";
    private static final String UNZIP_DIRECTORY = GUTENBERG_DVD + "UZ/";

    private static final Pattern FILE_TXT_FORMAT = Pattern.compile("[0-9]+.txt", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_ZIP_FORMAT = Pattern.compile("[0-9]+.zip", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARRIAGE_RETURN = Pattern.compile("\r", Pattern.LITERAL);
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final Directory index = FSDirectory.open(Paths.get("lucene"));

    public App6() throws IOException {
    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        App6 app6 = new App6();
        app6.logger.debug("start");

        app6.unzipAllZips();
        app6.logger.debug("unzip.done");

        app6.parseAllBooks();
        app6.logger.debug("parse.done");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String searching = scanner.nextLine();
            if (searching.equals("q")) {
                break;
            } else {
                try {
                    app6.searchTask(searching.trim());
                } catch (ParseException e) {
                    app6.logger.warn("Query cannot be parsed: " + e.toString());
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void unzipAllZips() throws IOException {
        AtomicInteger counter = new AtomicInteger();

        Files.find(
                Paths.get(GUTENBERG_DVD),
                Integer.MAX_VALUE,
                (path, attr) -> attr.isRegularFile()
        )
                .parallel()
                .filter(path -> FILE_ZIP_FORMAT.matcher(path.getFileName().toString()).matches())
                .filter(path -> !path.toString().contains("ETEXT"))
                .filter(path -> !Files.exists(Paths.get(UNZIP_DIRECTORY + path.getFileName() + "-UZ/")))
                .forEach(path -> {
                    try {
                        ZipFile zipFile = new ZipFile(path.toString());
                        zipFile.extractAll(UNZIP_DIRECTORY + path.getFileName() + "-UZ/");
                    } catch (ZipException e) {
                        System.out.printf("unzip.error %s %s%n", path, e);
                    }

                    int cnt = counter.incrementAndGet();
                    if (cnt % 100 == 0) {
                        logger.debug("unzip.counter: " + cnt);
                    }
                });
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void parseAllBooks() throws IOException {
        AtomicInteger counter = new AtomicInteger();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter indexWriter = new IndexWriter(index, config)) {

            Files.find(
                    Paths.get(UNZIP_DIRECTORY),
                    Integer.MAX_VALUE,
                    (path, attr) -> attr.isRegularFile()

            )
                    .parallel()
                    .filter(path -> FILE_TXT_FORMAT.matcher(path.getFileName().toString()).matches())
                    .forEach(path -> {
                        try {
                            byte[] bytes = Files.readAllBytes(path);
                            String string = new String(bytes, StandardCharsets.ISO_8859_1);
                            Book book = parseBook(path.getFileName().toString(), string);

                            Document doc = new Document();
                            doc.add(new StringField("id", book.getId(), Field.Store.YES));
                            doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
                            doc.add(new TextField("content", book.getContent(), Field.Store.YES));
                            indexWriter.addDocument(doc);

                        } catch (IOException e) {
                            logger.error("parse.error " + path + ' ' + e);
                        }

                        int cnt = counter.incrementAndGet();
                        if (cnt % 100 == 0) {
                            logger.debug("parse.counter: " + cnt);
                        }
                    });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Book parseBook(String filename, String raw) {
        String[] text = CARRIAGE_RETURN.matcher(raw).replaceAll("").split("\n");

        int titleIndex = 0;
        boolean titleProcess = false;
        StringBuilder titleBuilder = new StringBuilder(50);
        for (int i = 0; i < text.length; i++) {
            if (text[i].startsWith("Title:")) {
                titleBuilder.append(text[i].substring(6));
                titleIndex = i;
                titleProcess = true;

            } else if (titleProcess) {
                if (!text[i].isEmpty()) {
                    titleBuilder.append(text[i]);
                } else {
                    break;
                }
            }
        }

        int contentIndex = 0;
        for (int i = titleIndex; i < text.length; i++) {
            if (text[i].startsWith("*** START OF THIS PROJECT GUTENBERG")) {
                contentIndex = i + 1;
                break;
            }
        }

        String title = WHITESPACES.matcher(titleBuilder).replaceAll(" ");
        String content = String.join("\n", Arrays.asList(text).subList(contentIndex, text.length)).trim();

        return new Book(filename, title, content);
    }


    private void searchTask(String find) throws ParseException, IOException {
        Query query = new QueryParser("title", analyzer).parse(find);

        try (IndexReader reader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs docs = searcher.search(query, 10);
            ScoreDoc[] hits = docs.scoreDocs;

            System.out.println("Found " + docs.totalHits + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                Document d = searcher.doc(hits[i].doc);
                System.out.printf("%2d. [%2.4f] %-10s %s%n", i + 1, hits[i].score, d.get("id"), d.get("title"));
            }
        }
    }
}
