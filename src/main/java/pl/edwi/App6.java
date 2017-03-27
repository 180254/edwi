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
import org.apache.lucene.store.NIOFSDirectory;
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

    public static final String GUT_DVD = "pgdvd042010/";
    public static final Pattern CARRIAGE_RETURN = Pattern.compile("\r", Pattern.LITERAL);
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    private static final Pattern FILENAME_FORMAT = Pattern.compile("[0-9]+.txt");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private final Directory index = new NIOFSDirectory(Paths.get("lucene"));

    public App6() throws IOException {
    }

    // ---------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        App6 app6 = new App6();
        app6.logger.debug("START");

//        app6.unzipAllZips();
        app6.logger.debug("UNZIP_ALL_ZIPS");

        app6.parseAllBooks();
        app6.logger.debug("PARSE_ALL_BOOKS");

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
        Files.find(
                Paths.get(GUT_DVD),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile()

        ).parallel().forEach(zipPath -> {
            String zipPathStr = zipPath.toString();
            String zipPathStrLower = zipPathStr.toLowerCase();
            if (zipPathStrLower.contains("etext") || !zipPathStrLower.endsWith(".zip")) {
                return;
            }

            String unpackPathStr = zipPathStr.substring(0, zipPathStr.lastIndexOf('.'));
            if (Files.exists(Paths.get(unpackPathStr))) {
                return;
            }

            try {
                ZipFile zipFile = new ZipFile(zipPathStr);
                zipFile.extractAll(unpackPathStr);
            } catch (ZipException e) {
                System.out.printf("UNPACK.ERROR %s %s%n", zipPath, e);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------

    private void parseAllBooks() throws IOException {
        AtomicInteger counter = new AtomicInteger();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter indexWriter = new IndexWriter(index, config)) {

            Files.find(
                    Paths.get(GUT_DVD),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile()

            ).parallel().forEach(bookPath -> {
                String filename = bookPath.getFileName().toString();
                if (!FILENAME_FORMAT.matcher(filename).matches()) {
                    return;
                }

                try {
                    byte[] bytes = Files.readAllBytes(bookPath);
                    String string = new String(bytes, StandardCharsets.ISO_8859_1);
                    Book book = parseBook(filename, string);

                    Document doc = new Document();
                    doc.add(new StringField("id", book.getId(), Field.Store.YES));
                    doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
                    doc.add(new TextField("content", book.getContent(), Field.Store.YES));
                    indexWriter.addDocument(doc);

                } catch (IOException e) {
                    System.out.printf("PARSE.ERROR %s %s%n", bookPath, e);
                }

                int cnt = counter.incrementAndGet();
                if (cnt % 100 == 0) {
                    logger.debug("counter: " + cnt);
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Book parseBook(String path, String raw) {
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

        String title = WHITESPACES.matcher(titleBuilder).replaceAll(" ");
        String content = String.join("\n", Arrays.asList(text).subList(titleIndex, text.length)).trim();

        return new Book(path, title, content);
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
